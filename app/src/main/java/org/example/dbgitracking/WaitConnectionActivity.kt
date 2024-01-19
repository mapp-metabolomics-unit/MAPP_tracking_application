package org.example.dbgitracking

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bradysdk.api.printerdiscovery.PrinterDiscovery

class WaitConnectionActivity : AppCompatActivity() {
    private lateinit var printerDiscovery: PrinterDiscovery
    private lateinit var connectionLabel: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wait_connection)

        connectionLabel = findViewById(R.id.connectionLabel)

        printerDiscovery = PrinterDetailsSingleton.printerDiscovery
        val isNewConnection = intent.getStringExtra("IS_NEW_CONNECTION")

        if(isNewConnection == "false") {

            if (printerDiscovery.haveOwnership != null) {
                showToast("Connected!")
                val accessToken = intent.getStringExtra("ACCESS_TOKEN")
                val username = intent.getStringExtra("USERNAME")
                val password = intent.getStringExtra("PASSWORD")
                val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

                val intent = Intent(this, HomePageActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
                startActivity(intent)
                finish()
            } else {
                val accessToken = intent.getStringExtra("ACCESS_TOKEN")
                val username = intent.getStringExtra("USERNAME")
                val password = intent.getStringExtra("PASSWORD")

                val intent = Intent(this, ManagePrinterActivity::class.java)
                intent.putExtra("ACCESS_TOKEN", accessToken)
                intent.putExtra("USERNAME", username)
                intent.putExtra("PASSWORD", password)
                startActivity(intent)
                finish()
            }
        } else{
            showToast("Connected!")
            val accessToken = intent.getStringExtra("ACCESS_TOKEN")
            val username = intent.getStringExtra("USERNAME")
            val password = intent.getStringExtra("PASSWORD")
            val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

            val intent = Intent(this, HomePageActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
            finish()
        }

    }

    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_SHORT).show() }
    }

}