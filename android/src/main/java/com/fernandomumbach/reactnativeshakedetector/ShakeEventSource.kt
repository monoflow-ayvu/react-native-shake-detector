package com.fernandomumbach.reactnativeshakedetector

import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeEventSource(
    reactContext: ReactApplicationContext,
    private val maxSamples: Long = 25,
    private val minTimeBetweenSamplesMs: Int = 20,
    private val visibleTimeRangeMs: Int = 500,
    private val magnitudeThreshold: Int = 25,
    private val percentOverThresholdForShake: Int = 66
) {
    private val TAG = javaClass.simpleName
    private val mReactContext: ReactApplicationContext = reactContext
    private val ctx = reactContext.applicationContext
    private var mLastTimestamp: Long = -1
    private var mCurrentIndex = 0
    private var mMagnitudes = DoubleArray(maxSamples.toInt())
    private var mTimestamps = LongArray(maxSamples.toInt())

    private val disposable: Disposable = RxSensor
        .sensorEvent(ctx, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST)
        .subscribeOn(Schedulers.computation())
        .filter(RxSensorFilter.minAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_HIGH))
        .onBackpressureBuffer(
            maxSamples,
            { Log.w(TAG, "dropped item!") },
            BackpressureOverflowStrategy.DROP_OLDEST
        )
        .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
        .filter {
            return@filter it.sensor.type == Sensor.TYPE_ACCELEROMETER
        }
        .observeOn(Schedulers.computation())
        .subscribe { sub -> process(sub) }

    fun stop() {
        return this.disposable.dispose()
    }

    private fun process(se: RxSensorEvent) {
        if (se.timestamp - mLastTimestamp < minTimeBetweenSamplesMs) {
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
        mCurrentIndex = ((mCurrentIndex + 1) % maxSamples).toInt()
    }

    private fun maybeDispatchShake(currentTimestamp: Long) {
        var numOverThreshold = 0
        var total = 0
        for (i in 0 until maxSamples) {
            val index = ((mCurrentIndex + 1) % maxSamples).toInt()
            if (currentTimestamp - mTimestamps[index] <= visibleTimeRangeMs) {
                total++
                if (mMagnitudes[index] >= magnitudeThreshold) {
                    numOverThreshold++
                }
            }
        }

        val percentOverThreshold = numOverThreshold.toDouble() / total
        if (total > 1 && percentOverThreshold > percentOverThresholdForShake / 100.0) {
            val ev = Arguments.createMap()
            ev.putDouble("percentOverThreshold", percentOverThreshold)

            if (mReactContext.hasActiveReactInstance()) {
                mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("shake", ev)
            }
        }
    }
}