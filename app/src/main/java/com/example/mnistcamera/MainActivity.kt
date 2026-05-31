package com.example.mnistcamera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.RectF
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

        val guideRect = binding.bboxOverlay.getGuideRect()
        if (guideRect == null) {
            runOnUiThread {
                Toast.makeText(this, "Guide box is not ready yet.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        cameraExecutor.execute {
            try {
                val roi = cropToGuide(frame, guideRect)
                val inkRatio = ImageUtils.estimateInkRatioInGuideRoi(roi)
                if (inkRatio < 0.02f) {
                    runOnUiThread {
                        binding.resultText.text = "Prediction: no digit detected"
                        binding.confidenceText.text = "Confidence: 0.0%"
                    }
                    return@execute
                }

                val preprocessed = ImageUtils.preprocessGuideRoiTo28(roi)
                val result = localClassifier.classifyInput(preprocessed)
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

    private fun cropToGuide(frame: Bitmap, guideRectInView: RectF): Bitmap {
        val overlayWidth = binding.bboxOverlay.width.coerceAtLeast(1)
        val overlayHeight = binding.bboxOverlay.height.coerceAtLeast(1)

        val sx = frame.width.toFloat() / overlayWidth.toFloat()
        val sy = frame.height.toFloat() / overlayHeight.toFloat()

        val left = (guideRectInView.left * sx).toInt().coerceIn(0, frame.width - 1)
        val top = (guideRectInView.top * sy).toInt().coerceIn(0, frame.height - 1)
        val right = (guideRectInView.right * sx).toInt().coerceIn(left + 1, frame.width)
        val bottom = (guideRectInView.bottom * sy).toInt().coerceIn(top + 1, frame.height)

        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(frame, left, top, width, height)
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
