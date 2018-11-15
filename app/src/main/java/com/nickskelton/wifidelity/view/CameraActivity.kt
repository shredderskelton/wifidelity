package com.nickskelton.wifidelity.view

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.PixelCopy
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.tbruyelle.rxpermissions2.RxPermissions
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.preview.Frame
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.selector.autoFlash
import io.fotoapparat.selector.back
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import android.os.HandlerThread
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth

class CameraActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private var fotoApparat: Fotoapparat? = null

    private val bitmapRepository: SingleItemRepository<Bitmap> by inject()

    private lateinit var disposable: Disposable

    private fun View.addBottomMargin(bottomMargin: Int) {
        val params = layoutParams as CoordinatorLayout.LayoutParams
        params.bottomMargin += bottomMargin
        layoutParams = params
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraButton.setOnApplyWindowInsetsListener { _, windowInsets ->
            val bottomInset = windowInsets.systemWindowInsetBottom
            cameraButton.addBottomMargin(bottomInset)
            galleryButton.addBottomMargin(bottomInset)
            windowInsets.consumeSystemWindowInsets()
        }
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
        val intent = Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, RESULT_LOAD_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, data.data)
                processImage(bitmap)
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error retrieving photo", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private var lastFrame: Frame? = null

    fun screenShot() {
        // Create a bitmap the size of the scene view.
        val bitmap = Bitmap.createBitmap(
            cameraView.getWidth(),
            cameraView.getHeight(),
            Bitmap.Config.ARGB_8888
        )

        // Create a handler thread to offload the processing of the image.
        val handlerThread = HandlerThread("PixelCopier")
        handlerThread.start()
        // Make the request to copy.
        PixelCopy.request(window, bitmap, { copyResult: Int ->
            if (copyResult === PixelCopy.SUCCESS) {
                snapsnotImageView.setImageBitmap(bitmap)
                snapsnotImageView.visibility = View.VISIBLE
            } else {
            }
            handlerThread.quitSafely()
        }, Handler(handlerThread.looper))
    }

    private fun initCamera() {
        cameraView.getPreview()
        fotoApparat = Fotoapparat(
            context = this,
            view = cameraView,                   // view which will draw the camera preview
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            cameraConfiguration = CameraConfiguration(autoFlash(),
                frameProcessor = { frame ->
                    lastFrame = frame
                }
            ),
            lensPosition = back(),               // (optional) we want back camera
            cameraErrorCallback = { error -> Timber.e(error) } // (optional) log fatal errors
        )
        fotoApparat?.start()
    }

    private fun takePicture() {
        screenShot()
        fotoApparat?.let { photoManager ->
            photoManager
                .takePicture()
                .toBitmap()
                .transform {
                    rotate(it)
                }
                .whenAvailable { bitmap ->
                    bitmap?.let {
                        processImage(it)
                    }
                }
        }
    }

    private fun processImage(it: Bitmap) {
        val bitmapId = bitmapRepository.add(it)
        ChooseNetworkActivity.start(this, bitmapId)
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

    companion object {
        private const val RESULT_LOAD_IMAGE = 8734
    }
}