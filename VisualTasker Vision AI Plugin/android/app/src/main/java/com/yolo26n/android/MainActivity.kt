package com.yolo26n.android

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.yolo26n.android.databinding.ActivityMainBinding
import com.visualtasker.vision.yolo.YoloPredictor
import com.visualtasker.vision.yolo.YoloTask
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val loadExecutor = Executors.newSingleThreadExecutor()
    private val sessionLock = ReentrantLock()
    private val loadGeneration = AtomicInteger(0)
    private var predictor: YoloPredictor? = null
    private var cameraBound = false
    private var frameCount = 0
    private var fpsWindowStart = 0L
    private var selectedTask = YoloTask.DETECT

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) bindCamera() else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
        setupChips()
        loadTask(YoloTask.DETECT)
    }

    private fun setupChips() {
        YoloTask.entries.forEach { task ->
            val chip = Chip(this).apply {
                id = android.view.View.generateViewId()
                text = task.label
                isCheckable = true
                isChecked = task == YoloTask.DETECT
                isCheckedIconVisible = false
                setOnClickListener { if (selectedTask != task) loadTask(task) }
            }
            binding.taskChips.addView(chip)
        }
    }

    private fun setChipsEnabled(enabled: Boolean) {
        for (i in 0 until binding.taskChips.childCount) {
            binding.taskChips.getChildAt(i).isEnabled = enabled
        }
    }

    private fun loadTask(task: YoloTask) {
        selectedTask = task
        val generation = loadGeneration.incrementAndGet()
        setChipsEnabled(false)
        binding.overlayView.clear()
        binding.statusText.text = "${task.label} wird geladen…"
        loadExecutor.execute {
            sessionLock.withLock {
                if (generation != loadGeneration.get()) return@withLock
                val previous = predictor
                predictor = null
                previous?.close()
                if (generation != loadGeneration.get()) return@withLock
                try {
                    val loaded = YoloPredictor(this, task)
                    if (generation != loadGeneration.get()) {
                        loaded.close()
                        return@withLock
                    }
                    predictor = loaded
                    runOnUiThread {
                        if (generation != loadGeneration.get()) return@runOnUiThread
                        binding.statusText.text =
                            "${task.label} · ${loaded.accelerator} · ${loaded.session.inputWidth}×${loaded.session.inputHeight}"
                        setChipsEnabled(true)
                        ensureCameraPermission()
                    }
                } catch (error: Throwable) {
                    Log.e(TAG, "Modell ${task.asset} nicht geladen", error)
                    runOnUiThread {
                        if (generation != loadGeneration.get()) return@runOnUiThread
                        binding.statusText.text = "${task.label}: ${error.message}"
                        setChipsEnabled(true)
                    }
                }
            }
        }
    }

    private fun ensureCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            bindCamera()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun bindCamera() {
        if (cameraBound) return
        binding.previewView.post {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val rotation = binding.previewView.display.rotation
                val preview = Preview.Builder().setTargetRotation(rotation).build().also {
                    it.surfaceProvider = binding.previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setTargetRotation(rotation)
                    .build()
                analysis.setAnalyzer(analysisExecutor, ::analyze)
                val viewPort = binding.previewView.viewPort
                cameraProvider.unbindAll()
                if (viewPort != null) {
                    cameraProvider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        UseCaseGroup.Builder().setViewPort(viewPort).addUseCase(preview).addUseCase(analysis).build(),
                    )
                } else {
                    cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
                cameraBound = true
            }, ContextCompat.getMainExecutor(this))
        }
    }

    private fun analyze(image: ImageProxy) {
        if (!sessionLock.tryLock()) {
            image.close()
            return
        }
        try {
            val current = predictor
            if (current == null) return
            val bitmap = image.toOrientedBitmap()
            try {
                val start = System.nanoTime()
                val result = current.predict(bitmap)
                val ms = (System.nanoTime() - start) / 1_000_000f
                val now = System.currentTimeMillis()
                if (fpsWindowStart == 0L) fpsWindowStart = now
                frameCount++
                val elapsed = (now - fpsWindowStart).coerceAtLeast(1L)
                val fps = frameCount * 1000f / elapsed
                val task = current.task
                val accelerator = current.accelerator
                runOnUiThread {
                    if (task != selectedTask) return@runOnUiThread
                    binding.overlayView.setResult(result)
                    binding.statusText.text =
                        "${task.label} · $accelerator · ${"%.1f".format(fps)} FPS · ${"%.0f".format(ms)} ms · ${result.summary}"
                }
                if (elapsed >= 2000L) {
                    frameCount = 0
                    fpsWindowStart = now
                }
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (error: Throwable) {
            Log.e(TAG, "Inferenz fehlgeschlagen", error)
        } finally {
            sessionLock.unlock()
            image.close()
        }
    }

    private fun ImageProxy.toOrientedBitmap(): Bitmap {
        val raw = toBitmap()
        val degrees = imageInfo.rotationDegrees
        if (degrees == 0) return raw
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (rotated !== raw) raw.recycle()
        return rotated
    }

    override fun onDestroy() {
        super.onDestroy()
        loadGeneration.incrementAndGet()
        analysisExecutor.shutdown()
        loadExecutor.shutdown()
        sessionLock.withLock {
            predictor?.close()
            predictor = null
        }
    }

    companion object {
        private const val TAG = "Yolo26n"
    }
}
