package fr.kewan.testtraps

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // private val BROKER_URL = "tcp://192.168.0.21:1883";
    // private val CLIENT_ID = Build.MODEL;

    private lateinit var mqttClientManager: MqttClientManager;

    private lateinit var serverHost: EditText
    private lateinit var serverPort: EditText
    private lateinit var deviceName: EditText
    private lateinit var toogleNotifSound: Switch
//    private lateinit var toogleSendStats: Switch
    private lateinit var refreshInterval: EditText
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var testNotifButton: Button

    fun connectToServer(){
        // On vérifie si on est déjà connecté et si mqttClientManager est déjà initialisé
        /*if (mqttClientManager.isConnected()) {
            mqttClientManager.disconnect()
        }*/

        val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE);

        val host = sharedPref.getString("serverHost", "")
        val port = sharedPref.getInt("serverPort", 80)
        val name = sharedPref.getString("deviceName", Build.MODEL)

        if (host == null || host == "") {
            Toast.makeText(this, "Veuillez renseigner l'adresse du serveur", Toast.LENGTH_SHORT).show();
            return
        }

        val brokerUrl = "tcp://$host:$port";

        // On vérifie que les champs sont remplis
        mqttClientManager = MqttClientManager(this, brokerUrl, name ?: Build.MODEL);

        try {
            mqttClientManager.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Impossible de se connecter au serveur", Toast.LENGTH_SHORT).show();
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        serverHost = findViewById(R.id.serverHost)
        serverPort = findViewById(R.id.serverPort)
        deviceName = findViewById(R.id.deviceName)
        toogleNotifSound = findViewById(R.id.toogleNotifSound)
//        toogleSendStats = findViewById(R.id.toogleSendStats)
//        refreshInterval = findViewById(R.id.refreshInterval)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        testNotifButton = findViewById(R.id.testNotifButton)

        // Chargez les préférences
        val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE)

        if (sharedPref.contains("serverHost")) {
            serverHost.setText(sharedPref.getString("serverHost", ""))
        }

        if (sharedPref.contains("serverPort")) {
            println("serverPort: ${sharedPref.getInt("serverPort", 80)}")
            serverPort.setText(sharedPref.getInt("serverPort", 80).toString())
        }

        if (sharedPref.contains("deviceName"))  {
            deviceName.setText(sharedPref.getString("deviceName", Build.MODEL))
        }

        if (sharedPref.contains("toogleNotifSound")) {
            toogleNotifSound.isChecked = sharedPref.getBoolean("toogleNotifSound", true)
        }

//        if (sharedPref.contains("toogleSendStats")) {
//            toogleSendStats.isChecked = sharedPref.getBoolean("toogleSendStats", false)
//        }

        if (sharedPref.contains("refreshInterval")) {
            refreshInterval.setText(sharedPref.getInt("refreshInterval", 60).toString())
        }

        mqttClientManager = MqttClientManager(this, "", "")

        connectButton.setOnClickListener {
            // Récupérer les valeurs et démarer la connexion MQTT
            val host = serverHost.text.toString()
            val port = serverPort.text.toString().toIntOrNull() ?: 80
            val name = deviceName.text.toString()
            val toogle_notif_sound = toogleNotifSound.isChecked
//            val toogle = toogleSendStats.isChecked
            val interval = refreshInterval.text.toString().toIntOrNull() ?: 0

            val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE)
            with (sharedPref.edit()) {
                putString("serverHost", host)
                putInt("serverPort", port)
                putString("deviceName", name)
                putBoolean("toogleNotifSound", toogle_notif_sound)
//                putBoolean("toogleSendStats", toogle)
                putInt("refreshInterval", interval)
                apply()
            }

            if (mqttClientManager.isConnected()) {
                mqttClientManager.disconnect()
            }

            connectToServer()
        }

        disconnectButton.setOnClickListener {
            mqttClientManager.disconnect()
        }

        testNotifButton.setOnClickListener {
            mqttClientManager.publishMessage("notifs/${mqttClientManager.clientId}", "Test de notification")
        }


        // Toast.makeText(this, "Connecting to broker: $BROKER_URL, client: $CLIENT_ID", Toast.LENGTH_SHORT).show();

        /*try {

            mqttHandler.connect(BROKER_URL, CLIENT_ID)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to connect to broker: $BROKER_URL", Toast.LENGTH_SHORT).show();
        }

        publishMessage("/test/tt", "Hello, world!");

        subscribeToTopic("/test/tt")*/

        // On message
        // mqttClientManager = MqttClientManager(this, BROKER_URL, CLIENT_ID)
        // mqttClientManager.connect()

        // Toast.makeText(this, "Device name: $CLIENT_ID", Toast.LENGTH_SHORT).show();


        connectToServer()

    }

    override fun onDestroy() {
        mqttClientManager.publishMessage("devices/${mqttClientManager.clientId}", "Disconnected");
        mqttClientManager.disconnect()
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