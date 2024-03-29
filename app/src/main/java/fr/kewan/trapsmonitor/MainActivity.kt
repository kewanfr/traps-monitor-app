package fr.kewan.trapsmonitor

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    // private val BROKER_URL = "tcp://192.168.0.21:1883";
    // private val CLIENT_ID = Build.MODEL;

    private lateinit var mqttClientManager: MqttClientManager


    private lateinit var serverHost: EditText
    private lateinit var serverPort: EditText
    private lateinit var deviceName: EditText
    private lateinit var toogleNotifSound: SwitchCompat
//    private lateinit var toogleSendStats: Switch
//    private lateinit var refreshInterval: EditText
    private lateinit var saveButton: Button
    private lateinit var connectButton: Button
    private lateinit var disconnectButton: Button
    private lateinit var testNotifButton: Button


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
        saveButton = findViewById(R.id.saveButton)
        connectButton = findViewById(R.id.connectButton)
        disconnectButton = findViewById(R.id.disconnectButton)
        testNotifButton = findViewById(R.id.testNotifButton)

        // Chargez les préférences
        val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE)

        // get datas from traps_config.txt in Downloads folder
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "traps_config.txt")
        if (file.exists()) {
            val lines = file.readLines()
            for (line in lines) {
                val (key, value) = line.split("=")
                when (key) {
                    "serverHost" -> serverHost.setText(value)
                    "serverPort" -> serverPort.setText(value)
                    "deviceName" -> deviceName.setText(value)
                    "toogleNotifSound" -> toogleNotifSound.isChecked = value.toBoolean()
                }
            }
        }

        if (sharedPref.contains("serverHost")) {
            serverHost.setText(sharedPref.getString("serverHost", "192.168.0.21"))
        }

        if (sharedPref.contains("serverPort")) {
            println("serverPort: ${sharedPref.getInt("serverPort", 1883 )}")
            serverPort.setText(sharedPref.getInt("serverPort", 1883).toString())
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

//        if (sharedPref.contains("refreshInterval")) {
//            refreshInterval.setText(sharedPref.getInt("refreshInterval", 60).toString())
//        }

        mqttClientManager = MqttClientManager(this, "", "")
        saveButton.setOnClickListener {
            savePreferences()
            Toast.makeText(this, "Préférences sauvegardées", Toast.LENGTH_SHORT).show()
        }

        connectButton.setOnClickListener {

            savePreferences()

            if (!mqttClientManager.isConnected()) {
                connectToServer()
            }else {
                Toast.makeText(this, "Déjà connecté", Toast.LENGTH_SHORT).show()
            }

        }

        disconnectButton.setOnClickListener {
            mqttClientManager.disconnect()
        }

        testNotifButton.setOnClickListener {

//            val textView: TextView = findViewById(R.id.title)
//            textView.setText(batteryStatus)

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


    fun connectToServer(){
        // On vérifie si on est déjà connecté et si mqttClientManager est déjà initialisé
        val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE)

        val host = sharedPref.getString("serverHost", "192.168.0.21")
        val port = sharedPref.getInt("serverPort", 1883)
        val name = sharedPref.getString("deviceName", Build.MODEL)

        if (host == null || host == "") {
            Toast.makeText(this, "Veuillez renseigner l'adresse du serveur", Toast.LENGTH_SHORT).show()
            return
        }

        val brokerUrl = "tcp://$host:$port"

        // On vérifie que les champs sont remplis
        mqttClientManager = MqttClientManager(this, brokerUrl, name ?: Build.MODEL)

        try {
            mqttClientManager.connect()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Impossible de se connecter au serveur", Toast.LENGTH_SHORT).show()
        }
    }

    fun savePreferences() {
        // Récupérer les valeurs et les sauvegarder dans les préférences
        val host = serverHost.text.toString()
        val port = serverPort.text.toString().toIntOrNull() ?: 80
        val name = deviceName.text.toString()
        val toogle_notif_sound = toogleNotifSound.isChecked
//            val toogle = toogleSendStats.isChecked
//            val interval = refreshInterval.text.toString().toIntOrNull() ?: 0

        val sharedPref = getSharedPreferences("MQTTConfig", MODE_PRIVATE)
        with (sharedPref.edit()) {
            putString("serverHost", host)
            putInt("serverPort", port)
            putString("deviceName", name)
            putBoolean("toogleNotifSound", toogle_notif_sound)
//                putBoolean("toogleSendStats", toogle)
//                putInt("refreshInterval", interval)
            apply()
        }
        // save file to storage that can be accessed by other apps
        // save in Downloads folder
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "traps_config.txt")
        file.writeText("serverHost=$host\nserverPort=$port\ndeviceName=$name\ntoogleNotifSound=$toogle_notif_sound\n")

    }

    override fun onDestroy() {
        mqttClientManager.publishMessage("devices/${mqttClientManager.clientId}", "Disconnected")
        mqttClientManager.disconnect()
        super.onDestroy()
    }


}