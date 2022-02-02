package com.fernandomumbach.reactnativeshakedetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class RNShakeDetectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val LOG_TAG = "RNShakeDetectorModule"
    private var mReactContext: ReactApplicationContext = reactContext
    private var mApplicationContext: Context = reactContext.applicationContext

    override fun getName() = LOG_TAG

    override fun initialize() {
        val filter = IntentFilter(ShakeServiceConstants.ACTION_SHAKE)
        mReactContext.registerReceiver(mReceiver, filter)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == ShakeServiceConstants.ACTION_SHAKE) {
                val percentOverThreshold =
                    intent.getDoubleExtra(ShakeServiceConstants.EXTRA_PERCENT_OVER_THRESHOLD, -1.0)
                val ev = Arguments.createMap()
                ev.putDouble("percentOverThreshold", percentOverThreshold)

                if (reactContext.hasActiveReactInstance()) {
                    reactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("shake", ev)
                }
            }
        }
    }

    @ReactMethod
    fun start(
        maxSamples: Int,
        MinTimeBetweenSamplesMs: Int,
        VisibleTimeRangeMs: Int,
        MagnitudeThreshold: Int,
        PercentOverThresholdForShake: Int,
        promise: Promise
    ) {
        try {
            val intent = Intent(mApplicationContext, ShakeService::class.java)
            intent.putExtra(ShakeServiceConstants.EXTRA_CONFIG_MAX_SAMPLES, maxSamples)
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_MIN_TIME_BETWEEN_SAMPLES_MS,
                MinTimeBetweenSamplesMs
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_VISIBLE_TIME_RANGE_MS,
                VisibleTimeRangeMs
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_MAGNITUDE_THRESHOLD,
                MagnitudeThreshold
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_PERCENT_OVER_THRESHOLD,
                PercentOverThresholdForShake
            )
            mApplicationContext.startService(intent)
            return promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }
    }

    @ReactMethod
    fun stop(promise: Promise) {
        try {
            internalStop()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not stop ShakeService", e)
        }
    }

    private fun internalStop() {
        val intent = Intent(mApplicationContext, ShakeService::class.java)
        mApplicationContext.stopService(intent)
    }

    override fun onHostResume() {
        return
    }

    override fun onHostPause() {
        return
    }

    override fun onHostDestroy() {
        mReactContext.unregisterReceiver(mReceiver)
        internalStop()
    }
}
