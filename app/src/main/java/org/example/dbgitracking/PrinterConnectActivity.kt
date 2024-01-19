package org.example.dbgitracking

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class PrinterConnectActivity : AppCompatActivity() {
    
    private lateinit var connectText: TextView
    private lateinit var connectButton: Button
    private lateinit var ignoreButton: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_connect)

        // Initialize views
        connectText = findViewById(R.id.connectText)
        connectButton = findViewById(R.id.connectButton)
        ignoreButton = findViewById(R.id.ignoreButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")
        val isPrinterConnected = "no"

        // Set up button click listeners here
        connectButton.setOnClickListener {
            val intent = Intent(this,ManagePrinterActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
            finish()
        }
        ignoreButton.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
            finish()
        }
    }
}