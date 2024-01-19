@file:Suppress("DEPRECATION")

package org.example.dbgitracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SignalingScanActivity : AppCompatActivity() {

    private lateinit var scanSignalingLabel: TextView
    private lateinit var scanButtonSignaling: Button
    private var isObjectScanActive = false
    private var hasTriedAgain = false
    private var lastAccessToken: String? = null
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView
    private var isQrScannerActive = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signaling_scan)

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        scanSignalingLabel = findViewById(R.id.scanSignalingLabel)
        scanButtonSignaling = findViewById(R.id.scanButtonSignaling)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        val token = intent.getStringExtra("ACCESS_TOKEN").toString()

        // stores the original token
        retrieveToken(token)

        // Set up button click listener for Object QR Scanner
        scanButtonSignaling.setOnClickListener {
            isObjectScanActive = false
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan the sample to signal"
            flashlightButton.visibility = View.VISIBLE
            scanButtonSignaling.visibility = View.INVISIBLE
            QRCodeScannerUtility.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                QRCodeScannerUtility.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonSignaling.visibility = View.VISIBLE
                scanStatus.text = ""
                scanButtonSignaling.text = scannedSample
                manageScan()
            }
        }

    }

    @SuppressLint("SetTextI18n")
    @Deprecated("Deprecated in Java")
    fun manageScan() {

        // Counts the spaces left in the rack
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {

                        if (isObjectScanActive) {

                            val sampleId = scanButtonSignaling.text.toString()
                            showToast("sample id: $sampleId")
                            when {
                                sampleId.matches(Regex("^dbgi_\\d{6}\$")) -> {
                                    val collection = "Field_Samples"
                                    withContext(Dispatchers.IO) {
                                        sendDataToDirectus(
                                            sampleId, collection)
                                    }
                                }
                                sampleId.matches(Regex("^dbgi_\\d{6}_\\d{2}\$")) -> {
                                    val collection = "Lab_Extracts"
                                    withContext(Dispatchers.IO) {
                                        sendDataToDirectus(
                                            sampleId, collection)
                                    }
                                }
                                sampleId.matches(Regex("^dbgi_\\d{6}_\\d{2}_\\d{2}\$")) -> {
                                    val collection = "Aliquots"
                                    withContext(Dispatchers.IO) {
                                        sendDataToDirectus(
                                            sampleId, collection)
                                    }
                                }
                                sampleId.matches(Regex("^dbgi_batch_blk_\\d{6}\$")) -> {
                                    val collection = "Batch"
                                    val parts = sampleId.split("_")
                                    val batchId = parts[0] + "_" + parts[1] + "_" + parts[3]
                                    withContext(Dispatchers.IO) {
                                        sendDataToDirectus(
                                            batchId, collection)
                                    }
                                }
                                else -> {
                                    // Default case if none of the patterns match
                                    showToast("You are trying to signal an invalid code")
                                }
                            }
                        }
                    }
                }
            }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n")
    private suspend fun sendDataToDirectus(
        sampleId: String,
        collection: String
    ) {
        // Define the table url
        val accessToken = retrieveToken()
        val collectionUrl = "http://directus.dbgi.org/items/$collection/$sampleId"
        val url = URL(collectionUrl)
        val urlConnection = withContext(Dispatchers.IO) { url.openConnection() as HttpURLConnection }

        // Perform the POST request to add the values on directus
        try {
            urlConnection.requestMethod = "PATCH"
            urlConnection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            urlConnection.setRequestProperty(
                "Authorization",
                "Bearer $accessToken"
            )

            val data = JSONObject().apply {
                put("status", "NOTOK")
                put("mobile_container_id", "absent")
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

            // Capture the response code and control if it's successful
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

                // Display a Toast with the response message
                showToast("$sampleId correctly updated")
                delay(1500)
                scanButtonSignaling.performClick()
            } else if (!hasTriedAgain) {
                    hasTriedAgain = true
                    val newAccessToken = getNewAccessToken()

                    if (newAccessToken != null) {
                        retrieveToken(newAccessToken)
                        showToast("connection to directus lost, reconnecting...")
                        // Retry the operation with the new access token
                        return sendDataToDirectus(sampleId, collection)
                    }
                // Request failed
                showToast("Error: $responseCode")
            }
        } finally {
            urlConnection.disconnect()
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
    private fun retrieveToken(token: String? = null): String {
        if (token != null) {
            lastAccessToken = token
        }
        return lastAccessToken ?: "null"
    }
}