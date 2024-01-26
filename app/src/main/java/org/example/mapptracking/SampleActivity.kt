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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

@Suppress("NAME_SHADOWING")
class SampleActivity : AppCompatActivity() {

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
    private var batchNcRecordId: String = ""

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
        fetchValuesAndPopulateLabBookSpinner()

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
        val tableId = "mkoh26oqevas69p" // Replace with your table ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records?sort=Batches_ID&limit=1000"
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
                    val batchDescriptions = HashMap<String, String>()
                    val projectDescriptions = HashMap<String, String>()
                    val ncRecordIds = HashMap<String, String>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose an option")

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val value = jsonObject.getString("Batches_ID")
                        val batchDescription = jsonObject.getString("Batch Short Name")
                        val projectDescription = jsonObject.getString("MAPP_Project Short description")
                        val ncRecordId = jsonObject.getString("ncRecordId")
                        values.add(value)
                        batchDescriptions[value] = batchDescription
                        projectDescriptions[value] = projectDescription
                        ncRecordIds[value] = ncRecordId
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@SampleActivity,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        batchSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        batchSpinner.onItemSelectedListener =
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
                                        val selectedBatchDescription = batchDescriptions[selectedValue]
                                        val selectedProjectDescription = projectDescriptions[selectedValue]
                                        val selectedncRecordId = ncRecordIds[selectedValue]
                                        batchNcRecordId = selectedncRecordId.toString()
                                        batchSpinnerInformation.visibility = View.VISIBLE
                                        batchSpinnerInformation.text = "Batch description: $selectedBatchDescription, Project description: $selectedProjectDescription"
                                        labBookSpinnerLabel.visibility = View.VISIBLE
                                        labBookSpinner.visibility = View.VISIBLE
                                    } else {
                                        batchSpinnerInformation.visibility = View.INVISIBLE
                                        labBookSpinnerLabel.visibility = View.INVISIBLE
                                        labBookSpinner.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    batchSpinnerInformation.visibility = View.INVISIBLE
                                    labBookSpinnerLabel.visibility = View.INVISIBLE
                                    labBookSpinner.visibility = View.INVISIBLE
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

    private fun fetchValuesAndPopulateLabBookSpinner() {
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
                            this@SampleActivity,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        labBookSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        labBookSpinner.onItemSelectedListener =
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
                                        labBookSpinnerInformation.visibility = View.VISIBLE
                                        labBookSpinnerInformation.text = "Name: $selectedDescription, Pages: $selectedPage"
                                        pageNumber.visibility = View.VISIBLE
                                        pageNumber.hint = "Page number (max $selectedPage)"
                                        maxPage = selectedPage
                                    } else {
                                        labBookSpinnerInformation.visibility = View.INVISIBLE
                                        pageNumber.visibility = View.INVISIBLE
                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    //newExtractionMethod.text = "No suitable referenced method? add it by following this link and restart the application"
                                    pageNumber.visibility = View.INVISIBLE
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
        val metadataId = retrieveMetadataId(sampleId)
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val zpPage = pageNumber.padStart(2, '0')

        // Define the table url
        val tableId = "mifbpx143i29sjf" // Replace with your table ID
        val link2Control = "c589dtcb5sj3jfn"
        val linkMAPPBatch = "cqdqhdh11r7j13e"
        val linkMetadata = "cytjr67dejq7osc"

        val apiUrlPost = "http://134.21.20.118:8080/api/v2/tables/$tableId/records"
        val urlPost = URL(apiUrlPost)
        val urlConnectionPost = withContext(Dispatchers.IO) { urlPost.openConnection() as HttpURLConnection }

        try {
            urlConnectionPost.requestMethod = "POST"
            urlConnectionPost.setRequestProperty("accept", "application/json")
            urlConnectionPost.setRequestProperty("Content-Type", "application/json")
            urlConnectionPost.setRequestProperty("xc-auth", accessToken)
            urlConnectionPost.setRequestProperty("xc-token", accessToken)

            // Define the data in the expected format for NocoDB
            val recordData = JSONArray().apply {
                put(JSONObject().apply {
                    put("Labbook", labBook)
                    put("Page_number", zpPage)
                })
            }

            // Convert the JSON data to bytes
            val postData = recordData.toString().toByteArray(Charsets.UTF_8)

            // Set up the output stream to write the data
            val outputStream: OutputStream = urlConnectionPost.outputStream
            withContext(Dispatchers.IO) {
                outputStream.write(postData)
            }

            val responseCode = urlConnectionPost.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = urlConnectionPost.inputStream
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

                // Parse the JSON array to extract the ncRecordId
                val jsonArray = JSONArray(response.toString())
                val jsonObject = jsonArray.getJSONObject(0)
                val ncRecordId = jsonObject.optString("ncRecordId", "")

                // Define the link API URL
                val link2ApiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/links/$link2Control/records/$ncRecordId"
                val link2Url = URL(link2ApiUrl)

                val link2UrlConnection = withContext(Dispatchers.IO) { link2Url.openConnection() as HttpURLConnection }

                link2UrlConnection.requestMethod = "POST"
                link2UrlConnection.setRequestProperty("accept", "application/json")
                link2UrlConnection.setRequestProperty("Content-Type", "application/json")
                link2UrlConnection.setRequestProperty("xc-auth", accessToken)
                link2UrlConnection.setRequestProperty("xc-token", accessToken)

                // Define the data for linking records
                val link2Data = JSONArray().apply {
                    put(JSONObject().apply {
                        put("ncRecordId", ncRecordId)
                        put("ncRecordId", "recbK2AGqImpC88qC")
                    })
                }

                // Convert the JSON data to bytes
                val post2Data = link2Data.toString().toByteArray(Charsets.UTF_8)

                // Set up the output stream to write the data
                val link2OutputStream: OutputStream = link2UrlConnection.outputStream
                withContext(Dispatchers.IO) {
                    link2OutputStream.write(post2Data)
                }

                // Very strange but these 2 lines under are really important to perform a successful link
                val link2ResponseCode = link2UrlConnection.responseCode
                println("$link2ResponseCode")

                // Define the link API URL
                val linkBApiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/links/$linkMAPPBatch/records/$ncRecordId"
                val linkBUrl = URL(linkBApiUrl)

                val linkBUrlConnection = withContext(Dispatchers.IO) { linkBUrl.openConnection() as HttpURLConnection }

                linkBUrlConnection.requestMethod = "POST"
                linkBUrlConnection.setRequestProperty("accept", "application/json")
                linkBUrlConnection.setRequestProperty("Content-Type", "application/json")
                linkBUrlConnection.setRequestProperty("xc-auth", accessToken)
                linkBUrlConnection.setRequestProperty("xc-token", accessToken)

                // Define the data for linking records
                val linkBData = JSONArray().apply {
                    put(JSONObject().apply {
                        put("ncRecordId", ncRecordId)
                        put("ncRecordId", batchNcRecordId)
                    })
                }

                // Convert the JSON data to bytes
                val postBData = linkBData.toString().toByteArray(Charsets.UTF_8)

                // Set up the output stream to write the data
                val linkBOutputStream: OutputStream = linkBUrlConnection.outputStream
                withContext(Dispatchers.IO) {
                    linkBOutputStream.write(postBData)
                }

                // Very strange but these 2 lines under are really important to perform a successful link
                val linkBResponseCode = linkBUrlConnection.responseCode
                println(linkBResponseCode)

                // Define the link API URL
                val linkMApiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/links/$linkMetadata/records/$ncRecordId"
                val linkMUrl = URL(linkMApiUrl)

                val linkMUrlConnection = withContext(Dispatchers.IO) { linkMUrl.openConnection() as HttpURLConnection }

                linkMUrlConnection.requestMethod = "POST"
                linkMUrlConnection.setRequestProperty("accept", "application/json")
                linkMUrlConnection.setRequestProperty("Content-Type", "application/json")
                linkMUrlConnection.setRequestProperty("xc-auth", accessToken)
                linkMUrlConnection.setRequestProperty("xc-token", accessToken)

                // Define the data for linking records
                val linkMData = JSONArray().apply {
                    put(JSONObject().apply {
                        put("ncRecordId", ncRecordId)
                        put("id", metadataId)
                    })
                }

                // Convert the JSON data to bytes
                val postMData = linkMData.toString().toByteArray(Charsets.UTF_8)

                // Set up the output stream to write the data
                val linkMOutputStream: OutputStream = linkMUrlConnection.outputStream
                withContext(Dispatchers.IO) {
                    linkMOutputStream.write(postMData)
                }

                // Very strange but these 2 lines under are really important to perform a successful link
                val linkMResponseCode = linkMUrlConnection.responseCode
                println(linkMResponseCode)

                    // 'response' contains the JSON response from the server
                showToast("Database correctly updated")

                val mappId = getMappId(ncRecordId)

                showToast("mapp code: $mappId")

                // print label here
                val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")
                if (isPrinterConnected == "yes") {
                    val printerDetails = PrinterDetailsSingleton.printerDetails
                    // Specify the name of the template file you want to use.
                    val selectedFileName = "template_mapp"

                    // Initialize an input stream by opening the specified file.
                    val iStream = resources.openRawResource(
                        resources.getIdentifier(
                            selectedFileName, "raw",
                            packageName
                        )
                    )

                    val parts = mappId.toString().split("_")
                    val labBook = "_" + parts[1]
                    val page = "_" + parts[2]
                    val variant = "_" + parts[3]

                    // Call the SDK method ".getTemplate()" to retrieve its Template Object
                    val template =
                        TemplateFactory.getTemplate(iStream, this@SampleActivity)
                    // Simple way to iterate through any placeholders to set desired values.
                    for (placeholder in template.templateData) {
                        when (placeholder.name) {
                            "QR" -> {
                                placeholder.value = mappId
                            }
                            "labBook" -> {
                                placeholder.value = labBook
                            }
                            "page" -> {
                                placeholder.value = page
                            }
                            "variant" -> {
                                placeholder.value = variant
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

                withContext(Dispatchers.Main) {
                    delay(1500)
                    scanButtonSample.performClick()
                }

            } else {
                showToast("Database error, please try again")
            }
        } finally {
            urlConnectionPost.disconnect()
        }
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private suspend fun retrieveMetadataId(sampleId: String): String? {
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val tableId = "mpnz61lt2anwk8s" // Replace with your table ID
        val viewId = "vwoqm6iyl6k9hr77" // Replace with your view ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records?viewId=$viewId"
        val url = URL(apiUrl)

        // Use CompletableDeferred to wait for the result
        val resultDeferred = CompletableDeferred<String?>()

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
                    val ids = HashMap<String, String>()

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val value = jsonObject.getString("Sample_ID_original")
                        val id = jsonObject.getString("Id")
                        ids[value] = id
                    }

                    val selectedId = ids[sampleId]

                    // Set the result to the deferred
                    resultDeferred.complete(selectedId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println(e)

                // Set null to the deferred in case of an exception
                resultDeferred.complete(null)
            }
        }

        // Await for the result and return it
        return resultDeferred.await()
    }

    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private suspend fun getMappId(ncRecordId: String): String? {
        val accessToken = intent.getStringExtra("ACCESS_TOKEN").toString()
        val tableId = "mifbpx143i29sjf" // Replace with your table ID
        val apiUrl = "http://134.21.20.118:8080/api/v2/tables/$tableId/records/$ncRecordId?fields=Final"
        val url = URL(apiUrl)

        // Use CompletableDeferred to wait for the result
        val resultDeferred = CompletableDeferred<String?>()

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
                    val jsonObject = JSONObject(response.toString())
                    val stringValue = jsonObject.getString("Final")

                    // Set the result to the deferred
                    resultDeferred.complete(stringValue)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                println(e)

                // Set null to the deferred in case of an exception
                resultDeferred.complete(null)
            }
        }

        // Await for the result and return it
        return resultDeferred.await()
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                return if (isQrScannerActive){
                    QRCodeScannerUtility.stopScanning()
                    isQrScannerActive = false
                    previewView.visibility = View.INVISIBLE
                    flashlightButton.visibility = View.INVISIBLE
                    batchSpinner.visibility = View.VISIBLE
                    labBookSpinner.visibility = View.VISIBLE
                    pageNumber.visibility = View.VISIBLE
                    batchSpinnerLabel.visibility = View.VISIBLE
                    labBookSpinnerLabel.visibility = View.VISIBLE
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
