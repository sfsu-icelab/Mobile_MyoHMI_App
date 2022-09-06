package example.ASPIRE.MyoHMI_Android

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CNN(private val context: Context) {
    // TODO: Add a TF Lite interpreter as a field.
    private var interpreter: Interpreter? = null
    var isInitialized = false
        private set

    /** Executor to run inference task in the background. */
    private val executorService: ExecutorService = Executors.newCachedThreadPool()

    //private var inputImageWidth: Int = 0 // will be inferred from TF Lite model.
    //private var inputImageHeight: Int = 0 // will be inferred from TF Lite model.
    //private var modelInputSize: Int = 0 // will be inferred from TF Lite model.

    fun initialize(): Task<Void?> {
        val task = TaskCompletionSource<Void?>()
        executorService.execute {
            try {
                initializeInterpreter()
                task.setResult(null)
            } catch (e: IOException) {
                task.setException(e)
            }
        }
        return task.task
    }

    @Throws(IOException::class)
    private fun initializeInterpreter() {
        // TODO: Load the TF Lite model from file and initialize an interpreter.

        // Load the TF Lite model from asset folder and initialize TF Lite Interpreter with NNAPI enabled.
        val assetManager = context.assets
        val model = loadModelFile(assetManager, "semg.tflite")
        val interpreter = Interpreter(model)

        // TODO: Read the model input shape from model file.

        // Read input shape from model file.
        //val inputShape = interpreter.getInputTensor(0).shape()
        //inputImageWidth = inputShape[1]
        //inputImageHeight = inputShape[2]
        //modelInputSize = FLOAT_TYPE_SIZE * inputImageWidth * inputImageHeight * PIXEL_SIZE

        // Finish interpreter initialization.
        this.interpreter = interpreter

        isInitialized = true
        Log.d(TAG, "Initialized TFLite interpreter.")
    }

    @Throws(IOException::class)
    private fun loadModelFile(assetManager: AssetManager, filename: String): ByteBuffer {
        val fileDescriptor = assetManager.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun classify(featureBuffer: Array<Double>): Int {
        check(isInitialized) { "TF Lite Interpreter is not initialized yet." }

        // TODO: Add code to run inference with TF Lite.
        // Pre-processing: resize the input image to match the model input shape.

        //val byteBuffer = convertBitmapToByteBuffer(resizedImage)
        //val byteBuffer = arrayOf(arrayOf(floatArrayOf(2.8875F)),arrayOf(floatArrayOf(5.3375F)),arrayOf(floatArrayOf(17.5625F)),arrayOf(floatArrayOf(18.2F)),arrayOf(floatArrayOf(5.0625F)),arrayOf(floatArrayOf(4.8625F)),arrayOf(floatArrayOf(6.9125F)),arrayOf(floatArrayOf(2.725F)))
        val byteBuffer = arrayOf(arrayOf(floatArrayOf(featureBuffer[0].toFloat())),arrayOf(floatArrayOf(
            featureBuffer[1].toFloat()
        )),arrayOf(floatArrayOf(featureBuffer[2].toFloat())),arrayOf(floatArrayOf(featureBuffer[3].toFloat())),arrayOf(floatArrayOf(
            featureBuffer[4].toFloat()
        )),arrayOf(floatArrayOf(featureBuffer[5].toFloat())),arrayOf(floatArrayOf(featureBuffer[6].toFloat())),arrayOf(floatArrayOf(
            featureBuffer[7].toFloat()
        )))

        // Define an array to store the model output.
        val output = Array(1) { FloatArray(OUTPUT_CLASSES_COUNT) }

        // Run inference with the input data.
        interpreter?.run(byteBuffer, output)

        // Post-processing: find the digit that has the highest probability
        // and return it a human-readable string.
        val result = output[0]
        val maxIndex = result.indices.maxByOrNull { result[it] } ?: -1
        val resultString =
            "Prediction Result: %d\nConfidence: %2f"
                .format(maxIndex, result[maxIndex])
        Log.i("Gesture", resultString)
        return maxIndex
    }

    fun classifyAsync(featureBuffer: Array<Double>): Task<Int> {
        val task = TaskCompletionSource<Int>()
        executorService.execute {
            val result = classify(featureBuffer)
            task.setResult(result)
        }
        return task.task
    }

    fun close() {
        executorService.execute {
            interpreter?.close()
            Log.d(TAG, "Closed TFLite interpreter.")
        }
    }

    companion object {
        private const val TAG = "CNN"

        private const val FLOAT_TYPE_SIZE = 4
        private const val PIXEL_SIZE = 1

        private const val OUTPUT_CLASSES_COUNT = 8
    }
}