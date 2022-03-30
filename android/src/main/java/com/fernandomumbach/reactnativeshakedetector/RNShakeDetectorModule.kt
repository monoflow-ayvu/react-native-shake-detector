package com.fernandomumbach.reactnativeshakedetector

import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import io.reactivex.BackpressureOverflowStrategy
import io.reactivex.disposables.Disposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RNShakeDetectorModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), LifecycleEventListener {
    private val TAG = "RNShakeDetectorModule"
    private val shakeEvents: AtomicReference<Disposable?> = AtomicReference(null)
    private var mReactContext: ReactApplicationContext = reactContext
    private var mApplicationContext: Context = reactContext.applicationContext
    private var classifier: AudioClassifierSource
    private var classifications = PublishProcessor.create<MutableMap<String, Float>>()
    private var classificationPipeline = classifications.onBackpressureBuffer(100, {
        Log.w(TAG, "classifications queue full!")
    }, BackpressureOverflowStrategy.DROP_OLDEST)
        .observeOn(Schedulers.computation())
        .buffer(3, TimeUnit.SECONDS)
        .map { items -> reduceClassificationsToMap(items) }
        .onBackpressureDrop()
        .share()

    override fun getName() = TAG

    init {
        classifier = AudioClassifierSource(mApplicationContext) {
            classifications.offer(it)
        }
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
        Log.w(TAG, "Starting shake event detector")
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
        useClassifier: Boolean = false,
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

            Log.w(TAG, "before classification pipeline")
            val currentValues = classificationPipeline.blockingLatest().first()
            Log.w(TAG, "after classification pipeline $currentValues")
            val values = Arguments.createMap()
            currentValues.forEach { (k, v) -> values.putDouble(k, v.toDouble()) }
            ev.putMap("classifications", values)

            if (mReactContext.hasActiveReactInstance()) {
                mReactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    .emit("shake", ev)
            }
        }
    }

    private fun reduceClassificationsToMap(items: Iterable<MutableMap<String, Float>>): MutableMap<String, Float> {
        val m = mutableMapOf<String, Float>()
        items.forEach {
            it.forEach { (k, v) ->
                if (!m.containsKey(k)) {
                    m[k] = v
                }

                if (v > m[k]!!) {
                    m[k] = v
                }
            }
        }

        return m
    }
}
