
package org.example.dbgitracking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class MoveActivity : Activity() {

    private val ZXING_SCAN_REQUEST = 1 // Arbitrary request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_move)

        // Launch ZXing Barcode Scanner app to scan QR code
        val intent = Intent("com.google.zxing.client.android.SCAN")
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE")

        try {
            startActivityForResult(intent, ZXING_SCAN_REQUEST)
        } catch (e: Exception) {
            // If ZXing Barcode Scanner app is not installed, prompt the user to install it
            Toast.makeText(this, "Please install ZXing Barcode Scanner app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ZXING_SCAN_REQUEST) {
            if (resultCode == RESULT_OK) {
                val contents = data?.getStringExtra("SCAN_RESULT") // Retrieved QR code data
                Toast.makeText(this, "Scanned QR code data: $contents", Toast.LENGTH_LONG).show()
                // Handle the scanned QR code data as needed
            } else if (resultCode == RESULT_CANCELED) {
                // Handle canceled scan
                Toast.makeText(this, "Scan canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
