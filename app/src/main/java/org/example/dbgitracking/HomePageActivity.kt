// Homepage screen that redirects to other screens when user chooses an action to perform

package org.example.dbgitracking

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ExperimentalGetImage

class HomePageActivity : AppCompatActivity() {

    private lateinit var button1: Button
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var button4: Button
    private lateinit var button5: Button
    private lateinit var button6: Button
    private lateinit var button7: Button
    private lateinit var button8: Button
    private lateinit var button9: Button

    @OptIn(ExperimentalGetImage::class) override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        button1 = findViewById(R.id.SampleButton)
        button2 = findViewById(R.id.WeightingButton)
        button3 = findViewById(R.id.ExtractionButton)
        button4 = findViewById(R.id.AliquotsButton)
        button5 = findViewById(R.id.InjectionButton)
        button6 = findViewById(R.id.SignalingButton)
        button7 = findViewById(R.id.FindButton)
        button8 = findViewById(R.id.MoveButton)
        button9 = findViewById(R.id.LocationButton)

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
            startActivity(intent)
        }
        button2.setOnClickListener {
            val intent = Intent(this, WeightingActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
        }

        button3.setOnClickListener {
            val intent = Intent(this, ExtractionActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
        }

        button4.setOnClickListener {
            val intent = Intent(this, AliquotsActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            intent.putExtra("IS_PRINTER_CONNECTED", isPrinterConnected)
            startActivity(intent)
        }

        //button5.setOnClickListener {
            //val intent = Intent(this, InjectionActivity::class.java)
            //intent.putExtra("ACCESS_TOKEN", accessToken)
            //intent.putExtra("USERNAME", username)
            //intent.putExtra("PASSWORD", password)
            //startActivity(intent)
        //}

        //button6.setOnClickListener {
            //val intent = Intent(this, SignalingActivity::class.java)
            //intent.putExtra("ACCESS_TOKEN", accessToken)
            //intent.putExtra("USERNAME", username)
            //intent.putExtra("PASSWORD", password)
            //startActivity(intent)
        //}

        //button7.setOnClickListener {
            //val intent = Intent(this, FindActivity::class.java)
            //intent.putExtra("ACCESS_TOKEN", accessToken)
            //intent.putExtra("USERNAME", username)
            //intent.putExtra("PASSWORD", password)
            //startActivity(intent)
        //}

        //button8.setOnClickListener {
            //val intent = Intent(this, MoveActivity::class.java)
            //intent.putExtra("ACCESS_TOKEN", accessToken)
            //intent.putExtra("USERNAME", username)
            //intent.putExtra("PASSWORD", password)
            //startActivity(intent)
        //}

        //button9.setOnClickListener {
            //val intent = Intent(this, LocationActivity::class.java)
            //intent.putExtra("ACCESS_TOKEN", accessToken)
            //intent.putExtra("USERNAME", username)
            //intent.putExtra("PASSWORD", password)
            //startActivity(intent)
        //}
    }
}