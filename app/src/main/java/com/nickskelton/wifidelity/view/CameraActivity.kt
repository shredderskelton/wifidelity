package com.nickskelton.wifidelity.view

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.tbruyelle.rxpermissions2.RxPermissions
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.selector.back
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import android.view.animation.AlphaAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.nickskelton.punchcard.utils.PreferenceHelper
import com.nickskelton.wifidelity.extensions.NicksFrameProcessor
import com.nickskelton.wifidelity.utils.PerformanceLogger
import io.fotoapparat.result.Photo
import java.io.IOException

class CameraActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private var fotoApparat: Fotoapparat? = null

    private val bitmapRepository: SingleItemRepository<Photo> by inject()

    private val frameProcessor by lazy {
        NicksFrameProcessor(detectionOverlayView)
    }

    private lateinit var disposable: Disposable

    private val performanceLogger = PerformanceLogger("Photo")

    private fun View.addBottomMargin(bottomMargin: Int) {
        val params = layoutParams as CoordinatorLayout.LayoutParams
        params.bottomMargin += bottomMargin
        layoutParams = params
    }

    private lateinit var detectionOverlayControl: DetectionOverlay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        cameraButton.setOnApplyWindowInsetsListener { view, windowInsets ->
            val bottomInset = windowInsets.systemWindowInsetBottom
            cameraButton.addBottomMargin(bottomInset)
            galleryButton.addBottomMargin(bottomInset)
            windowInsets.consumeSystemWindowInsets()
        }
        cameraButton.setOnClickListener { takePicture() }
        galleryButton.setOnClickListener { openGallery() }
        rxPermissions = RxPermissions(this)

        detectionOverlayControl = detectionOverlayView

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

    private fun initCamera() {
        fotoApparat = Fotoapparat(
            context = this,
            view = cameraView,                   // view which will draw the camera preview
            cameraConfiguration = CameraConfiguration(
                //autoFlash(),
                frameProcessor = frameProcessor
            ),
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            lensPosition = back(),               // (optional) we want back camera
            cameraErrorCallback = { error -> Timber.e(error) } // (optional) log fatal errors
        )
        fotoApparat?.start()
    }

    private fun onPictureCaptureStarted() {
        setInteractions(false)
        // flashAnimation()
    }

//    private fun flashAnimation() {
//        val animation1 = AlphaAnimation(1.0f, 0.1f).apply {
//            duration = animationDuration
//            interpolator = AccelerateInterpolator()
//            setAnimationListener(object : Animation.AnimationListener {
//                override fun onAnimationRepeat(p0: Animation?) {
//                }
//
//                override fun onAnimationEnd(p0: Animation?) {
//                    snapShotOverlay.visible(false)
//                }
//
//                override fun onAnimationStart(p0: Animation?) {
//                    snapShotOverlay.visible(true)
//                }
//            })
//        }
//
//        snapShotOverlay.startAnimation(animation1)
//    }

    private var animationDuration: Long
        get() {
            return PreferenceHelper
                .defaultPrefs(this@CameraActivity)
                .getLong(PREF_CAPTURE_TIME, 100L)
        }
        set(value) {
            PreferenceHelper
                .defaultPrefs(this)
                .edit()
                .putLong(PREF_CAPTURE_TIME, value)
                .apply()
        }

    private fun onPictureCaptureFinished() {
        setInteractions(true)
    }

    private fun setInteractions(enabled: Boolean) {
        cameraButton.visible(enabled)
        galleryButton.visible(enabled)
//        progressBar.visible(!enabled)
    }

    private fun takePicture() {
        performanceLogger.reset()
        performanceLogger.addSplit("Started")
        onPictureCaptureStarted()

//        ChooseNetworkActivity.start(this, "", frameProcessor.lastResults.toTypedArray())

//        fotoApparat?.let { photoManager ->
//            photoManager
//                .takePicture()
//                .toPendingResult()
//                .transform {
//                    Timber.d("Rotation: " + it.rotationDegrees)
//                    lastRotation = it.rotationDegrees
//                    it
//                }
//                .whenAvailable { bitmap ->
//                    bitmap?.let {
//                        processImage(it)
//                    }
//                    onPictureCaptureFinished()
//                }
//
//        }
        onPictureCaptureFinished()
    }
//
//    private fun processImage(it: Photo) {
//        val bitmapId = bitmapRepository.add(it)
//        ChooseNetworkActivity.start(this, bitmapId, lastRotation)
//    }

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
        private const val PREF_CAPTURE_TIME = "photoCaptureMillis"
    }
}

fun ViewPropertyAnimator.setAnimationEndListener(listener: (Animator?) -> Unit): ViewPropertyAnimator {
    this.setListener(object : OnAnimationEndListener {
        override fun onAnimationEnd(animator: Animator?) {
            listener.invoke(animator)
        }
    })
    return this
}

fun ViewPropertyAnimator.setAnimationStartListener(listener: (Animator?) -> Unit): ViewPropertyAnimator {
    this.setListener(object : OnAnimationStartListener {
        override fun onAnimationStart(animator: Animator?) {
            listener.invoke(animator)
        }
    })
    return this
}

interface OnAnimationEndListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(p0: Animator?) {
    }

    override fun onAnimationCancel(p0: Animator?) {
    }

    override fun onAnimationStart(p0: Animator?) {
    }
}

interface OnAnimationStartListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(p0: Animator?) {
    }

    override fun onAnimationCancel(p0: Animator?) {
    }

    override fun onAnimationEnd(p0: Animator?) {
    }
}