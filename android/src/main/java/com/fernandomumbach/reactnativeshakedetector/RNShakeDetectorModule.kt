package com.fernandomumbach.reactnativeshakedetector

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import java.util.*
import java.util.concurrent.atomic.AtomicReference

class RNShakeDetectorModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val tag = "RNShakeDetectorModule"
    private val shakeEvents: AtomicReference<Disposable?> = AtomicReference(null)
    private var mReactContext: ReactApplicationContext = reactContext
    private var mApplicationContext: Context = reactContext.applicationContext
    private var classifierV2: AudioClassifier = AudioClassifier(mApplicationContext)
    private val mHandlerThread = HandlerThread("RecognitionHandlerThread")
    private val mHandler: Handler

    override fun getName() = tag

    init {
        mHandlerThread.start()
        mHandler = Handler(mHandlerThread.looper)
    }

    @ReactMethod
    fun start(
        maxSamples: Int,
        minTimeBetweenSamplesMs: Int,
        visibleTimeRangeMs: Int,
        magnitudeThreshold: Int,
        percentOverThresholdForShake: Int,
        useAudioClassifier: Boolean = false,
        promise: Promise
    ) {
        Log.w(tag, "Starting shake event detector")
        try {
            internalStart(
                maxSamples,
                minTimeBetweenSamplesMs,
                visibleTimeRangeMs,
                magnitudeThreshold,
                percentOverThresholdForShake,
                useAudioClassifier,
            )
        } catch (e: Exception) {
            promise.reject("Could not start ShakeService", e)
        }

        promise.resolve(null)
    }

    @ReactMethod
    fun stop(promise: Promise) {
        classifierV2.stopRecording()

        Log.w(tag, "Stopping shake event detector")
        try {
            internalStop()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("Could not stop ShakeService", e)
        }
    }

    @ReactMethod
    fun classify(promise: Promise) {
        val currentClassification = classifierV2.classify()
        val values = Arguments.createMap()
        currentClassification.forEach { (k, v) -> values.putDouble(k, v.toDouble()) }
        promise.resolve(values)
    }

    override fun onHostResume() {
        return
    }

    override fun onHostPause() {
        return
    }

    override fun onHostDestroy() {
        internalStop()
        mHandlerThread.quit()
    }

    private fun internalStart(
        maxSamples: Int,
        minTimeBetweenSamplesMs: Int,
        visibleTimeRangeMs: Int,
        magnitudeThreshold: Int,
        percentOverThresholdForShake: Int,
        useClassifier: Boolean = false,
    ) {
        internalStop()

        if (useClassifier) {
            Log.w(tag, "initializeAudioClassifier")
            classifierV2.startRecording()
        }

        Log.w(tag, "Starting new ShakeEventSource")
        val source = ShakeEventSource(
            mApplicationContext,
            maxSamples.toLong(),
            minTimeBetweenSamplesMs,
            visibleTimeRangeMs,
            magnitudeThreshold,
            percentOverThresholdForShake,
        )
        shakeEvents.set(source.stream().subscribe({
            Log.w(tag, "detected shake event! " + it.magnitude.toString())
            onShakeEvent(it)
        }, {
            Log.e(tag, it.toString())
        }))
    }

    private fun internalStop() {
        classifierV2.stopRecording()
        shakeEvents.get().let {
            if (it?.isDisposed == false) {
                it.dispose()
            }
            shakeEvents.set(null)
        }
    }

    private fun onShakeEvent(sensorEvent: ShakeEvent) {
        Log.w(tag, "onShakeEvent " + sensorEvent.magnitude.toString())
        val eventWhen = Calendar.getInstance().timeInMillis
        mHandler.postDelayed(
            {
                val ev = Arguments.createMap()
                ev.putDouble("percentOverThreshold", sensorEvent.magnitude)
                ev.putDouble("when", eventWhen.toDouble())

                val currentClassification = classifierV2.classify()
                val values = Arguments.createMap()
                currentClassification.forEach { (k, v) -> values.putDouble(k, v.toDouble()) }
                ev.putMap("classifications", values)

                if (mReactContext.hasActiveReactInstance()) {
                    mReactContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("shake", ev)
                }
            },
            2500 // wait for 2.5 seconds to collect more audio
        )
    }
}
