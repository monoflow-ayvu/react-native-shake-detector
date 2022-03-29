package com.fernandomumbach.reactnativeshakedetector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.os.HandlerCompat
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class AudioClassifierSource(ctx: Context) {
    private val ctx = ctx
    private val TAG = javaClass.simpleName

    companion object {
        const val MODEL_FILE = "yamnet.tflite"
        const val MINIMUM_DISPLAY_THRESHOLD = 0.3F
    }

    private var audioClassifier: AudioClassifier? = null
    private var audioRecord: AudioRecord? = null
    private var classificationInterval = 30L // how often should classification run in milli-secs
    private var handler: Handler // background thread handler to run classification

    init {
        // Create a handler to run classification in a background thread
        val handlerThread = HandlerThread("backgroundThread")
        handlerThread.start()
        handler = HandlerCompat.createAsync(handlerThread.looper)

        if (hasPermissions()) {
            startAudioClassification()
        } else {
            throw Error("Audio classifier needs RECORD_AUDIO permission")
        }
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

    private fun startAudioClassification() {
        // If the audio classifier is initialized and running, do nothing.
        if (audioClassifier != null) return;

        // Initialize the audio classifier
        val classifier = AudioClassifier.createFromFile(ctx, MODEL_FILE)
        val audioTensor = classifier.createInputTensorAudio()

        // Initialize the audio recorder
        val record = classifier.createAudioRecord()
        record.startRecording()

        // Define the classification runnable
        val run = object : Runnable {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun run() {
                val startTime = System.currentTimeMillis()

                // Load the latest audio sample
                audioTensor.load(record)
                val output = classifier.classify(audioTensor)

                // Filter out results above a certain threshold, and sort them descendingly
                val filteredModelOutput = output[0].categories.filter {
                    it.score > MINIMUM_DISPLAY_THRESHOLD
                }.sortedBy {
                    -it.score
                }

                val finishTime = System.currentTimeMillis()

                Log.d(TAG, "Latency = ${finishTime - startTime}ms")

                filteredModelOutput.forEach {
                    Log.d(TAG, "${it.displayName} = ${it.score * 100}%")
                }

//                // Updating the UI
//                runOnUiThread {
//                    probabilitiesAdapter.categoryList = filteredModelOutput
//                    probabilitiesAdapter.notifyDataSetChanged()
//                }
//
                // Rerun the classification after a certain interval
                handler.postDelayed(this, classificationInterval)
            }
        }

        // Start the classification process
        handler.post(run)

        // Save the instances we just created for use later
        audioClassifier = classifier
        audioRecord = record
    }
}