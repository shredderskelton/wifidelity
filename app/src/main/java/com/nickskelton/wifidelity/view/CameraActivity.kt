package com.nickskelton.wifidelity.view

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.model.SingleItemRepository
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
import kotlinx.android.synthetic.main.activity_main.*

class CameraActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private var fotoApparat: Fotoapparat? = null

    private val bitmapRepository: SingleItemRepository<Bitmap> by inject()

    private lateinit var disposable: Disposable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton.setOnClickListener { takePicture() }
        galleryButton.setOnClickListener { openGallery() }
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

    private fun openGallery() {
        // TODO
    }

    private fun initCamera() {
        fotoApparat = Fotoapparat(
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
        fotoApparat?.start()
    }

    private fun takePicture() {
        fotoApparat?.let { photoManager ->
            photoManager
                .takePicture()
                .toBitmap()
                .transform {
                    rotate(it)
                }
                .whenAvailable { bitmap ->
                    bitmap?.let {
                        val bitmapId = bitmapRepository.add(it)
                        ChooseNetworkActivity.start(this, bitmapId)
                    }
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
        fotoApparat?.start()
    }

    override fun onStop() {
        super.onStop()
        fotoApparat?.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.dispose()
    }
}