package com.nickskelton.wifidelity.view

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.model.BitmapRepository
import com.tbruyelle.rxpermissions2.RxPermissions
import io.fotoapparat.Fotoapparat
import io.fotoapparat.log.fileLogger
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.selector.back
import io.reactivex.disposables.Disposable
import org.koin.android.ext.android.inject
import timber.log.Timber

class CameraActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var cameraView: io.fotoapparat.view.CameraView
    private lateinit var rxPermissions: RxPermissions
    private lateinit var thumbView: ImageView

    private var fotoapparat: Fotoapparat? = null

    val bitmapRepository: BitmapRepository by inject()

    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraView = findViewById(R.id.camera_view)
        textView = findViewById(R.id.text)
        button = findViewById(R.id.button)
        thumbView = findViewById(R.id.thumbView)
        button.setOnClickListener { takePicture() }
        rxPermissions = RxPermissions(this)
        disposable = rxPermissions.request(Manifest.permission.CAMERA)
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    initCamera()
                } else {
                    Timber.d("Permission denied. Bail.")
                    finish()
                }
            }
    }

    private fun initCamera() {
        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,                   // view which will draw the camera preview
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            lensPosition = back(),               // (optional) we want back camera
//                cameraConfiguration = configuration, // (optional) define an advanced configuration
            logger = loggers(// (optional) we want to log camera events in 2 places at once
                logcat(),                   // ... in logcat
                fileLogger(this)            // ... and to file
            ),
            cameraErrorCallback = { error -> Timber.e(error) } // (optional) log fatal errors
        )
        fotoapparat?.start()
    }

    private fun takePicture() {
        val photoResult = fotoapparat?.takePicture() ?: return
        photoResult
            .toBitmap()
            .whenAvailable { bitmapPhoto ->
                if (bitmapPhoto != null) {
                    val rotatedBitmap = rotate(bitmapPhoto)
                    thumbView.setImageBitmap(rotatedBitmap)
                    bitmapRepository.bitmap = rotatedBitmap
                    startActivity(Intent(this, ChooseNetworkActivity::class.java))
                }
            }
    }

    private fun rotate(bitmapPhoto: BitmapPhoto): Bitmap {
        Timber.i("${bitmapPhoto.rotationDegrees}")
        val rotationCompensation = -bitmapPhoto.rotationDegrees.toFloat()
        val source = bitmapPhoto.bitmap
        val matrix = Matrix()
        matrix.postRotate(rotationCompensation)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    override fun onStart() {
        super.onStart()
        fotoapparat?.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}