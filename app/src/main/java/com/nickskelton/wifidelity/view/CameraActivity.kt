package com.nickskelton.wifidelity.view

import android.Manifest
import android.animation.Animator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.R.id.cameraButton
import com.nickskelton.wifidelity.R.id.galleryButton
import com.nickskelton.wifidelity.R.id.snapShotOverlay
import com.nickskelton.wifidelity.model.SingleItemRepository
import com.tbruyelle.rxpermissions2.RxPermissions
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.result.BitmapPhoto
import io.fotoapparat.selector.autoFlash
import io.fotoapparat.selector.back
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.IOException
import java.util.Collections.rotate
import android.view.animation.AlphaAnimation

class CameraActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private var fotoApparat: Fotoapparat? = null

    private val bitmapRepository: SingleItemRepository<Bitmap> by inject()

    private lateinit var disposable: Disposable

    private var timeToTakeAPhoto = 3000L

    private var split = 3L

    private fun View.addBottomMargin(bottomMargin: Int) {
        val params = layoutParams as CoordinatorLayout.LayoutParams
        params.bottomMargin += bottomMargin
        layoutParams = params
    }

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

    private fun initCamera() {
        fotoApparat = Fotoapparat(
            context = this,
            view = cameraView,                   // view which will draw the camera preview
            cameraConfiguration = CameraConfiguration(autoFlash()),
            scaleType = ScaleType.CenterCrop,    // (optional) we want the preview to fill the view
            lensPosition = back(),               // (optional) we want back camera
            cameraErrorCallback = { error -> Timber.e(error) } // (optional) log fatal errors
        )
        fotoApparat?.start()
    }

    private fun disableInteractions() {
        setInteractions(false)
        snapShotOverlay.visible(true)
        val animation1 = AlphaAnimation(1.0f, 0.1f)
        animation1.duration = timeToTakeAPhoto
        animation1.startOffset = 0
        animation1.interpolator = android.view.animation.AccelerateInterpolator()
        animation1.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(p0: Animation?) {
            }

            override fun onAnimationEnd(p0: Animation?) {
                snapShotOverlay.visible(false)
            }

            override fun onAnimationStart(p0: Animation?) {
            }
        })
        snapShotOverlay.startAnimation(animation1)
//
//        snapShotOverlay.alpha = 0.5f
//        snapShotOverlay.visible(true)
//        snapShotOverlay.animate()
//            .setDuration(1000)
//            .alpha(0F)
//            .setInterpolator(LinearInterpolator())
//            .setAnimationStartListener { snapShotOverlay.visible(true) }
////            .setAnimationEndListener { snapShotOverlay.visible(false) }
//            .start()
    }

    private fun enableInteractions() {
        setInteractions(true)
    }

    private fun setInteractions(enabled: Boolean) {
        cameraButton.visible(enabled)
        galleryButton.visible(enabled)
    }

    private fun takePicture() {
        split = System.currentTimeMillis()
        disableInteractions()
        fotoApparat?.let { photoManager ->
            photoManager
                .takePicture()
                .toBitmap()
                .transform {
                    if (Looper.myLooper() == Looper.getMainLooper()) {
                        Timber.e("Shouldn't be on the main thread")
                    }
                    rotate(it)
                }
                .whenAvailable { bitmap ->
                    bitmap?.let {
                        processImage(it)
                    }
                    enableInteractions()
                    timeToTakeAPhoto = System.currentTimeMillis() - split
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
        private val RESULT_LOAD_IMAGE = 8734
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