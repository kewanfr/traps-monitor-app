package fr.kewan.trapsmonitor

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dcastalia.localappupdate.DownloadApk
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.json.JSONObject


class MqttClientManager(private val context: Context, serverUri: String, val clientId: String) {

    var batteryLevelMonitor: BatteryLevelMonitor = BatteryLevelMonitor(this, context)
    private var mqttClient: MqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)


    private val WifiUpdateHandler = Handler(Looper.getMainLooper())
    private val runnableCodeWifiUpdate = object : Runnable {
        override fun run() {
            wifiInfo()
            WifiUpdateHandler.postDelayed(this, 60*1000*10) // 10 minutes en millisecondes
        }
    }
    init {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                // Gérer la perte de connexion ici
                batteryLevelMonitor.stopMonitoring()

                WifiUpdateHandler.removeCallbacks(runnableCodeWifiUpdate)
                Toast.makeText(context, "Connexion au serveur perdue", Toast.LENGTH_SHORT).show()
                connect()

            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {

                if (topic == null) {
                    return
                }

                // topic: notifs/DeviceName
                topic.let {
                    when {
                        it.startsWith("notifs/") -> {
                            val deviceName = topic.substring(7)
                            if (deviceName.lowercase() == clientId.lowercase()) {
                                Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()

                                showNotification("$message", "$message")
                            }
                        }

                        it.startsWith("toasts/") -> {
                            val deviceName = topic.substring(7)
                            if (deviceName.lowercase() == clientId.lowercase()) {
                                Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()
                            }
                        }

                        // cmds, json format
                        it.startsWith("cmd/") -> {
                            val deviceName = topic.substring(4)
                            if (deviceName.lowercase() == clientId.lowercase()) {

                                val jsonContent = message.toString()
                                val json = JSONObject(jsonContent)
                                val cmd = json.getString("cmd")

                                when (cmd) {
                                    "batteryLevel" -> batteryLevelMonitor.publishBatteryLevel()
                                    "batteryChargingStatus" -> batteryLevelMonitor.publishBatteryChargingStatus()
                                    "batteryStatus" -> {
                                        batteryLevelMonitor.publishBatteryLevel()
                                        batteryLevelMonitor.publishBatteryChargingStatus()
                                    }

                                    "wifiInfo" -> wifiInfo()
                                    "ping" -> {
                                        val startTime = System.currentTimeMillis() // Enregistrez le moment où le ping est reçu
                                        publishMessage("ping/${clientId}", "Ping received")
                                        val endTime = System.currentTimeMillis() // Enregistrez le moment où la réponse est envoyée
                                        val elapsedTime = endTime - startTime // Calculez le temps écoulé
                                        publishMessage("ping/${clientId}", "$elapsedTime")
                                    }
                                    "updateApp" -> {
                                        val apkUrl = json.getString("url") // L'URL du fichier APK
                                        updateApp(apkUrl)
                                    }

                                    "getVersion" -> {
                                        try {
                                            val pInfo = context.packageManager.getPackageInfo(
                                                context.packageName,
                                                0
                                            )
                                            val version = pInfo.versionName
                                            val versionCode = pInfo.versionCode

                                            val jsonObj = JSONObject()
                                            jsonObj.put("versionCode", versionCode)
                                            jsonObj.put("versionName", version)
                                            publishMessage("version/${clientId}", jsonObj.toString())
                                        } catch (e: PackageManager.NameNotFoundException) {
                                            e.printStackTrace()
                                        }
                                    }
                                }

                            }
                        }
                    }
                }

                /*Toast.makeText(context, "Nouveau message MQTT: $topic - $message", Toast.LENGTH_SHORT).show()

                message?.let {
                    showNotification("Nouveau message MQTT", String(it.payload))
                }*/
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Message livré
            }
        })
    }

    fun isConnected(): Boolean {
        return mqttClient.isConnected
    }

    fun disconnect() {
        try {
            publishMessage("devices/$clientId", "Disconnected")
            mqttClient.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    fun connect() {
        try {
            val options = MqttConnectOptions()
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Connecté. Ici, souscrivez au topic souhaité.
                    subscribeToTopic("notifs/#")
                    subscribeToTopic("toasts/#")
                    subscribeToTopic("cmd/#")
                    val clientId = mqttClient.clientId
                    publishMessage("devices/$clientId", "Connected")


                    Toast.makeText(context, "Connecté au serveur", Toast.LENGTH_SHORT).show()
                    batteryLevelMonitor.startMonitoring()

                    wifiInfo()
                    WifiUpdateHandler.post(runnableCodeWifiUpdate)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    // Échec de la connexion

                    Toast.makeText(context, "Échec de la connexion au serveur", Toast.LENGTH_SHORT).show()
                    batteryLevelMonitor.stopMonitoring()
                    WifiUpdateHandler.removeCallbacks(runnableCodeWifiUpdate)
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun wifiInfo(){
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager


        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Demandez la permission si elle n'est pas déjà accordée
            ActivityCompat.requestPermissions(context as AppCompatActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        val wifiInfo = wifiManager.connectionInfo
        println("wifiInfo: $wifiInfo")

        if (wifiInfo != null) {
            val bssid = wifiInfo.bssid
            val rssi = wifiInfo.rssi
            val ssid = wifiInfo.ssid
            val ip = wifiInfo.ipAddress
            val ipString = String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
            val mac = wifiInfo.macAddress
            val linkSpeed = wifiInfo.linkSpeed
            val signalStrength = WifiManager.calculateSignalLevel(rssi, 100)


            val jsonObj = JSONObject()
            jsonObj.put("rssi", rssi)
            jsonObj.put("ssid", ssid)
            jsonObj.put("ip", ipString)
            jsonObj.put("mac", mac)
            jsonObj.put("linkSpeed", linkSpeed)
            jsonObj.put("signalStrength", signalStrength)
            jsonObj.put("bssid", bssid)

            publishMessage("wifi/${clientId}", jsonObj.toString())
        } else {
            publishMessage("wifi/${clientId}", "couldn't get wifi info")
        }

    }
    fun subscribeToTopic(topic: String) {
        try {
            mqttClient.subscribe(topic, 1)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun publishMessage(topic: String, message: String, qos: Int = 1) {
        try {
            mqttClient.publish(topic, MqttMessage().apply {
                payload = message.toByteArray()
                this.qos = qos
            })
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }


    fun updateApp(apkUrl: String){
        val url = "https://raw.githubusercontent.com/kewanfr/traps-monitor-app/main/app-debug.apk"
        println("updateApp: $apkUrl")
        println("updateApp: $url")
        val downloadApk = DownloadApk(context)

        // With standard fileName 'App Update.apk'
        downloadApk.startDownloadingApk(apkUrl)
        println("start downloading apk")
    }
    fun updateApp2(apkUrl: String) {
        println("updateApp: $apkUrl")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val packageManager = context.packageManager
            if (!packageManager.canRequestPackageInstalls()) {
                // Demandez la permission d'installer des packages inconnus
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                intent.data = Uri.parse("package:" + context.packageName)
                context.startActivity(intent)
                return
            }
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl))
        request.setMimeType("application/vnd.android.package-archive")
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "update.apk")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (columnIndex != -1 && DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            val uriColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            if (uriColumnIndex != -1) {
                                val uriString = cursor.getString(uriColumnIndex)
                                println("Download complete. File Uri: $uriString") // Log when the download is complete
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.setDataAndType(Uri.parse(uriString), "application/vnd.android.package-archive")
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                println("Starting APK installation.") // Log when the APK installation starts
                                context.startActivity(intent)
                            }
                        }
                    }
                    cursor.close()
                }
            }
        }
       /* val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    val query = DownloadManager.Query()
                    query.setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                            val uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI))
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.parse(uriString), "application/vnd.android.package-archive")
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                            context.startActivity(intent)
                        }
                    }
                    cursor.close()
                }
            }
        }*/

        context.registerReceiver(broadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun showNotification(title: String, content: String){
        val notificationId = 1
        val channelId = "mqtt_messages"

        val sharedPref = context.getSharedPreferences("MQTTConfig", AppCompatActivity.MODE_PRIVATE)

        val toogleNotifSound = sharedPref.getBoolean("toogleNotifSound", true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Messages TRAPS"
            val descriptionText = "Notifications de messages TRAPS"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))

        if (toogleNotifSound) builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            notify(notificationId, builder.build())
        }
    }
}
