package com.fernandomumbach.reactnativeshakedetector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.lang.Exception
import java.util.concurrent.atomic.AtomicBoolean

class AudioClassifierSource(
    private val ctx: Context,
    private val cb: (MutableMap<String, Float>) -> Unit
) {
    private val TAG = javaClass.simpleName
    private val shouldRun = AtomicBoolean(false)

    companion object {
        const val MODEL_FILE = "yamnet.tflite"
        const val MINIMUM_DISPLAY_THRESHOLD = 0.3F
        const val MAX_SAMPLES = 100L
    }

    private var classificationInterval = 10L // how often should classification run in milli-secs
    private var handler: Handler // background thread handler to run classification
    private var classifier: AudioClassifier
    private var audioTensor: TensorAudio
    private var record: AudioRecord? = null

    init {
        // Create a handler to run classification in a background thread
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)

        classifier = AudioClassifier.createFromFile(ctx, MODEL_FILE)
        audioTensor = classifier.createInputTensorAudio()
    }

    private fun hasPermissions(): Boolean {
        if (ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }

        return false
    }

    fun start() {
        if (!hasPermissions()) {
            throw Error("Audio classifier needs RECORD_AUDIO permission")
        }

        if (record == null) {
            // Initialize the audio recorder
            record = classifier.createAudioRecord()
        }

        record!!.startRecording()
        shouldRun.set(true)

        // Define the classification runnable
        val run = object : Runnable {
            override fun run() {
                shouldRun.run {
                    val run = this.get()

                    if (!run) {
                        // no classifier, retry on next loop
                        return@run run
                    }

                    // Load the latest audio sample
                    audioTensor.load(record)
                    val output = classifier.classify(audioTensor)

                    // Filter out results above a certain threshold, and sort them descendingly
                    val filteredModelOutput = output[0].categories.filter {
                        it.score > MINIMUM_DISPLAY_THRESHOLD
                    }.sortedBy {
                        -it.score
                    }

                    val m = mutableMapOf<String, Float>()
                    filteredModelOutput.forEach {
                        m[it.label] = it.score
                    }

                    try {
                        cb(m)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error running callback", e)
                    }

                    return@run run
                }

                // Rerun the classification after a certain interval
                handler.postDelayed(this, classificationInterval)
            }
        }

        // Start the classification process
        handler.post(run)
    }

    fun stop() {
//        handler.removeCallbacksAndMessages(null)
        shouldRun.set(false)
        try {
            record?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio record", e)
        }
    }
}