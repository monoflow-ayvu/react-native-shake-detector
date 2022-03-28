package com.fernandomumbach.reactnativeshakedetector

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import java.util.concurrent.TimeUnit
import android.util.Log
import com.gvillani.rxsensors.RxSensor
import com.gvillani.rxsensors.RxSensorEvent
import com.gvillani.rxsensors.RxSensorFilter
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt

class ShakeEvent(val magnitude: Double)

class ShakeEventSource(
    ctx: Context,
    private val maxSamples: Long = 25,
    private val minTimeBetweenSamplesMs: Int = 20,
    private val visibleTimeRangeMs: Int = 500,
    private val magnitudeThreshold: Int = 25,
    private val percentOverThresholdForShake: Int = 66
) {
    private val TAG = javaClass.simpleName
    private var mLastTimestamp: Long = -1
    private var mCurrentIndex = 0
    private var mMagnitudes = DoubleArray(maxSamples.toInt())
    private var mTimestamps = LongArray(maxSamples.toInt())
    private val flowable: Flowable<ShakeEvent> = RxSensor
        .sensorEvent(ctx, Sensor.TYPE_ACCELEROMETER, SensorManager.SENSOR_DELAY_FASTEST)
        .subscribeOn(Schedulers.computation())
        .filter(RxSensorFilter.minAccuracy(SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM))
        .onBackpressureBuffer(
            maxSamples,
            { Log.w(TAG, "dropped item!") },
            BackpressureOverflowStrategy.DROP_OLDEST
        )
        .distinctUntilChanged(RxSensorFilter.uniqueEventValues())
        .filter {
            return@filter it.sensor.type == Sensor.TYPE_ACCELEROMETER
        }
        .flatMap { return@flatMap process(it) }
        // max one event per second
        .debounce(1, TimeUnit.SECONDS)
        .observeOn(Schedulers.computation())
        .share()


    private fun process(se: RxSensorEvent): Flowable<ShakeEvent> {
        if (se.timestamp - mLastTimestamp < minTimeBetweenSamplesMs) {
            return Flowable.empty()
        }

        val ax: Float = se.values[0]
        val ay: Float = se.values[1]
        val az: Float = se.values[2]
        val evTimeMillis = se.timestamp / 1_000_000L

        mLastTimestamp = evTimeMillis
        mTimestamps[mCurrentIndex] = evTimeMillis
        mMagnitudes[mCurrentIndex] = sqrt(ax.pow(2) + ay.pow(2) + az.pow(2)).toDouble()

        val evt = maybeDispatchShake(evTimeMillis)
        mCurrentIndex = ((mCurrentIndex + 1) % maxSamples).toInt()

        if (evt == null) {
            return Flowable.empty()
        } else {
            return Flowable.just(evt)
        }
    }

    private fun maybeDispatchShake(currentTimestamp: Long): ShakeEvent? {
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
            return ShakeEvent(percentOverThreshold)
        }

        return null
    }

    fun stream(): Flowable<ShakeEvent> {
        return flowable
    }
}