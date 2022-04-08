package com.fernandomumbach.reactnativeshakedetector

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier

class AudioClassifier(
    private val ctx: Context
) : AudioRecord.OnRecordPositionUpdateListener {
    companion object {
        const val MODEL_FILE = "yamnet.tflite"
        const val MINIMUM_DISPLAY_THRESHOLD = 0.1F
        const val SECONDS = 4
        const val SECONDS_FACTOR = 10
    }

    private val tag = javaClass.simpleName
    private var classifier: AudioClassifier =
        AudioClassifier.createFromFile(ctx, MODEL_FILE)

    // each audio tensor is approx 1 second
    private var audioTensors: Array<TensorAudio> = Array(SECONDS * SECONDS_FACTOR) {
        classifier.createInputTensorAudio()
    }
    private var audioTensorIdx: Int = 0
    private var record: AudioRecord? = null

    fun startRecording() {
        Log.i(tag, "Starting AudioClassifier recording")

        if (!hasPermissions()) {
            throw Error("Audio classifier needs RECORD_AUDIO permission")
        }

        record = classifier.createAudioRecord()
        record!!.setRecordPositionUpdateListener(this)
        // sample rate = 1 second
        record!!.positionNotificationPeriod = record!!.sampleRate / SECONDS_FACTOR
        record!!.startRecording()
    }

    fun stopRecording() {
        Log.i(tag, "Stopping AudioClassifier recording")

        if (!hasPermissions()) {
            throw Error("Audio classifier needs RECORD_AUDIO permission")
        }

        try {
            record?.stop()
            record?.release()
        } catch (e: Exception) {
            Log.e(tag, "could not release audio record, might be already released")
        }
    }

    fun classify(): MutableMap<String, Float> {
        Log.i(tag, "classifying...")
        val m = mutableMapOf<String, Float>()
        val idx = audioTensorIdx
        val tensors =
            audioTensors.slice(
                IntRange(
                    idx,
                    audioTensors.size - 1
                )
            ) + audioTensors.slice(IntRange(0, idx))

        tensors.forEach tensorLoop@{ element ->
            val output = classifier.classify(element)
            output[0].categories.forEach categoryLoop@{
                if (it.score < MINIMUM_DISPLAY_THRESHOLD) {
                    return@categoryLoop
                }

                if (!m.containsKey(it.label)) {
                    m[it.label] = it.score
                }

                if (it.score > m[it.label]!!) {
                    m[it.label] = it.score
                }
            }
        }

        return m
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

    override fun onMarkerReached(p0: AudioRecord?) {
        Log.i(tag, "onMarkerReached")
    }

    override fun onPeriodicNotification(record: AudioRecord?) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            Log.e(tag, "SDK version too low to run classifier")
            return
        }

        if (record == null) {
            Log.e(tag, "No record given. Cannot add it to buffer.")
            return
        }

        try {
            val tensor = audioTensors[audioTensorIdx]
            tensor.load(record)
            Log.i(
                tag,
                "onPeriodicNotification, tensor [${audioTensorIdx}] buffer size = ${tensor.tensorBuffer.flatSize}, record size (frames) = ${record.bufferSizeInFrames}, record sample rate = ${record.sampleRate}"
            )

            audioTensorIdx = (audioTensorIdx + 1) % audioTensors.size
        } catch (e: Exception) {
            Log.e(tag, "could not write audio buffer")
        }
    }
}