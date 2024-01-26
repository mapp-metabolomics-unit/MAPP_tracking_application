// 1)
// Connection and first page of the application. Permits to extract the NocoDB token to perform further actions
// and to be sure that user is connected to the database before adding some information's on the database.
// After connection performed and verified, user is redirected to home page to select the action he wants to perform.

// References the folder where the code is stored
package org.example.mapptracking

// Necessary imports for this class
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


// Define the class
class ConnectionActivity : AppCompatActivity() {

    // Define the names of the different displayed elements
    private lateinit var editTextUsername: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var connectionStatusTextView: TextView


    // Function that is executed when the page is reached by the user (no need to call it)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make the link to the correct xml file where the displayed elements are defined and shaped
        setContentView(R.layout.activity_connection)

        // Make the connection to the displayed elements
        editTextUsername = findViewById(R.id.usernameEditText)
        editTextPassword = findViewById(R.id.passwordEditText)
        buttonLogin = findViewById(R.id.loginButton)
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView)

        // Action executed by a click on the login button
        buttonLogin.setOnClickListener {
            // Stores username and password in variables to use them
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()

            // Change the status text to confirm to user that the connection process is in progress
            connectionStatusTextView.text = "Connecting..."

            // Start a coroutine to perform the network operation
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val baseUrl = "http://134.21.20.118:8080" // NocoDB base url
                    val loginUrl = "$baseUrl/api/v1/auth/user/signin" // NocoDB signin complement url
                    val url = URL(loginUrl) //declare the url as an url for kotlin
                    // Make a post request to send the credentials to NocoDB
                    val connection =
                        withContext(Dispatchers.IO) {
                            url.openConnection()
                        } as HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true

                    val requestBody = "{\"email\":\"$username\",\"password\":\"$password\"}"

                    val outputStream: OutputStream = connection.outputStream
                    withContext(Dispatchers.IO) {
                        outputStream.write(requestBody.toByteArray())
                    }
                    withContext(Dispatchers.IO) {
                        outputStream.close()
                    }

                    //Retrieve the server response to know if the connection was successful or not
                    val responseCode = connection.responseCode

                    // If connection is a success, store the token as a variable to use it to make multiple operation after
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                        val content = StringBuilder()
                        var inputLine: String?
                        while (withContext(Dispatchers.IO) {
                                `in`.readLine()
                            }.also { inputLine = it } != null) {
                            content.append(inputLine)
                        }
                        withContext(Dispatchers.IO) {
                            `in`.close()
                        }

                        // Convert the json output token to a string
                        val jsonData = content.toString()
                        val jsonResponse = JSONObject(jsonData)
                        val accessToken = jsonResponse.getString("token")

                        // Launch permission activity and pass the necessary variables to it
                        val intent = Intent(this@ConnectionActivity, PermissionsActivity::class.java)
                        intent.putExtra("USERNAME", username)
                        intent.putExtra("PASSWORD", password)
                        intent.putExtra("ACCESS_TOKEN", accessToken)
                        startActivity(intent)

                        // Close the connection to the NocoDB server
                        finish()

                        // Handle invalid credentials or access from outside unifr intranet
                    } else {
                        withContext(Dispatchers.Main) {
                            connectionStatusTextView.text = "Error connecting. Please check your credentials and/or verify your connection"
                        }
                    }
                    // Handle exceptions
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        connectionStatusTextView.text = "Error connecting. Please check your credentials."

                    }
                }
            }
        }
    }
}
