package fr.kewan.trapsmonitor

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage


class MqttClientManager(private val context: Context, serverUri: String, clientId: String) {

    var clientId: String = clientId
    var serverUri: String = serverUri
    private var mqttClient: MqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)

    init {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                // Gérer la perte de connexion ici
                Toast.makeText(context, "Connexion au serveur perdue", Toast.LENGTH_SHORT).show()
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {

                if (topic == null) {
                    return
                }

                // topic: notifs/DeviceName
                if (topic.startsWith("notifs/")) {
                    val deviceName = topic.substring(7)
                    if (deviceName.lowercase() == clientId.lowercase()) {
                        Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()

                        showNotification("Nouveau message", "$message")
                    }
                }

                if (topic.startsWith("toasts/")) {
                    val deviceName = topic.substring(7)
                    if (deviceName.lowercase() == clientId.lowercase()) {
                        Toast.makeText(context, "$message", Toast.LENGTH_LONG).show()
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
                    val clientId = mqttClient.clientId
                    publishMessage("devices/$clientId", "Connected")


                    Toast.makeText(context, "Connecté au serveur", Toast.LENGTH_SHORT).show()
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    // Échec de la connexion

                    Toast.makeText(context, "Échec de la connexion au serveur", Toast.LENGTH_SHORT).show()

                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun subscribeToTopic(topic: String) {
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

    private fun showNotification(title: String, content: String){
        val notificationId = 1
        val channelId = "mqtt_messages"

        val sharedPref = context.getSharedPreferences("MQTTConfig", AppCompatActivity.MODE_PRIVATE);

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
            .setStyle(NotificationCompat.BigTextStyle().bigText(content));

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

    private fun showNotification2(title: String, content: String) {
        val channelId = "mqtt_messages"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Messages MQTT"
            val descriptionText = "Notifications de messages MQTT"
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

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
