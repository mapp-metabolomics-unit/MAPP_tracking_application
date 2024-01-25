// 6)
// Only prints a label identical to the one that is scanned.

@file:Suppress("DEPRECATION")

package org.example.mapptracking

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL

class `6PrintActivity` : AppCompatActivity() {

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

        // Set up button click listener for Object QR Scanner
        scanButtonSignaling.setOnClickListener {
            isObjectScanActive = false
            isQrScannerActive = true
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan the sample"
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
                            printLabel(
                                sampleId)
                        }
                    }
                }
            }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private fun printLabel (sampleId: String) {
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

            // Call the SDK method ".getTemplate()" to retrieve its Template Object
            val template =
                TemplateFactory.getTemplate(iStream, this@`6PrintActivity`)
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