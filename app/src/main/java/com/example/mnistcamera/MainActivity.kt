package com.example.mnistcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mnistcamera.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var classifier: DigitClassifier? = null
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            classifier = DigitClassifier(this)
        } catch (e: Exception) {
            Toast.makeText(this, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()

        binding.captureButton.setOnClickListener {
            captureAndClassify()
        }

        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndClassify() {
        val capture = imageCapture ?: return
        val localClassifier = classifier
        if (localClassifier == null) {
            runOnUiThread {
                Toast.makeText(this, "Classifier is not ready.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val photoFile = File.createTempFile("mnist_capture_", ".jpg", cacheDir)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Capture failed: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                    if (bitmap == null) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Image decode failed.", Toast.LENGTH_SHORT)
                                .show()
                        }
                        return
                    }

                    val result = localClassifier.classify(bitmap)
                    photoFile.delete()

                    runOnUiThread {
                        binding.resultText.text = "Prediction: ${result.digit}"
                        binding.confidenceText.text = String.format(
                            Locale.US,
                            "Confidence: %.1f%%",
                            result.confidence * 100f
                        )
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier?.close()
        cameraExecutor.shutdown()
    }
}
