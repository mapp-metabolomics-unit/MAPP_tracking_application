package org.example.dbgitracking

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

@Suppress("DEPRECATION", "PrivatePropertyName")
class PermissionsActivity : AppCompatActivity() {

    private val RequestEnableBt: Int = 1
    private val RequestEnableLocation: Int = 2

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        permissions()

    }

    private fun permissions() {
        // Ask bluetooth and location permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.CAMERA
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    @SuppressLint("InlinedApi")
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->

            val bluetoothCPermission = Manifest.permission.BLUETOOTH_CONNECT

            val permissionStatusBluetoothC = ActivityCompat.checkSelfPermission(this, bluetoothCPermission)

            val bluetoothSPermission = Manifest.permission.BLUETOOTH_SCAN

            val permissionStatusBluetoothS = ActivityCompat.checkSelfPermission(this, bluetoothSPermission)

            val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION

            val permissionStatusLocation = ActivityCompat.checkSelfPermission(this, locationPermission)

            if (permissionStatusLocation == PackageManager.PERMISSION_GRANTED
                && permissionStatusBluetoothC == PackageManager.PERMISSION_GRANTED
                && permissionStatusBluetoothS == PackageManager.PERMISSION_GRANTED) {
                activation()
            } else {
                // Not all permissions are granted, redirect to Activity B
                showToast("You need to accept all permissions to connect a printer")
                val intent = Intent(this, PrinterConnectActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                activation()
            } else {
                showToast("You need to accept all permissions to connect a printer")
                val intent = Intent(this, PrinterConnectActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

    private fun activation() {
        val bluetoothSniffer = BluetoothAdapter.getDefaultAdapter()
        val isBluetoothEnabled = bluetoothSniffer?.isEnabled ?: false

        val locationSniffer = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isLocationEnabled = locationSniffer.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (!isBluetoothEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            startActivityForResult(enableBtIntent, RequestEnableBt)
        }

        if (!isLocationEnabled) {
            val locationIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(locationIntent, RequestEnableLocation)
        }

        if (isBluetoothEnabled && isLocationEnabled) {
            // Both Bluetooth and Location are enabled
            val accessToken = intent.getStringExtra("ACCESS_TOKEN")
            val username = intent.getStringExtra("USERNAME")
            val password = intent.getStringExtra("PASSWORD")

            val intent = Intent(this, PrinterConnectActivity::class.java)
            intent.putExtra("ACCESS_TOKEN", accessToken)
            intent.putExtra("USERNAME", username)
            intent.putExtra("PASSWORD", password)
            startActivity(intent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestEnableBt && resultCode == RESULT_OK) {
            // Bluetooth was enabled
            activation() // Check Location again
        } else if (requestCode == RequestEnableLocation) {
            // Location settings changed, check both again
            activation()
        }
    }

    private fun showToast(toast: String?) {
        runOnUiThread { Toast.makeText(this, toast, Toast.LENGTH_LONG).show() }
    }

}