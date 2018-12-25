package com.nickskelton.wifidelity.view.start

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
import com.nickskelton.wifidelity.view.ChooseNetworkActivity
import com.nickskelton.wifidelity.view.DetectionOverlay
import com.nickskelton.wifidelity.view.network.text.NetworkTextActivity
import com.nickskelton.wifidelity.view.visible
import io.fotoapparat.result.Photo
import java.io.IOException

class StartActivity : AppCompatActivity() {

    private lateinit var rxPermissions: RxPermissions

    private var fotoApparat: Fotoapparat? = null

    private val frameProcessor by lazy {
        NicksFrameProcessor(detectionOverlayView)
    }

    private lateinit var disposable: Disposable

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
    private fun takePicture() {
        NetworkTextActivity.start(this, NetworkTextActivity.Args(frameProcessor.lastResults))
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
