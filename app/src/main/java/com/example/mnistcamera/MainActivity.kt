package com.example.mnistcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mnistcamera.databinding.ActivityMainBinding
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var classifier: DigitClassifier? = null
    private lateinit var cameraExecutor: ExecutorService

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

        initializeClassifier()
        cameraExecutor = Executors.newSingleThreadExecutor()
        applyBottomInsetPadding()

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

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun captureAndClassify() {
        if (classifier == null) {
            initializeClassifier()
        }

        val localClassifier = classifier
        if (localClassifier == null) {
            runOnUiThread {
                Toast.makeText(this, "Classifier is not ready.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val frame = binding.previewView.bitmap
        if (frame == null) {
            runOnUiThread {
                Toast.makeText(this, "Camera frame is not ready yet.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        cameraExecutor.execute {
            try {
                val result = localClassifier.classify(frame)
                runOnUiThread {
                    binding.resultText.text = "Prediction: ${result.digit}"
                    binding.confidenceText.text = String.format(
                        Locale.US,
                        "Confidence: %.1f%%",
                        result.confidence * 100f
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Classification failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun initializeClassifier() {
        try {
            classifier?.close()
            classifier = DigitClassifier(this)
            binding.resultText.text = getString(R.string.result_default)
            binding.confidenceText.text = getString(R.string.confidence_default)
        } catch (e: Exception) {
            classifier = null
            binding.resultText.text = "Prediction: model init failed"
            binding.confidenceText.text = e.message ?: e.javaClass.simpleName
            Toast.makeText(this, "Model load failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun applyBottomInsetPadding() {
        val panel = binding.bottomPanel
        val baseBottomPadding = panel.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(panel) { view, insets ->
            val systemBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                baseBottomPadding + systemBottom
            )
            insets
        }

        ViewCompat.requestApplyInsets(panel)
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier?.close()
        cameraExecutor.shutdown()
    }
}
