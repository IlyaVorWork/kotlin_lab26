package com.ilyavorontsov.lab26

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.util.concurrent.ListenableFuture
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var pvPreview: PreviewView

    private lateinit var ibCapture: ImageButton

    private lateinit var viewColorBox: View
    private lateinit var tvColor: TextView

    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
            preview.surfaceProvider = pvPreview.getSurfaceProvider()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val imageCapture = ImageCapture.Builder()
                .setTargetRotation(pvPreview.display.rotation)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                val buffer = imageProxy.planes[0].buffer
                buffer.rewind()
                val x = 320
                val y = 240
                val offset = (y * imageProxy.width + x) * 4
                val r = buffer[offset + 0].toInt() and 0xff
                val g = buffer[offset + 1].toInt() and 0xff
                val b = buffer[offset + 2].toInt() and 0xff

                viewColorBox.setBackgroundColor(Color.argb(255, r, g, b))
                tvColor.text = "#${Integer.toHexString(r)}${Integer.toHexString(g)}${Integer.toHexString(b)}"

                imageProxy.close()
            }

            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageCapture,
                imageAnalysis,
                preview
            )

            ibCapture = findViewById(R.id.ibCapture)
            ibCapture.setOnClickListener {
                val storageDir = getExternalFilesDir(Environment.DIRECTORY_DCIM)
                val f = File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}",
                    ".jpg",
                    storageDir
                )
                Log.d("Camera", "File will be saved to: ${f.absolutePath}")
                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(f).build()

                imageCapture.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(this),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onError(error: ImageCaptureException) {
                            Log.d("NEW PHOTO", error.message!!)
                        }

                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d("NEW PHOTO", "success")
                        }
                    })
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted)
            initCamera()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pvPreview = findViewById(R.id.pvPreview)
        tvColor = findViewById(R.id.tvColor)
        viewColorBox = findViewById(R.id.viewColorBox)

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            initCamera()
        }
    }
}