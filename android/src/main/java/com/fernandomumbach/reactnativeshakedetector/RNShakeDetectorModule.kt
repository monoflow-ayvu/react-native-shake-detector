package com.fernandomumbach.reactnativeshakedetector

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.fernandomumbach.reactnativeshakedetector.ShakeServiceConstants.ACTION_SHAKE

class RNShakeDetectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    private val LOG_TAG = "RNShakeDetectorModule"
    private var mReactContext: ReactApplicationContext
    private var mApplicationContext: Context

    override fun getName() = LOG_TAG

    init {
        mReactContext = reactContext
        mApplicationContext = reactContext.applicationContext
    }

    override fun initialize() {
        val filter = IntentFilter(ShakeServiceConstants.ACTION_SHAKE)
        mReactContext!!.registerReceiver(mReceiver, filter)
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action != null && action == ShakeServiceConstants.ACTION_SHAKE) {
                val magnitude =
                    intent.getFloatExtra(ShakeServiceConstants.EXTRA_SHAKE_MAGNITUDE, 0.0F)
                Log.d(LOG_TAG, "onShakeEvent $magnitude")
            }
        }
    }

    @ReactMethod
    fun start(promise: Promise) {
        try {
            val intent = Intent(mApplicationContext, ShakeService::class.java)
            mApplicationContext.startService(intent)
            return promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }
    }
}
