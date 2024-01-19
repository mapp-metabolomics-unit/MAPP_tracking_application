package org.example.dbgitracking

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LocationActivity : AppCompatActivity() {

    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var previewView: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var flashLightButton: Button
    private lateinit var scanner: BarcodeScanner

    private var isFlashlightOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        previewView = findViewById(R.id.previewView)
        flashLightButton = findViewById(R.id.flashlightButton)
        cameraExecutor = Executors.newSingleThreadExecutor()
        scanner = createBarcodeScanner()

        flashLightButton.setOnClickListener {
            toggleFlashlight()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun createBarcodeScanner(): BarcodeScanner {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        return BarcodeScanning.getClient(options)
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreviewAndAnalysis()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindPreviewAndAnalysis() {
        val preview = Preview.Builder().build()

        val imageAnalyzer = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(cameraExecutor, QRCodeAnalyzer())
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        preview.setSurfaceProvider(previewView.surfaceProvider)

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
        } catch (exc: Exception) {
            // Handle exceptions
        }
    }

    private inner class QRCodeAnalyzer : ImageAnalysis.Analyzer {
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
                            runOnUiThread {
                                Toast.makeText(
                                    this@LocationActivity,
                                    "Scanned QR code data: $qrData",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // Process the QR code data as needed
                                stopCamera()
                            }
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

    private fun stopCamera() {
        cameraProvider.unbindAll()
    }

    fun toggleFlashlight() {
        if (!isFlashlightOn){
            turnOnFlashlight()
        } else {
            turnOffFlashlight()
        }
    }

    // Function to turn on the flashlight
    @SuppressLint("SetTextI18n")
    private fun turnOnFlashlight() {
        isFlashlightOn = true
        flashLightButton.text = "light off"

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
        flashLightButton.text = "light on"
        try {
            camera.cameraControl.enableTorch(false)
        } catch (_: CameraInfoUnavailableException) {
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    fun startQrCodeScanning() {
        startCamera() // Initiate camera and QR code scanning
    }

}

