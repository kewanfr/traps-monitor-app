package fr.kewan.testtraps

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private val BROKER_URL = "tcp://192.168.0.21:1883";
    private val CLIENT_ID = "android-client";

    private lateinit var mqttClientManager: MqttClientManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Toast.makeText(this, "Connecting to broker: $BROKER_URL, client: $CLIENT_ID", Toast.LENGTH_SHORT).show();

        /*try {

            mqttHandler.connect(BROKER_URL, CLIENT_ID)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to connect to broker: $BROKER_URL", Toast.LENGTH_SHORT).show();
        }

        publishMessage("/test/tt", "Hello, world!");

        subscribeToTopic("/test/tt")*/

        // On message

        mqttClientManager = MqttClientManager(this, BROKER_URL, CLIENT_ID)
        mqttClientManager.connect()


    }

    override fun onDestroy() {
        super.onDestroy();
    }

    private fun publishMessage(topic: String, message: String) {
        Toast.makeText(this, "Publishing message: $message", Toast.LENGTH_SHORT).show();

        //mqttHandler.publish(topic, message);


    }

    private fun subscribeToTopic(topic: String) {
        Toast.makeText(this, "Subscribing to topic: $topic", Toast.LENGTH_SHORT).show();

        //mqttHandler.subscribe(topic)

    }
}