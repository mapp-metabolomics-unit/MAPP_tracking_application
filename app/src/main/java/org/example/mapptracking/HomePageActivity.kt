// 4)
// Homepage screen that redirects to other screens when user chooses an action to perform

package org.example.mapptracking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage

class HomePageActivity : AppCompatActivity() {

    private lateinit var button1: Button
    private lateinit var button2: Button

    @OptIn(ExperimentalGetImage::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        button1 = findViewById(R.id.SampleButton)
        button2 = findViewById(R.id.PrintButton)

        val accessToken = intent.getStringExtra("ACCESS_TOKEN")
        val username = intent.getStringExtra("USERNAME")
        val password = intent.getStringExtra("PASSWORD")
        val isPrinterConnected = intent.getStringExtra("IS_PRINTER_CONNECTED")

        // Set up button click listeners here
        button1.setOnClickListener {
            val intent = Intent(this, SampleActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
        }
        button2.setOnClickListener {
            val intent = Intent(this, PrintActivity::class.java)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
        }
    }
}