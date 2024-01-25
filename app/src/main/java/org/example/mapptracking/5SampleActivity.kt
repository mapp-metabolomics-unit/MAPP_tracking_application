// 5)
// Activity that permits to create records in the nocodb database.
// It takes an original sample code from the client and prints a new mapp code.

@file:Suppress("DEPRECATION")

package org.example.mapptracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.bradysdk.api.printerconnection.CutOption
import com.bradysdk.api.printerconnection.PrintingOptions
import com.bradysdk.printengine.templateinterface.TemplateFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class `5SampleActivity` : AppCompatActivity() {

    private lateinit var batchSpinnerLabel: TextView
    private lateinit var batchSpinner: Spinner
    private lateinit var batchSpinnerInformation: TextView
    private lateinit var labBookSpinnerLabel: TextView
    private lateinit var labBookSpinner: Spinner
    private lateinit var labBookSpinnerInformation: TextView
    private lateinit var pageNumber: EditText
    private lateinit var scanButtonSample: Button
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView

    private var choices: List<String> = mutableListOf("Choose an option")
    private var isObjectScanActive = false
    private var isQrScannerActive = false
    private var maxPage: Int = 0

    @SuppressLint("CutPasteId", "MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        // Initialize views
        batchSpinnerLabel = findViewById(R.id.batchSpinnerLabel)
        batchSpinner = findViewById(R.id.batchSpinner)
        batchSpinnerInformation = findViewById(R.id.batchSpinnerInformation)
        labBookSpinnerLabel = findViewById(R.id.labBookSpinnerLabel)
        labBookSpinner = findViewById(R.id.labBookSpinner)
        labBookSpinnerInformation = findViewById(R.id.labBookSpinnerInformation)
        pageNumber = findViewById(R.id.pageNumber)
        scanButtonSample = findViewById(R.id.scanButtonSample)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        // Fetch values and populate spinner
        fetchValuesAndPopulateBatchSpinner()

        batchSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                    labBookSpinnerLabel.visibility = View.VISIBLE
                    labBookSpinner.visibility = View.VISIBLE
                } else {
                    labBookSpinnerLabel.visibility = View.INVISIBLE
                    labBookSpinner.visibility = View.INVISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        // Fetch values and populate spinner
        fetchValuesAndPopulatelLabBookSpinner()

        labBookSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                    pageNumber.visibility = View.VISIBLE
                } else {
                    pageNumber.visibility = View.INVISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // No action needed
            }
        }

        // Add a TextWatcher to the numberInput for real-time validation. Permits to constrain the user entry from 47.5 to 52.5
        pageNumber.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val inputText = s.toString()
                val inputNumber = inputText.toFloatOrNull()
                val smallNumber = 1
                val bigNumber = maxPage

                if (inputNumber != null && inputNumber >= smallNumber && inputNumber <= bigNumber) {
                    pageNumber.setBackgroundResource(android.R.color.transparent) // Set background to transparent if valid
                    scanButtonSample.visibility = View.VISIBLE
                } else {
                    pageNumber.setBackgroundResource(android.R.color.holo_red_light) // Set background to red if not valid
                    scanButtonSample.visibility = View.INVISIBLE
                }
            }
        })

        // Set up button click listener for Object QR Scanner
        scanButtonSample.setOnClickListener {
            isObjectScanActive = true
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan the sample"
            flashlightButton.visibility = View.VISIBLE
            labBookSpinner.visibility = View.INVISIBLE
            pageNumber.visibility = View.INVISIBLE
            labBookSpinnerLabel.visibility = View.INVISIBLE
            scanButtonSample.visibility = View.INVISIBLE
            QRCodeScannerUtility.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                QRCodeScannerUtility.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                labBookSpinner.visibility = View.VISIBLE
                pageNumber.visibility = View.VISIBLE
                labBookSpinnerLabel.visibility = View.VISIBLE
                scanButtonSample.visibility = View.VISIBLE
                scanButtonSample.text = scannedSample
                scanStatus.text = ""
                manageScan()
            }
        }
    }

    private fun fetchValuesAndPopulateBatchSpinner() {
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val tableId = "mt2q0qk45cqaqjo" // Replace with your table ID
        val viewId = "vwekmipn51fus9j0" // Replace with your view ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records?viewId=$viewId"
        val url = URL(apiUrl)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlConnection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("accept", "application/json")
                urlConnection.setRequestProperty("xc-auth", accessToken)
                urlConnection.setRequestProperty("xc-token", accessToken)

                withContext(Dispatchers.IO) {
                    urlConnection.connect()
                }

                val responseCode = urlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = urlConnection.inputStream
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    println(response)
                    var line: String?

                    while (withContext(Dispatchers.IO) {
                            bufferedReader.readLine()
                        }.also { line = it } != null) {
                        response.append(line)
                    }

                    withContext(Dispatchers.IO) {
                        bufferedReader.close()
                    }

                    // Parse JSON response
                    val jsonArray = JSONObject(response.toString()).getJSONArray("list")
                    val values = ArrayList<String>()
                    val descriptions = HashMap<String, String>()
                    val pageNumbers = HashMap<String, String>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose an option")

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val value = jsonObject.getString("Batches_ID")
                        val description = jsonObject.getString("Batch Short Name")
                        val pageNumber = jsonObject.getString("MAPP_Project Short description")
                        values.add(value)
                        descriptions[value] = description
                        pageNumbers[value] = pageNumber
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@`5SampleActivity`,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        extractionMethodSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        extractionMethodSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                @SuppressLint("SetTextI18n")
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val selectedValue = values[position]
                                        val selectedDescription = descriptions[selectedValue]
                                        val selectedPage = pageNumbers[selectedValue]!!.toInt()
                                        batchInformation.visibility = View.VISIBLE
                                        extractionInformation.text = "Batch description: $selectedDescription, Project description: $selectedPage"
                                        volumeInput.visibility = View.VISIBLE
                                        volumeInput.hint = "Page number (max $selectedPage)"
                                        maxPage = selectedPage
                                    } else {
                                        extractionInformation.visibility = View.INVISIBLE
                                        volumeInput.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    //newExtractionMethod.text = "No suitable referenced method? add it by following this link and restart the application"
                                    volumeInput.visibility = View.INVISIBLE
                                }
                            }
                    }
                } else {
                    showToast("Error: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println(e)
            }
        }
    }

    private fun fetchValuesAndPopulateSpinner() {
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val tableId = "mb5duvvrvthtzbq" // Replace with your table ID
        val viewId = "vwaqhch1xclxi5ye" // Replace with your view ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records?viewId=$viewId"
        val url = URL(apiUrl)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlConnection = withContext(Dispatchers.IO) {
                    url.openConnection() as HttpURLConnection
                }
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("accept", "application/json")
                urlConnection.setRequestProperty("xc-auth", accessToken)
                urlConnection.setRequestProperty("xc-token", accessToken)

                withContext(Dispatchers.IO) {
                    urlConnection.connect()
                }

                val responseCode = urlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = urlConnection.inputStream
                    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
                    val response = StringBuilder()
                    println(response)
                    var line: String?

                    while (withContext(Dispatchers.IO) {
                            bufferedReader.readLine()
                        }.also { line = it } != null) {
                        response.append(line)
                    }

                    withContext(Dispatchers.IO) {
                        bufferedReader.close()
                    }

                    // Parse JSON response
                    val jsonArray = JSONObject(response.toString()).getJSONArray("list")
                    val values = ArrayList<String>()
                    val descriptions = HashMap<String, String>()
                    val pageNumbers = HashMap<String, String>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose an option")

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val value = jsonObject.getString("zp labbook number")
                        val description = jsonObject.getString("Labbook name")
                        val pageNumber = jsonObject.getString("page_number")
                        values.add(value)
                        descriptions[value] = description
                        pageNumbers[value] = pageNumber
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@`5SampleActivity`,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        extractionMethodSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        extractionMethodSpinner.onItemSelectedListener =
                            object : AdapterView.OnItemSelectedListener {
                                @SuppressLint("SetTextI18n")
                                override fun onItemSelected(
                                    parent: AdapterView<*>?,
                                    view: View?,
                                    position: Int,
                                    id: Long
                                ) {
                                    if (position > 0) { // Check if a valid option (not "Choose an option") is selected
                                        val selectedValue = values[position]
                                        val selectedDescription = descriptions[selectedValue]
                                        val selectedPage = pageNumbers[selectedValue]!!.toInt()
                                        extractionInformation.visibility = View.VISIBLE
                                        extractionInformation.text = "Name: $selectedDescription, Pages: $selectedPage"
                                        volumeInput.visibility = View.VISIBLE
                                        volumeInput.hint = "Page number (max $selectedPage)"
                                        maxPage = selectedPage
                                    } else {
                                        extractionInformation.visibility = View.INVISIBLE
                                        volumeInput.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    //newExtractionMethod.text = "No suitable referenced method? add it by following this link and restart the application"
                                    volumeInput.visibility = View.INVISIBLE
                                }
                            }
                    }
                } else {
                    showToast("Error: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println(e)
            }
        }
    }

    @SuppressLint("SetTextI18n", "SuspiciousIndentation")
    @Deprecated("Deprecated in Java")
    fun manageScan() {
        // Counts the spaces left in the rack
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {

                if (isObjectScanActive){
                    val sampleId = scanButtonSample.text.toString()
                    val labBook = labBookSpinner.selectedItem.toString()
                    val pageNumber = pageNumber.text.toString()
                    showToast("page: $pageNumber")
                    CoroutineScope(Dispatchers.IO).launch {
                        sendDataToNocoDB(sampleId, labBook, pageNumber)
                    }
                    }
                }
            }
        }


    // Function to send data to NocoDB
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private suspend fun sendDataToNocoDB(sampleId: String, labBook: String, pageNumber: String) {
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val pageNumberZP = pageNumber//String.format("%02d", pageNumber)

        // Define the table url
        val tableId = "mh129atmks9fich" // Replace with your table ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records"
        val url = URL(apiUrl)

        val urlConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }

        try {
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("accept", "application/json")
            urlConnection.setRequestProperty("xc-auth", accessToken)
            urlConnection.setRequestProperty("xc-token", accessToken)

            val data = JSONObject().apply {
                put("Labbook", labBook.toString())
                put("Page_number", pageNumberZP.toString())
        }



            // Convert the JSON data to bytes
            val postData = data.toString().toByteArray(Charsets.UTF_8)

            println(data)
            println(postData)

            // Set up the output stream to write the data
            val outputStream: OutputStream = urlConnection.outputStream
            withContext(Dispatchers.IO) {
                outputStream.write(postData)
            }

            val responseCode = urlConnection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
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
                showToast("Database correctly updated")

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

                    // Call the SDK method ".getTemplate()" to retrieve its Template Object
                    val template =
                        TemplateFactory.getTemplate(iStream, this@`5SampleActivity`)
                    // Simple way to iterate through any placeholders to set desired values.
                    for (placeholder in template.templateData) {
                        when (placeholder.name) {
                            "code" -> {
                                placeholder.value = sampleId
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

            } else {
                showToast("Database error, please try again")
            }
        } finally {
            urlConnection.disconnect()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                return if (isQrScannerActive){
                    QRCodeScannerUtility.stopScanning()
                    isQrScannerActive = false
                    previewView.visibility = View.INVISIBLE
                    flashlightButton.visibility = View.INVISIBLE
                    extractionMethodSpinner.visibility = View.VISIBLE
                    volumeInput.visibility = View.VISIBLE
                    extractionMethodLabel.visibility = View.VISIBLE
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
}
