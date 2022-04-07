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
        const val MINIMUM_DISPLAY_THRESHOLD = 0.3F
    }

    private val tag = javaClass.simpleName
    private var classifier: AudioClassifier =
        AudioClassifier.createFromFile(ctx, MODEL_FILE)

    // each audio tensor is approx 1 second
    private var audioTensors: Array<TensorAudio> = Array(5) {
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
        record!!.positionNotificationPeriod = audioTensors[0].tensorBuffer.flatSize
        record!!.startRecording()
    }

    fun stopRecording() {
        Log.i(tag, "Stopping AudioClassifier recording")

        if (!hasPermissions()) {
            throw Error("Audio classifier needs RECORD_AUDIO permission")
        }

        record?.stop()
        record?.release()
    }

    fun classify(): MutableMap<String, Float> {
        Log.i(tag, "classifying...")
        val idx = audioTensorIdx
        val tensors =
            audioTensors.slice(
                IntRange(
                    idx,
                    audioTensors.size - 1
                )
            ) + audioTensors.slice(IntRange(0, idx))

        val m = mutableMapOf<String, Float>()
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
            return
        }

        if (record == null) {
            return
        }

        val tensor = audioTensors[audioTensorIdx]
        tensor.load(record)
        Log.i(
            tag,
            "onPeriodicNotification, tensor [${audioTensorIdx}] buffer size = ${tensor.tensorBuffer.flatSize}, record size (frames) = ${record.bufferSizeInFrames}, record sample rate = ${record.sampleRate}"
        )

        audioTensorIdx = (audioTensorIdx + 1) % audioTensors.size
    }
}