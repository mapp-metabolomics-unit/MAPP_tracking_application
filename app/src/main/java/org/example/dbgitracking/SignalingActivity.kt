package org.example.dbgitracking

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SignalingActivity : AppCompatActivity() {
    // Initiate the displayed objects
    private lateinit var informationLabel: TextView
    private lateinit var scanSignaling: Button
    private lateinit var manualSignaling: Button

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Create the connection with the XML file to add the displayed objects
        setContentView(R.layout.activity_signaling)

        // Add the back arrow to this screen
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_back_arrow)

        // Initialize objects views
        informationLabel = findViewById(R.id.informationLabel)
        scanSignaling = findViewById(R.id.scanSignaling)
        manualSignaling = findViewById(R.id.manualSignaling)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")

        // Set up button click listener for Object QR Scanner
        scanSignaling.setOnClickListener {
            val intent = Intent(this, SignalingScanActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
        manualSignaling.setOnClickListener {
            val intent = Intent(this, SignalingManActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
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
}