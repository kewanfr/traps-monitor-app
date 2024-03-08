package fr.kewan.trapsmonitor

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper

class BatteryLevelMonitor(private val mqttClientManager: MqttClientManager, private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val runnableCode = object : Runnable {
        override fun run() {
            val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }

            // is charging or not
            val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

            batteryStatus?.let { intent ->
                val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct: Float = level / scale.toFloat()


//                val message = MqttMessage()
//                message.payload = ("Battery level: ${(batteryPct * 100).toInt()}%").toByteArray()
                mqttClientManager.publishMessage("battery/${mqttClientManager.clientId}", "${(batteryPct * 100).toInt()}")
                mqttClientManager.publishMessage("charging/${mqttClientManager.clientId}", if (isCharging) "true" else "false")
            }

            handler.postDelayed(this, 60000*10) // Run this every 10 minutes
        }
    }

    fun startMonitoring() {
        handler.post(runnableCode)
    }

    fun stopMonitoring() {
        handler.removeCallbacks(runnableCode)
    }
}