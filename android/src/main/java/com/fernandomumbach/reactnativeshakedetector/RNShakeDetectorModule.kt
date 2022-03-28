package com.fernandomumbach.reactnativeshakedetector

import com.facebook.react.bridge.*

class RNShakeDetectorModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val TAG = "RNShakeDetectorModule"
    private var mReactContext: ReactApplicationContext = reactContext
    private var shakeEventSource: ShakeEventSource? = null

    override fun getName() = TAG

    @ReactMethod
    fun start(
        maxSamples: Int,
        minTimeBetweenSamplesMs: Int,
        visibleTimeRangeMs: Int,
        magnitudeThreshold: Int,
        percentOverThresholdForShake: Int,
        promise: Promise
    ) {
        this.shakeEventSource?.stop()
        this.shakeEventSource = null

        try {
            this.shakeEventSource = ShakeEventSource(
                mReactContext,
                maxSamples.toLong(),
                minTimeBetweenSamplesMs,
                visibleTimeRangeMs,
                magnitudeThreshold,
                percentOverThresholdForShake
            )
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }
    }

    @ReactMethod
    fun stop(promise: Promise) {
        try {
            this.shakeEventSource?.stop()
            this.shakeEventSource = null
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not stop ShakeService", e)
        }
    }

    override fun onHostResume() {
        return
    }

    override fun onHostPause() {
        return
    }

    override fun onHostDestroy() {
        this.shakeEventSource?.stop()
        this.shakeEventSource = null
    }
}
