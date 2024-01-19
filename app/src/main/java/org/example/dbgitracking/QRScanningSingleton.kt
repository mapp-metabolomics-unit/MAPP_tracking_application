package org.example.dbgitracking

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
object QRCodeScannerUtility {

    private const val PREF_FILE_NAME = "QRCodeScannerPrefs"
    private const val PREF_FLASHLIGHT_STATE = "isFlashlightOn"

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var scanner: BarcodeScanner
    private var flashlightButton: Button? = null

    private lateinit var cameraExecutor: ExecutorService
    private var previewView: PreviewView? = null
    private var isFlashlightOn = false
    private var scanResultListener: ((String) -> Unit)? = null
    private lateinit var sharedPreferences: SharedPreferences

    // Modify initialize function to return the scanned value
    fun initialize(context: Context, previewView: PreviewView, flashlightButton: Button, callback: (String) -> Unit) {
        this.previewView = previewView
        cameraExecutor = Executors.newSingleThreadExecutor()
        scanner = createBarcodeScanner()
        startCamera(context)

        this.flashlightButton = flashlightButton

        this.flashlightButton?.setOnClickListener {
            toggleFlashlight()
        }

        // Initialize SharedPreferences
        sharedPreferences = context.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE)

        // Restore flashlight state from SharedPreferences
        isFlashlightOn = sharedPreferences.getBoolean(PREF_FLASHLIGHT_STATE, false)

        // If flashlight was on, turn it on
        if (isFlashlightOn) {
            turnOnFlashlight()
        }

        // Set the scan result listener to the provided callback function
        setScanResultListener(callback)
    }

    // Function to set a listener to retrieve scanned data
    private fun setScanResultListener(listener: (String) -> Unit) {
        this.scanResultListener = listener
    }

    // Function to wait for the scanned result
    private fun waitForScanResult() {
        // No longer needed as the result will be provided via the listener/callback
    }


    private fun createBarcodeScanner(): BarcodeScanner {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        return BarcodeScanning.getClient(options)
    }

    private fun startCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreviewAndAnalysis(context)
        }, ContextCompat.getMainExecutor(context))
    }

    private fun bindPreviewAndAnalysis(context: Context) {
        val preview = Preview.Builder().build()
        preview.setSurfaceProvider(previewView?.surfaceProvider)

        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer(context))
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(context as AppCompatActivity, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            // Handle exceptions
        }
    }

    private var scannedResult: String? = null

    private class QRCodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {
        @OptIn(ExperimentalGetImage::class) override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val inputImage = InputImage.fromMediaImage(
                    mediaImage, imageProxy.imageInfo.rotationDegrees
                )

                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            val qrData = barcode.rawValue
                            scannedResult = qrData // Store the scanned result
                            if (qrData != null) {
                                scanResultListener?.invoke(qrData)
                            } // Invoke listener to notify scanned result
                            println(qrData)
                        }
                    }
                    .addOnFailureListener {
                        // Handle failure or exceptions
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }
    }

    fun stopScanning() {
        // Save the flashlight state to SharedPreferences when stopping scanning
        sharedPreferences.edit().putBoolean(PREF_FLASHLIGHT_STATE, isFlashlightOn).apply()

        cameraProvider.unbindAll()
        cameraExecutor.shutdown()
    }

    private fun toggleFlashlight() {
        if (isFlashlightOn) {
            turnOffFlashlight()
        } else {
            turnOnFlashlight()
        }
    }

    // Function to turn on the flashlight
    @SuppressLint("SetTextI18n")
    private fun turnOnFlashlight() {
        isFlashlightOn = true

        try {
            val cameraInfo = camera.cameraInfo
            if (cameraInfo.hasFlashUnit()) {
                val cameraControl = camera.cameraControl
                cameraControl.enableTorch(true)
            } else {
                // Handle cases where the device doesn't have a flash unit
                // You can display a message or handle it as needed
            }
        } catch (e: CameraInfoUnavailableException) {
            // Handle exceptions if there's an issue with accessing camera information
        }
    }

    // Function to turn off the flashlight
    @SuppressLint("SetTextI18n")
    private fun turnOffFlashlight() {
        isFlashlightOn = false
        try {
            camera.cameraControl.enableTorch(false)
        } catch (_: CameraInfoUnavailableException) {
        }
    }
}
