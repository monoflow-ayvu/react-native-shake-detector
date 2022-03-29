package com.fernandomumbach.reactnativeshakedetector

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.reactivex.disposables.Disposable
import java.util.concurrent.atomic.AtomicReference

class RNShakeDetectorModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val TAG = "RNShakeDetectorModule"
    private var mReactContext: ReactApplicationContext = reactContext
    private var mApplicationContext: Context = reactContext.applicationContext
    private var shakeEvents: AtomicReference<Disposable?> = AtomicReference(null)
    private var classifier: AudioClassifierSource = AudioClassifierSource(mApplicationContext) {
        it.forEach { (k, v) -> Log.d(TAG, "--> $k = ${v * 100}%") }
        Log.d(TAG, "=======")
    }

    override fun getName() = TAG

    @ReactMethod
    fun start(
        maxSamples: Int,
        minTimeBetweenSamplesMs: Int,
        visibleTimeRangeMs: Int,
        magnitudeThreshold: Int,
        percentOverThresholdForShake: Int,
        useAudioClassifier: Boolean = true,
        promise: Promise
    ) {
        Log.w(TAG, "Starting shake event detector")
        try {
            internalStart(
                maxSamples,
                minTimeBetweenSamplesMs,
                visibleTimeRangeMs,
                magnitudeThreshold,
                percentOverThresholdForShake,
            )
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }
    }

    @ReactMethod
    fun stop(promise: Promise) {
        Log.w(TAG, "Stopping shake event detector")
        try {
            internalStop()
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
        internalStop()
    }

    private fun internalStart(
        maxSamples: Int,
        minTimeBetweenSamplesMs: Int,
        visibleTimeRangeMs: Int,
        magnitudeThreshold: Int,
        percentOverThresholdForShake: Int,
        useClassifier: Boolean = true
    ) {
        internalStop()

        if (useClassifier) {
            Log.w(TAG, "initializeAudioClassifier")
            classifier.start()
        }

        Log.w(TAG, "Starting new ShakeEventSource")
        val source = ShakeEventSource(
            mApplicationContext,
            maxSamples.toLong(),
            minTimeBetweenSamplesMs,
            visibleTimeRangeMs,
            magnitudeThreshold,
            percentOverThresholdForShake,
        )
        shakeEvents.set(source.stream().subscribe({
            Log.w(TAG, "detected shake event! " + it.magnitude.toString())
            onShakeEvent(it)
        }, {
            Log.e(TAG, it.toString())
        }))
    }

    private fun internalStop() {
        classifier.stop()
        shakeEvents.get().let {
            if (it?.isDisposed == false) {
                it.dispose()
            }
            shakeEvents.set(null)
        }
    }

    private fun onShakeEvent(sensorEvent: ShakeEvent) {
        Log.w(TAG, "onShakeEvent " + sensorEvent.magnitude.toString())
        mReactContext.let {
            val ev = Arguments.createMap()
            ev.putDouble("percentOverThreshold", sensorEvent.magnitude)
            if (mReactContext.hasActiveReactInstance()) {
                mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("shake", ev)
            }
        }
    }
}
