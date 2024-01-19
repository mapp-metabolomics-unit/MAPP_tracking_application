// QR code reader is deprecated, this avoid warnings.
// Maybe a good point to change the QR code reader to a not deprecated one in the future.

// Links the screen to the application
package org.example.dbgitracking

// Imports

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.printengine.templateinterface.TemplateFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


// Create the class for the actual screen
class WeightingActivity : AppCompatActivity() {

    // Initiate the displayed objects
    private lateinit var chooseWeightLabel: TextView
    private lateinit var weightInput: EditText
    private lateinit var extractionMethodLabel: TextView
    private lateinit var scanButtonSample: Button
    private var isObjectScanActive = false
    private lateinit var scannedInfoTextView: TextView
    private lateinit var numberInput: EditText
    private lateinit var actionButton: Button
    private lateinit var emptyPlace: TextView
    private var hasTriedAgain = false
    private var lastAccessToken: String? = null
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView
    private var isQrScannerActive = false

@SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create the connection with the XML file to add the displayed objects
        setContentView(R.layout.activity_weighting)

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        // Initialize objects views
        chooseWeightLabel = findViewById(R.id.chooseWeightLabel)
        weightInput = findViewById(R.id.weightInput)
        extractionMethodLabel = findViewById(R.id.extractionMethodLabel)
        scanButtonSample = findViewById(R.id.scanButtonSample)
        scannedInfoTextView = findViewById(R.id.scannedInfoTextView)
        numberInput = findViewById(R.id.numberInput)
        actionButton = findViewById(R.id.actionButton)
        emptyPlace = findViewById(R.id.emptyPlace)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        val token = intent.getStringExtra("ACCESS_TOKEN").toString()

        // stores the original token
        retrieveToken(token)

        weightInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                if (inputText != "") {
                    inputText.toInt()
                }

                weightInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                extractionMethodLabel.visibility = View.VISIBLE // Show actionButton if valid
                scanButtonSample.visibility = View.VISIBLE
            }
        })

        // Set up button click listener for Object QR Scanner
        scanButtonSample.setOnClickListener {
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(weightInput.windowToken, 0)
            chooseWeightLabel.visibility = View.INVISIBLE
            isObjectScanActive = true
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan the sample"
            flashlightButton.visibility = View.VISIBLE
            weightInput.visibility = View.INVISIBLE
            scanButtonSample.visibility = View.INVISIBLE
            actionButton.visibility = View.INVISIBLE
            QRCodeScannerUtility.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                QRCodeScannerUtility.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonSample.visibility = View.VISIBLE
                scanButtonSample.text = "Value"
                numberInput.visibility = View.VISIBLE
                actionButton.visibility = View.VISIBLE
                numberInput.text = null
                scanStatus.text = ""
                scanButtonSample.text = scannedSample
                //manageScan()
            }
        }

        // Add a TextWatcher to the numberInput for real-time validation. Permits to constrain the user entry from 47.5 to 52.5
        numberInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                weightInput.visibility = View.INVISIBLE
                chooseWeightLabel.visibility = View.INVISIBLE
                val inputText = s.toString()
                val inputNumber = inputText.toFloatOrNull()
                val weightNumber = weightInput.text.toString()
                val smallNumber = weightNumber.toInt() - weightNumber.toDouble()*5/100
                val bigNumber = weightNumber.toInt() + weightNumber.toDouble()*5/100

                if (inputNumber != null && inputNumber >= smallNumber && inputNumber <= bigNumber) {
                    numberInput.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                    actionButton.visibility = View.VISIBLE // Show actionButton if valid
                } else {
                    numberInput.setBackgroundResource(android.R.color.holo_red_light) // Set background to red if not valid
                    actionButton.visibility = View.INVISIBLE // Hide actionButton if not valid
                }
            }
        })

        actionButton.setOnClickListener {
            val inputText = numberInput.text.toString()
            val inputNumber = inputText.toFloatOrNull()

            if (inputNumber != null && inputNumber >= 47.5 && inputNumber <= 52.5) {

                // Define the table url
                val collectionUrl = "http://directus.dbgi.org/items/Lab_Extracts"

                // Function to send data to Directus
                @SuppressLint("DiscouragedApi")
                suspend fun sendDataToDirectus(sampleId: String, weight: String) {
                    val accessToken = retrieveToken()

                    val url = URL(collectionUrl)

                    val extractId = checkExistenceInDirectus(sampleId)

                    if (extractId != null) {

                        val urlConnection =
                            withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }

                        try {
                            urlConnection.requestMethod = "POST"
                            urlConnection.setRequestProperty("Content-Type", "application/json")
                            urlConnection.setRequestProperty(
                                "Authorization",
                                "Bearer $accessToken"
                            )

                            val data = JSONObject().apply {
                                put("lab_extract_id", extractId)
                                put("field_sample_id", sampleId)
                                put("dried_plant_weight", weight)
                            }

                            val outputStream: OutputStream = urlConnection.outputStream
                            val writer = BufferedWriter(withContext(Dispatchers.IO) {
                                OutputStreamWriter(outputStream, "UTF-8")
                            })
                            withContext(Dispatchers.IO) {
                                writer.write(data.toString())
                            }
                            withContext(Dispatchers.IO) {
                                writer.flush()
                            }
                            withContext(Dispatchers.IO) {
                                writer.close()
                            }

                            val responseCode = urlConnection.responseCode
                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                hasTriedAgain = false
                                val inputStream = urlConnection.inputStream
                                val bufferedReader = BufferedReader(
                                    withContext(
                                        Dispatchers.IO
                                    ) {
                                        InputStreamReader(inputStream, "UTF-8")
                                    })
                                val response = StringBuilder()
                                var line: String?
                                while (withContext(Dispatchers.IO) {
                                        bufferedReader.readLine()
                                    }.also { line = it } != null) {
                                    response.append(line)
                                }
                                withContext(Dispatchers.IO) {
                                    bufferedReader.close()
                                }
                                withContext(Dispatchers.IO) {
                                    inputStream.close()
                                }

                                // 'response' contains the response from the server
                                showToast("$extractId correctly added to database")
                                // print label here
                                val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")
                                if (isPrinterConnected == "yes") {
                                    val printerDetails = PrinterDetailsSingleton.printerDetails
                                    // Specify the name of the template file you want to use.
                                    val selectedFileName = "template_dbgi"

                                    // Initialize an input stream by opening the specified file.
                                    val iStream = resources.openRawResource(
                                        resources.getIdentifier(
                                            selectedFileName, "raw",
                                            packageName
                                        )
                                    )
                                    val parts = extractId.split("_")
                                    val sample = "_" + parts[1]
                                    val extract = "_" + parts[2]
                                    val injetemp = "_tmp"
                                    val weightId = extractId + "_tmp"

                                    // Call the SDK method ".getTemplate()" to retrieve its Template Object
                                    val template =
                                        TemplateFactory.getTemplate(iStream, this@WeightingActivity)
                                    // Simple way to iterate through any placeholders to set desired values.
                                    for (placeholder in template.templateData) {
                                        when (placeholder.name) {
                                            "QR" -> {
                                                placeholder.value = weightId
                                            }
                                            "sample" -> {
                                                placeholder.value = sample
                                            }
                                            "extract" -> {
                                                placeholder.value = extract
                                            }
                                            "injection/temp" -> {
                                                placeholder.value = injetemp
                                            }
                                        }
                                    }

                                    val printingOptions = PrintingOptions()
                                    printingOptions.cutOption = CutOption.EndOfJob
                                    printingOptions.numberOfCopies = 1
                                    val r = Runnable {
                                        runOnUiThread {
                                            printerDetails.print(
                                                this,
                                                template,
                                                printingOptions,
                                                null
                                            )
                                        }
                                    }
                                    val printThread = Thread(r)
                                    printThread.start()
                                }


                                // Start a coroutine to delay the next scan by 5 seconds
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(1500)
                                    scanButtonSample.performClick()
                                }
                            } else if (!hasTriedAgain) {
                                    hasTriedAgain = true
                                    val newAccessToken = getNewAccessToken()

                                    if (newAccessToken != null) {
                                        retrieveToken(newAccessToken)
                                        showToast("connection to directus lost, reconnecting...")
                                        // Retry the operation with the new access token
                                        return sendDataToDirectus(sampleId, weight)
                                    }
                                }
                        } finally {
                            urlConnection.disconnect()
                        }
                    } else {
                        showToast("No more available extraction labels")
                    }
                }

                // Usage
                CoroutineScope(Dispatchers.IO).launch {
                    sendDataToDirectus(scanButtonSample.text.toString(), inputNumber.toString())
                }
            }
        }
    }

    // Function that permits to control which extracts are already in the database and increment by one to create a unique one
    private suspend fun checkExistenceInDirectus(sampleId: String): String? {
        for (i in 1..99) {
            val testId = "${sampleId}_${String.format("%02d", i)}"
            val url = URL("http://directus.dbgi.org/items/Lab_Extracts?filter[lab_extract_id][_eq]=$testId")
            val accessToken = retrieveToken()
            val urlConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }

            try {
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("Authorization", "Bearer $accessToken")

                val responseCode = urlConnection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    hasTriedAgain = false
                    // Read the response body
                    val inputStream = urlConnection.inputStream
                    val bufferedReader = BufferedReader(withContext(Dispatchers.IO) {
                        InputStreamReader(inputStream, "UTF-8")
                    })
                    val response = StringBuilder()
                    var line: String?
                    while (withContext(Dispatchers.IO) {
                            bufferedReader.readLine()
                        }.also { line = it } != null) {
                        response.append(line)
                    }
                    withContext(Dispatchers.IO) {
                        bufferedReader.close()
                    }
                    withContext(Dispatchers.IO) {
                        inputStream.close()
                    }

                    // Check if the response is empty
                    if (response.toString() == "{\"data\":[]}") {
                        return testId
                    }
                } else if (!hasTriedAgain) {
                        hasTriedAgain = true
                        val newAccessToken = getNewAccessToken()

                        if (newAccessToken != null) {
                            retrieveToken(newAccessToken)
                            showToast("connection to directus lost, reconnecting...")
                            // Retry the operation with the new access token
                            return checkExistenceInDirectus(sampleId)
                        }
                    }
            } finally {
                urlConnection.disconnect()
            }
        }

        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                return if (isQrScannerActive){
                    QRCodeScannerUtility.stopScanning()
                    isQrScannerActive = false
                    previewView.visibility = View.INVISIBLE
                    flashlightButton.visibility = View.INVISIBLE
                    weightInput.visibility = View.VISIBLE
                    chooseWeightLabel.visibility = View.VISIBLE
                    scanStatus.text = ""
                    if (isObjectScanActive){
                        scanButtonSample.visibility = View.VISIBLE
                    }
                    true
                } else {
                    onBackPressed()
                    true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun getNewAccessToken(): String? {
        // Start a coroutine to perform the network operation
        val deferred = CompletableDeferred<String?>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val username = intent.getStringExtra("USERNAME")
                val password = intent.getStringExtra("PASSWORD")
                val baseUrl = "http://directus.dbgi.org"
                val loginUrl = "$baseUrl/auth/login"
                val url = URL(loginUrl)
                val connection =
                    withContext(Dispatchers.IO) {
                        url.openConnection()
                    } as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val requestBody = "{\"email\":\"$username\",\"password\":\"$password\"}"

                val outputStream: OutputStream = connection.outputStream
                withContext(Dispatchers.IO) {
                    outputStream.write(requestBody.toByteArray())
                }
                withContext(Dispatchers.IO) {
                    outputStream.close()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                    val content = StringBuilder()
                    var inputLine: String?
                    while (withContext(Dispatchers.IO) {
                            `in`.readLine()
                        }.also { inputLine = it } != null) {
                        content.append(inputLine)
                    }
                    withContext(Dispatchers.IO) {
                        `in`.close()
                    }

                    val jsonData = content.toString()
                    val jsonResponse = JSONObject(jsonData)
                    val data = jsonResponse.getJSONObject("data")
                    val accessToken = data.getString("access_token")
                    deferred.complete(accessToken)
                } else {
                    emptyPlace.text = "Database error, please check your connection."
                    deferred.complete(null)
                }
            }catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    emptyPlace.text = "Database error, please check your connection."
                    deferred.complete(null)
                }
            }
        }
        return deferred.await()
    }

    private fun retrieveToken(token: String? = null): String {
        if (token != null) {
            lastAccessToken = token
        }
        return lastAccessToken ?: "null"
    }
}