package com.nickskelton.wifidelity.view

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.extensions.addBottomMargin
import com.nickskelton.wifidelity.utils.PerformanceLogger
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber
import java.io.IOException

class CameraActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            val bottomInset = windowInsets.systemWindowInsetBottom
            cameraButton.addBottomMargin(bottomInset)
            galleryButton.addBottomMargin(bottomInset)
            windowInsets.consumeSystemWindowInsets()
        }
//        cameraButton.setOnClickListener { takePicture() }
        galleryButton.setOnClickListener { openGallery() }
        rxPermissions = RxPermissions(this)

        disposable = rxPermissions.request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) { // Always true pre-M
//                    initCamera()
                } else {
                    Timber.d("Permission denied. Bail.")
                    finish()
                }
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RESULT_LOAD_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                TODO("Processing bitmap not supported just yet")
//                processImage(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error retrieving photo", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }

    companion object {
        private const val RESULT_LOAD_IMAGE = 8734
        private const val PREF_CAPTURE_TIME = "photoCaptureMillis"
    }
}
