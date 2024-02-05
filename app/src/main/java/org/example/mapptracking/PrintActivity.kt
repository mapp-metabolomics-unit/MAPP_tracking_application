// 6)
// Only prints a label identical to the one that is scanned.

@file:Suppress("DEPRECATION")

package org.example.mapptracking

import android.annotation.SuppressLint
import android.graphics.Color
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrintActivity : AppCompatActivity() {

    private lateinit var printerStatus: TextView
    private lateinit var printLabel: TextView
    private lateinit var scanButtonPrinting: Button
    private lateinit var previewView: PreviewView
    private lateinit var flashlightButton: Button
    private lateinit var scanStatus: TextView

    private var isQrScannerActive = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        printerStatus = findViewById(R.id.printerStatus)
        printLabel = findViewById(R.id.printLabel)
        scanButtonPrinting = findViewById(R.id.scanButtonPrinting)
        previewView = findViewById(R.id.previewView)
        flashlightButton = findViewById(R.id.flashlightButton)
        scanStatus = findViewById(R.id.scanStatus)

        val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

        if(isPrinterConnected == "yes"){

            printerStatus.setTextColor(Color.GREEN)
            printerStatus.text = "Printer connected"
        } else {
            printerStatus.setTextColor(Color.RED)
            printerStatus.text = "No printer"
        }

        // Set up button click listener for Object QR Scanner
        scanButtonPrinting.setOnClickListener {
            previewView.visibility = View.VISIBLE
            scanStatus.text = "Scan the sample"
            flashlightButton.visibility = View.VISIBLE
            scanButtonPrinting.visibility = View.INVISIBLE
            QRCodeScannerUtility.initialize(this, previewView, flashlightButton) { scannedSample ->

                // Stop the scanning process after receiving the result
                QRCodeScannerUtility.stopScanning()
                isQrScannerActive = false
                previewView.visibility = View.INVISIBLE
                flashlightButton.visibility = View.INVISIBLE
                scanButtonPrinting.visibility = View.VISIBLE
                scanStatus.text = ""
                scanButtonPrinting.text = scannedSample
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

                val sampleId = scanButtonPrinting.text.toString()
                printLabel(
                    sampleId)
                    }
                }
            }

    // Function to send data to Directus
    @SuppressLint("SetTextI18n", "DiscouragedApi")
    private suspend fun printLabel (sampleId: String) {
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

            val parts = sampleId.split("_")
            val labBook = "_" + parts[1]
            val page = "_" + parts[2]
            val variant = "_" + parts[3]

            // Call the SDK method ".getTemplate()" to retrieve its Template Object
            val template =
                TemplateFactory.getTemplate(iStream, this@PrintActivity)
            // Simple way to iterate through any placeholders to set desired values.
            for (placeholder in template.templateData) {
                when (placeholder.name) {
                    "QR" -> {
                        placeholder.value = sampleId
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

            showToast("printing $sampleId...")

            delay(1500)
            scanButtonPrinting.performClick()

        } else{
            showToast("Please connect a printer to perform this task")
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
}