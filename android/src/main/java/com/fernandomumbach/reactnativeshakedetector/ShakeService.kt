package com.fernandomumbach.reactnativeshakedetector

import android.app.Service
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt


object ShakeServiceConstants {
    const val ACTION_SHAKE = "BROADCAST_SHAKE_ACTION"
    const val EXTRA_PERCENT_OVER_THRESHOLD = "PERCENT_OVER_THRESHOLD"
    const val EXTRA_CONFIG_MAX_SAMPLES = "EXTRA_CONFIG_MAX_SAMPLES"
    const val EXTRA_CONFIG_MIN_TIME_BETWEEN_SAMPLES_MS = "EXTRA_CONFIG_MIN_TIME_BETWEEN_SAMPLES_MS"
    const val EXTRA_CONFIG_VISIBLE_TIME_RANGE_MS = "EXTRA_CONFIG_VISIBLE_TIME_RANGE_MS"
    const val EXTRA_CONFIG_MAGNITUDE_THRESHOLD = "EXTRA_CONFIG_MAGNITUDE_THRESHOLD"
    const val EXTRA_CONFIG_PERCENT_OVER_THRESHOLD_FOR_SHAKE = "EXTRA_CONFIG_PERCENT_OVER_THRESHOLD_FOR_SHAKE"
}

class ShakeService : Service(), SensorEventListener {
    private var mSensorManager: SensorManager? = null
    private val LOG_TAG = "ShakeService"

    // https://www.tabnine.com/web/assistant/code/rs/5c7c5489ac38dc0001e28e44#L102
    private var MAX_SAMPLES = 25
    private var MIN_TIME_BETWEEN_SAMPLES_MS = 20
    private var VISIBLE_TIME_RANGE_MS = 500
    private var MAGNITUDE_THRESHOLD = 25
    private var PERCENT_OVER_THRESHOLD_FOR_SHAKE = 66

    private var mLastTimestamp: Long = -1
    private var mCurrentIndex = 0
    private var mMagnitudes = DoubleArray(0)
    private var mTimestamps = LongArray(0)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mSensorManager!!.unregisterListener(this)

        intent?.let {
            Log.d(LOG_TAG, "Setting service config via Intent")
            MAX_SAMPLES = intent.getIntExtra(ShakeServiceConstants.EXTRA_CONFIG_MAX_SAMPLES, 25)
            Log.d(LOG_TAG, "MAX_SAMPLES: $MAX_SAMPLES")
            MIN_TIME_BETWEEN_SAMPLES_MS = intent.getIntExtra(ShakeServiceConstants.EXTRA_CONFIG_MIN_TIME_BETWEEN_SAMPLES_MS, 20)
            Log.d(LOG_TAG, "MIN_TIME_BETWEEN_SAMPLES_MS: $MIN_TIME_BETWEEN_SAMPLES_MS")
            VISIBLE_TIME_RANGE_MS = intent.getIntExtra(ShakeServiceConstants.EXTRA_CONFIG_VISIBLE_TIME_RANGE_MS, 500)
            Log.d(LOG_TAG, "VISIBLE_TIME_RANGE_MS: $VISIBLE_TIME_RANGE_MS")
            MAGNITUDE_THRESHOLD = intent.getIntExtra(ShakeServiceConstants.EXTRA_CONFIG_MAGNITUDE_THRESHOLD, 25)
            Log.d(LOG_TAG, "MAGNITUDE_THRESHOLD: $MAGNITUDE_THRESHOLD")
            PERCENT_OVER_THRESHOLD_FOR_SHAKE = intent.getIntExtra(ShakeServiceConstants.EXTRA_CONFIG_PERCENT_OVER_THRESHOLD_FOR_SHAKE, 66)
            Log.d(LOG_TAG, "PERCENT_OVER_THRESHOLD_FOR_SHAKE: $PERCENT_OVER_THRESHOLD_FOR_SHAKE")
        }

        mLastTimestamp = -1
        mCurrentIndex = 0
        mMagnitudes = DoubleArray(MAX_SAMPLES)
        mTimestamps = LongArray(MAX_SAMPLES)

        mSensorManager!!.registerListener(
            this,
            mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        return START_STICKY
    }

    override fun onCreate() {
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
    }

    override fun onDestroy() {
        mSensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // we do not care about accuracy changes
        return
    }


    override fun onSensorChanged(se: SensorEvent?) {
        if (se?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            if (se.timestamp - mLastTimestamp < MIN_TIME_BETWEEN_SAMPLES_MS) {
                return
            }

            val ax: Float = se.values[0]
            val ay: Float = se.values[1]
            val az: Float = se.values[2]
            val evTimeMillis = se.timestamp / 1_000_000L

            mLastTimestamp = evTimeMillis
            mTimestamps[mCurrentIndex] = evTimeMillis
            mMagnitudes[mCurrentIndex] = sqrt(ax.pow(2) + ay.pow(2) + az.pow(2)).toDouble()

            maybeDispatchShake(evTimeMillis)

            mCurrentIndex = (mCurrentIndex + 1) % MAX_SAMPLES
        }
    }

    private fun maybeDispatchShake(currentTimestamp: Long) {
        var numOverThreshold = 0
        var total = 0
        for (i in 0 until MAX_SAMPLES) {
            val index = (mCurrentIndex - i + MAX_SAMPLES) % MAX_SAMPLES
            if (currentTimestamp - mTimestamps[index] <= VISIBLE_TIME_RANGE_MS) {
                total++
                if (mMagnitudes[index] >= MAGNITUDE_THRESHOLD) {
                    numOverThreshold++
                }
            }
        }

        val percentOverThreshold = numOverThreshold.toDouble() / total
        if (total > 1 && percentOverThreshold > PERCENT_OVER_THRESHOLD_FOR_SHAKE / 100.0) {
            val senderIntent = Intent()
            senderIntent.action = ShakeServiceConstants.ACTION_SHAKE
            senderIntent.putExtra(ShakeServiceConstants.EXTRA_PERCENT_OVER_THRESHOLD, percentOverThreshold)
            sendBroadcast(senderIntent)
        }
    }
}