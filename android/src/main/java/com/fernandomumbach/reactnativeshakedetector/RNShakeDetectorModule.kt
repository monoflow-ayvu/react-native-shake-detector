package com.fernandomumbach.reactnativeshakedetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class RNShakeDetectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
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
                    intent.getDoubleExtra(ShakeServiceConstants.EXTRA_PERCENT_OVER_THRESHOLD, 0.0)
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
        MAX_SAMPLES: Int,
        MIN_TIME_BETWEEN_SAMPLES_MS: Int,
        VISIBLE_TIME_RANGE_MS: Int,
        MAGNITUDE_THRESHOLD: Int,
        PERCENT_OVER_THRESHOLD_FOR_SHAKE: Int,
        promise: Promise
    ) {
        try {
            val intent = Intent(mApplicationContext, ShakeService::class.java)
            intent.putExtra(ShakeServiceConstants.EXTRA_CONFIG_MAX_SAMPLES, MAX_SAMPLES)
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_MIN_TIME_BETWEEN_SAMPLES_MS,
                MIN_TIME_BETWEEN_SAMPLES_MS
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_VISIBLE_TIME_RANGE_MS,
                VISIBLE_TIME_RANGE_MS
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_CONFIG_MAGNITUDE_THRESHOLD,
                MAGNITUDE_THRESHOLD
            )
            intent.putExtra(
                ShakeServiceConstants.EXTRA_PERCENT_OVER_THRESHOLD,
                PERCENT_OVER_THRESHOLD_FOR_SHAKE
            )
            mApplicationContext.startService(intent)
            return promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }
    }
}
