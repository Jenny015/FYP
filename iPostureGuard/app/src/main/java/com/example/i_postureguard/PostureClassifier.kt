package com.example.i_postureguard

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PostureClassifier(private val context: Context) {
    private lateinit var interpreter: Interpreter
    private val MODEL_PATH = "posture_rf_model.tflite"
    private val POSTURE_CLASSES = listOf(
        "Text Neck", "Left Lean", "Right Lean",
        "Sleep on Back", "Sleep on Side", "Unsafety Distance"
    )

    init {
        setupInterpreter()
    }

    private fun setupInterpreter() {
        val modelBuffer = loadModelFile()
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(MODEL_PATH)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun classify(input: FloatArray): Pair<String, Float> {
        // Assuming input is a float array of size 6 (e.g., [2, 1, 3, 4, 1, 2])
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 6), org.tensorflow.lite.DataType.FLOAT32)
        val byteBuffer = ByteBuffer.allocateDirect(6 * 4).order(ByteOrder.nativeOrder())
        input.forEach { byteBuffer.putFloat(it) }
        inputBuffer.loadBuffer(byteBuffer)

        // Prepare output buffer (assuming model outputs probabilities for 6 classes)
        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 6), org.tensorflow.lite.DataType.FLOAT32)
        interpreter.run(inputBuffer.buffer, outputBuffer.buffer)

        // Process output to get probabilities
        val tensorProcessor = TensorProcessor.Builder()
            .add(NormalizeOp(0f, 1f)) // Adjust if your model outputs need normalization
            .build()
        val probabilities = tensorProcessor.process(outputBuffer).floatArray

        // Find the class with the highest probability
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val score = probabilities[maxIndex] * 100 // Convert to percentage
        val posture = POSTURE_CLASSES[maxIndex]

        return Pair(posture, score)
    }

    fun close() {
        interpreter.close()
    }
}