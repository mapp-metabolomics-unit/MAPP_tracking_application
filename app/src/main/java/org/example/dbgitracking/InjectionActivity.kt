@file:Suppress("DEPRECATION")

package org.example.dbgitracking

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date

@Suppress("NAME_SHADOWING")
class InjectionActivity : AppCompatActivity() {

    private lateinit var newInjectionMethod: TextView
    private lateinit var injectionMethodLabel: TextView
    private lateinit var injectionMethodSpinner: Spinner
    private lateinit var injectionInformation: TextView
    private lateinit var rackInformation: TextView
    private lateinit var scanButtonAliquot: Button
    private lateinit var submitButton:Button
    private var isObjectScanActive = false
    private var hasTriedAgain = false
    private var lastAccessToken: String? = null
    private var rackNumber: String? = null
    private var x: Int = 1
    private var y: Int = 1
    private val scannedDataList = mutableListOf<String>()
    private var initials: String? = null
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView
    private var isQrScannerActive = false

    private var choices: List<String> = mutableListOf("Choose an option")

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_injection)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        // Initialize views
        newInjectionMethod = findViewById(R.id.newInjectionMethod)
        injectionMethodLabel = findViewById(R.id.injectionMethodLabel)
        injectionMethodSpinner = findViewById(R.id.injectionMethodSpinner)
        injectionInformation = findViewById(R.id.injectionInformation)
        rackInformation = findViewById(R.id.rackInformation)
        scanButtonAliquot = findViewById(R.id.scanButtonAliquot)
        submitButton = findViewById(R.id.submitButton)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        val token = intent.getStringExtra("ACCESS_TOKEN").toString()

        // stores the original token
        retrieveToken(token)

        getCurrentUser()

        // Make the link clickable
        val linkTextView: TextView = newInjectionMethod
        val spannableString = SpannableString(linkTextView.text)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val url = "http://directus.dbgi.org/admin/content/Injection_Methods/+"
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            }
        }
        spannableString.setSpan(clickableSpan, 56, 60, spannableString.length)
        linkTextView.text = spannableString
        linkTextView.movementMethod = LinkMovementMethod.getInstance()

        // Fetch values and populate spinner
        fetchValuesAndPopulateSpinner()

        // Set up button click listener for Object QR Scanner
        scanButtonAliquot.setOnClickListener {
            isObjectScanActive = false
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan in row $x column $y"
            flashlightButton.visibility = View.VISIBLE
            scanButtonAliquot.visibility = View.INVISIBLE
            QRCodeScannerUtility.initialize(this, previewView, flashlightButton) { scannedAliquot ->

                // Stop the scanning process after receiving the result
                QRCodeScannerUtility.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonAliquot.visibility = View.VISIBLE
                scanStatus.text = ""
                scanButtonAliquot.text = scannedAliquot
                manageScan()
            }
        }

        submitButton.setOnClickListener {
            csvCreator()
        }
    }

    @SuppressLint("SetTextI18n", "SimpleDateFormat")
    @Deprecated("Deprecated in Java")
    fun manageScan() {
                val aliquot = scanButtonAliquot.text.toString()
                val sdf = SimpleDateFormat("yyyyMMddHHmm")
                val currentDate = sdf.format(Date())
                val data = "${currentDate}_${initials}_${aliquot},,,$x$y,\n"
                handleScannedData(data)
                //sendDataToDirectus()
                // Display scanned information
                showToast("Sample ${scanButtonAliquot.text} injected with ${injectionMethodSpinner.selectedItem} has been added to database")

                // Start a coroutine to delay the next scan by 5 seconds
                run {
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(1500)
                        val rackNumber = retrieveRackNumber()
                        val parts = rackNumber.toString().split("x")
                        val rows = parts[0].toInt()
                        val columns = parts[1].toInt()
                        // Update x and y for the next scan
                        y += 1
                        if (y > columns) {
                            y = 1
                            x += 1
                        }
                        if (x <= rows) {
                            scanButtonAliquot.performClick()
                        }
                        // Check if all positions have been scanned
                        if (x > rows) {
                            showToast("rack is full, select another suffix to fill another rack or submit to create the template file")
                            scanButtonAliquot.visibility = View.INVISIBLE
                            submitButton.visibility = View.VISIBLE
                            // All positions have been scanned, you can add further logic here if needed
                        }
                    }
                }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchValuesAndPopulateSpinner() {
        val accessToken = retrieveToken()
        val apiUrl =
            "http://directus.dbgi.org/items/Injection_Methods" // Replace with your collection URL
        val url = URL(apiUrl)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlConnection =
                    withContext(Dispatchers.IO) {
                        url.openConnection()
                    } as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("Authorization", "Bearer $accessToken")
                withContext(Dispatchers.IO) {
                    urlConnection.connect()
                }

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

                val responseCode = urlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    hasTriedAgain = false
                    // Parse JSON response
                    val jsonArray = JSONObject(response.toString()).getJSONArray("data")
                    val values = ArrayList<String>()
                    val descriptions = HashMap<String, String>()
                    val rackSizes = HashMap<String, String>()

                    // Add "Choose an option" to the list of values
                    values.add("Choose an option")

                    for (i in 0 until jsonArray.length()) {
                        val jsonObject = jsonArray.getJSONObject(i)
                        val value = jsonObject.getString("method_name")
                        val description = jsonObject.getString("method_description")
                        val rack = jsonObject.getString("rack_size_rowxcolumn")
                        values.add(value)
                        descriptions[value] = description
                        rackSizes[value] = rack
                    }

                    runOnUiThread {
                        // Populate spinner with values
                        choices = values // Update choices list
                        val adapter = ArrayAdapter(
                            this@InjectionActivity,
                            android.R.layout.simple_spinner_item,
                            values
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        injectionMethodSpinner.adapter = adapter

                        // Add an OnItemSelectedListener to update newExtractionMethod text and handle visibility
                        injectionMethodSpinner.onItemSelectedListener =
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
                                        val rackSize = rackSizes[selectedValue].toString()
                                        if (rackSize != "null" && selectedValue != "Choose an option") {
                                            val parts = rackSize.split("x")
                                            val rows = parts[0]
                                            val columns = parts[1]
                                            rackInformation.visibility = View.VISIBLE
                                            rackInformation.setTextColor(Color.GRAY)
                                            rackInformation.text = "rack defined with $rows rows and $columns columns"
                                            scanButtonAliquot.visibility = View.VISIBLE
                                            retrieveRackNumber(rackSize)
                                        } else {
                                            rackInformation.visibility = View.VISIBLE
                                            rackInformation.setTextColor(Color.RED)
                                            rackInformation.text = "No rack dimensions defined, please fill the Rack Size Rowxvolumn here in format rowxcolumn"
                                            // Make the link clickable
                                            val linkTextView: TextView = rackInformation
                                            val spannableString = SpannableString(linkTextView.text)
                                            val clickableSpan = object : ClickableSpan() {
                                                override fun onClick(widget: View) {
                                                    val url = "http://directus.dbgi.org/admin/content/Injection_Methods/$selectedValue"
                                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                    startActivity(browserIntent)
                                                }
                                            }
                                            spannableString.setSpan(clickableSpan, 65, 69, spannableString.length)
                                            linkTextView.text = spannableString
                                            linkTextView.movementMethod = LinkMovementMethod.getInstance()
                                            scanButtonAliquot.visibility = View.INVISIBLE
                                        }
                                        injectionInformation.visibility = View.VISIBLE
                                        injectionInformation.text = selectedDescription
                                    } else {
                                        scanButtonAliquot.visibility = View.INVISIBLE
                                        rackInformation.visibility = View.INVISIBLE
                                        injectionInformation.visibility = View.INVISIBLE

                                    }
                                }

                                override fun onNothingSelected(parent: AdapterView<*>?) {
                                    //newExtractionMethod.text = "No suitable referenced method? add it by following this link and restart the application"
                                }
                            }
                    }
                } else if (!hasTriedAgain) {
                    hasTriedAgain = true
                    val newAccessToken = getNewAccessToken()

                    if (newAccessToken != null) {
                        retrieveToken(newAccessToken)
                        showToast("connection to directus lost, reconnecting...")
                        // Retry the operation with the new access token
                        return@launch fetchValuesAndPopulateSpinner()
                    } else {
                        showToast("Connection error")
                    }
                } else {
                    showToast("Error: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun retrieveToken(token: String? = null): String {
        if (token != null) {
            lastAccessToken = token
        }
        return lastAccessToken ?: "null"
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
                    showToast("Database error, please check your connection.")
                    deferred.complete(null)
                }
            }catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Database error, please check your connection.")
                    deferred.complete(null)
                }
            }
        }
        return deferred.await()
    }
    private fun retrieveRackNumber(number: String? = null): String? {
        if (number != null) {
            rackNumber = number

        }
        return rackNumber
    }

    @SuppressLint("SimpleDateFormat")
    private fun csvCreator() {
        try {
            val file = File(filesDir, "temp.csv")
            val fileWriter = FileWriter(file)
            fileWriter.write(scannedDataList.toString())
            fileWriter.flush()
            fileWriter.close()

            //uploadFileToNextcloud(file)
            uploadFileToNextcloud()
        } catch (e: IOException) {
            showToast("error: $e")
        }
    }

    // Function to handle scanned data
    private fun handleScannedData(data: String) {
        scannedDataList.add(data)
    }

    private fun getCurrentUser() {
        val accessToken = retrieveToken()
        val url = URL("http://directus.dbgi.org/users/me") // Replace with your Directus URL

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val urlConnection =
                    withContext(Dispatchers.IO) {
                        url.openConnection()
                    } as HttpURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.setRequestProperty("Authorization", "Bearer $accessToken")
                withContext(Dispatchers.IO) {
                    urlConnection.connect()
                }

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

                val responseCode = urlConnection.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    hasTriedAgain = false
                    // Parse JSON response
                    val jsonArray = JSONObject(response.toString()).getJSONObject("data")
                    val firstName = jsonArray.getString("first_name")
                    val lastName = jsonArray.getString("last_name")
                    initials = "${firstName[0]}${lastName[0]}"

                } else if (!hasTriedAgain) {
                    hasTriedAgain = true
                    val newAccessToken = getNewAccessToken()

                    if (newAccessToken != null) {
                        retrieveToken(newAccessToken)
                        showToast("connection to directus lost, reconnecting...")
                        // Retry the operation with the new access token
                        return@launch getCurrentUser()
                    } else {
                        showToast("Connection error")
                    }
                } else {
                    showToast("Error: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun uploadFileToNextcloud() {
        val sdf = SimpleDateFormat("yyyyMMddHHmm")
        val file = "${sdf}_${injectionMethodSpinner.selectedItem}_$initials"
        val deferred = CompletableDeferred<String?>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val username = "DBGI"
                val password = "dbgi_dbgi_dbgi"
                val baseUrl = "http://83.77.116.250:8888/"
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
                showToast("code: $responseCode")
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
                    showToast(accessToken)
                } else {
                    showToast("Database error, please check your connection.")
                    deferred.complete(null)
                }
            }catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    showToast("Database error, please check your connection.")
                    deferred.complete(null)
                }
            }
        }

        deleteTempCsvFile()
    }

    private fun deleteTempCsvFile() {
        val file = File(filesDir, "temp.csv")
        file.delete()
    }
}