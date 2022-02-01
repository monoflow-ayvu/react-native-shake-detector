package com.fernandomumbach.reactnativeshakedetector

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.widget.Toast

object ShakeServiceConstants {
    const val ACTION_SHAKE = "BROADCAST_SHAKE_ACTION"
    const val EXTRA_SHAKE_MAGNITUDE = "EXTRA_SHAKE_MAGNITUDE"
}

class ShakeService : Service(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private val LOG_TAG = "ShakeService"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show()
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        mSensorManager!!.registerListener(
            this,
            mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onDestroy() {
        mSensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }


    override fun onSensorChanged(se: SensorEvent?) {
        if (se?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = se.values[0]
            val y = se.values[0]
            val z = se.values[0]

            val senderIntent = Intent()
            senderIntent.action = ShakeServiceConstants.ACTION_SHAKE
            senderIntent.putExtra("x", x)
            senderIntent.putExtra("y", y)
            senderIntent.putExtra("z", z)
            senderIntent.putExtra(ShakeServiceConstants.EXTRA_SHAKE_MAGNITUDE, x*y*z)
            sendBroadcast(senderIntent)
        }
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // we do not care about accuracy changes
        return
    }
}