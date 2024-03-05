package fr.kewan.testtraps

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage

class MqttClientManager(private val context: Context, serverUri: String, clientId: String) {
    private var mqttClient: MqttAndroidClient = MqttAndroidClient(context, serverUri, clientId)

    init {
        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                // Gérer la perte de connexion ici
            }

            override fun messageArrived(topic: String?, message: MqttMessage?) {
                message?.let {
                    showNotification("Nouveau message MQTT", String(it.payload))
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                // Message livré
            }
        })
    }

    fun connect() {
        try {
            val options = MqttConnectOptions()
            mqttClient.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    // Connecté. Ici, souscrivez au topic souhaité.
                    subscribeToTopic("notifs/")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    // Échec de la connexion
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

    private fun showNotification(title: String, content: String) {
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
