package com.nickskelton.wifidelity.view.video

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.common.images.Size
import com.nickskelton.wifidelity.common.CameraDirection
import com.nickskelton.wifidelity.common.FrameMetadata
import com.nickskelton.wifidelity.view.video.frame.getIdForRequestedCamera
import com.nickskelton.wifidelity.view.video.frame.processor.VisionImageProcessor
import com.nickskelton.wifidelity.view.video.frame.selectPreviewFpsRange
import com.nickskelton.wifidelity.view.video.frame.selectSizePair
import com.nickskelton.wifidelity.view.video.frame.setRotation
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer
import java.util.IdentityHashMap

interface CameraSource {
    fun start()
    fun stop()
    fun release()
    fun requestCameraDirection(direction: CameraDirection)
    fun setFrameProcessor(processor: VisionImageProcessor)
    val cameraPreviewSize: Size?
    val activeCamera: CameraDirection
}

object CameraSourceNoop : CameraSource {
    override fun start() {}
    override fun stop() {}
    override fun release() {}
    override fun requestCameraDirection(direction: CameraDirection) {}
    override fun setFrameProcessor(processor: VisionImageProcessor) {}
    override val cameraPreviewSize: Size?
        get() = null
    override val activeCamera: CameraDirection
        get() = CameraDirection.BACK
}

/**
 * Manages the camera and allows UI updates on top of it (e.g. overlaying extra Graphics or
 * displaying extra information). This receives preview frames from the camera at a specified rate,
 * sending those frames to child classes' detectors / classifiers as fast as it is able to process.
 */
@Suppress("DEPRECATION")
@SuppressLint("MissingPermission", "LogNotTimber")
class CameraSourceImpl(
    private val activity: Activity,
    private val graphicOverlayView: GraphicOverlayView
) : CameraSource {
    private var camera: Camera? = null
    private var cameraFacing = CameraDirection.FRONT
    override val activeCamera: CameraDirection get() = cameraFacing
    private var rotation: Int = 0
    private var previewSize: Size? = null
    override val cameraPreviewSize: Size?
        get() {
            Log.i(TAG, "Size requested and got $previewSize")
            return previewSize
        }
    // These values may be requested by the caller.  Due to hardware limitations, we may need to
    // select close, but not exactly the same values for these.
    // TODO tweak these values for performance and different displays
    private val requestedFps = 20.0f
    private val requestedPreviewWidth = 480
    private val requestedPreviewHeight = 360
    private val requestedAutoFocus = true
    // These instances need to be held onto to avoid GC of their underlying resources.  Even though
    // these aren't used outside of the method that creates them, they still must have hard
    // references maintained to them.
    private var dummySurfaceTexture: SurfaceTexture? = null
    // True if a SurfaceTexture is being used for the preview, false if a SurfaceHolder is being
    // used for the preview.  We want to be compatible back to Gingerbread, but SurfaceTexture
    // wasn't introduced until Honeycomb.  Since the interface cannot use a SurfaceTexture, if the
    // developer wants to display a preview we must use a SurfaceHolder.  If the developer doesn't
    // want to display a preview we use a SurfaceTexture if we are running at least Honeycomb.
    //private var usingSurfaceTexture: Boolean = false
    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private var processingThread: Thread? = null
    private val processingRunnable: FrameProcessingRunnable
    private val processorLock = Any()
    // @GuardedBy("processorLock")
    private var frameProcessor: VisionImageProcessor? = null
    /**
     * Map to convert between a byte array, received from the camera, and its associated byte buffer.
     * We use byte buffers internally because this is a more efficient way to call into native code
     * later (avoids a potential copy).
     *
     *
     * **Note:** uses IdentityHashMap here instead of HashMap because the behavior of an array's
     * equals, hashCode and toString methods is both useless and unexpected. IdentityHashMap enforces
     * identity ('==') check on the keys.
     */
    private val bytesToByteBuffer = IdentityHashMap<ByteArray, ByteBuffer>()

    init {
        graphicOverlayView.clear()
        processingRunnable = FrameProcessingRunnable()

        if (Camera.getNumberOfCameras() == 1) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(0, cameraInfo)
            cameraFacing = CameraDirection.parse(
                cameraInfo.facing
            ) ?: CameraDirection.BACK
        }
    }
    // ==============================================================================================
    // Public
    // ==============================================================================================
    /** Stops the camera and releases the resources of the camera and underlying detector.  */
    override fun release() {
        synchronized(processorLock) {
            stop()
            processingRunnable.release()
            cleanScreen()

            if (frameProcessor != null) {
                frameProcessor!!.stop()
            }
        }
    }

    /**
     * Opens the camera and starts sending preview frames to the underlying detector. The preview
     * frames are not displayed.
     *
     * @throws IOException if the camera's preview texture or display could not be initialized
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(Manifest.permission.CAMERA)
    @Synchronized
    @Throws(IOException::class)
    override fun start() {
        if (camera != null) {
            return
        }

        camera = createCamera()
        dummySurfaceTexture = SurfaceTexture(DUMMY_TEXTURE_NAME)
        camera!!.setPreviewTexture(dummySurfaceTexture)
//        usingSurfaceTexture = true
        camera!!.startPreview()

        processingThread = Thread(processingRunnable)
        processingRunnable.setActive(true)
        processingThread!!.start()
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     *
     *
     * This camera source may be restarted again by calling [.start] or [ ][.start].
     *
     *
     * Call [.release] instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
    @Synchronized
    override fun stop() {
        processingRunnable.setActive(false)
        if (processingThread != null) {
            try {
                // Wait for the thread to complete to ensure that we can't have multiple threads
                // executing at the same time (i.e., which would happen if we called start too
                // quickly after stop).
                processingThread!!.join()
            } catch (e: InterruptedException) {
                Log.d(TAG, "Frame processing thread interrupted on release.")
            }

            processingThread = null
        }

        if (camera != null) {
            camera!!.stopPreview()
            camera!!.setPreviewCallbackWithBuffer(null)
            try {
//                if (usingSurfaceTexture) {
                camera!!.setPreviewTexture(null)
//                } else {
//                    camera!!.setPreviewDisplay(null)
//                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }

            camera!!.release()
            camera = null
        }
        // Release the reference to any image buffers, since these will no longer be in use.
        bytesToByteBuffer.clear()
    }

    @Synchronized
    override fun requestCameraDirection(direction: CameraDirection) {
        this.cameraFacing = direction
    }

    /**
     * Opens the camera and applies the user settings.
     *
     * @throws IOException if camera cannot be found or preview cannot be processed
     */
    @SuppressLint("InlinedApi")
    @Throws(IOException::class)
    private fun createCamera(): Camera {
        val requestedCameraId = getIdForRequestedCamera(cameraFacing.value)
        if (requestedCameraId == -1) {
            throw IOException("Could not find requested camera.")
        }
        val camera = Camera.open(requestedCameraId)
        val sizePair = camera.selectSizePair(
            requestedPreviewWidth,
            requestedPreviewHeight
        )
            ?: throw IOException("Could not find suitable preview size.")
        Log.i(TAG, "Using sizes: $sizePair")
        val pictureSize = sizePair.picture
        previewSize = sizePair.preview
        val previewFpsRange = camera.selectPreviewFpsRange(requestedFps)
            ?: throw IOException("Could not find suitable preview frames per second range.")

        Log.i(TAG, "Preview FPS:")
        previewFpsRange.forEach {
            Log.i(TAG, "$it")
        }
        val parameters = camera.parameters

        if (pictureSize != null) {
            Log.i(TAG, "Using Picture size: $pictureSize")
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
        }
        Log.i(TAG, "Using Preview size: $previewSize")
        parameters.setPreviewSize(previewSize!!.width, previewSize!!.height)
        parameters.setPreviewFpsRange(
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
            previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
        )
        parameters.previewFormat = ImageFormat.NV21

        this.rotation = camera.setRotation(activity, parameters, requestedCameraId)

        if (requestedAutoFocus) {
            if (parameters
                    .supportedFocusModes
                    .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)
            ) {
                parameters.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
            } else {
                Log.i(TAG, "Camera auto focus is not supported on this device.")
            }
        }

        camera.parameters = parameters
        // Four frame buffers are needed for working with the camera:
        //
        //   one for the frame that is currently being executed upon in doing detection
        //   one for the next pending frame to process immediately upon completing detection
        //   two for the frames that the camera uses to populate future preview images
        //
        // Through trial and error it appears that two free buffers, in addition to the two buffers
        // used in this code, are needed for the camera to work properly.  Perhaps the camera has
        // one thread for acquiring images, and another thread for calling into user code.  If only
        // three buffers are used, then the camera will spew thousands of warning messages when
        // detection takes a non-trivial amount of time.
        camera.setPreviewCallbackWithBuffer { data, cameraObject ->
            processingRunnable.setNextFrame(data, cameraObject)
        }
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))
        camera.addCallbackBuffer(createPreviewBuffer(previewSize!!))

        return camera
    }

    /**
     * Creates one buffer for the camera preview callback. The size of the buffer is based off of the
     * camera preview size and the format of the camera image.
     *
     * @return a new preview buffer of the appropriate size for the current camera settings
     */
    @SuppressLint("InlinedApi")
    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = previewSize.height.toLong() * previewSize.width.toLong() * bitsPerPixel
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1
        // Creating the byte array this way and wrapping it, as opposed to using .allocate(),
        // should guarantee that there will be an array to work with.
        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)
//        if (!buffer.hasArray() || buffer.array() != byteArray) {
//            // I don't think that this will ever happen.  But if it does, then we wouldn't be
//            // passing the preview content to the underlying detector later.
//            throw IllegalStateException("Failed to create valid buffer for camera source.")
//        }
        bytesToByteBuffer[byteArray] = buffer
        return byteArray
    }
    // ==============================================================================================
    // Frame processing
    // ==============================================================================================

    override fun setFrameProcessor(processor: VisionImageProcessor) {
        synchronized(processorLock) {
            cleanScreen()
            frameProcessor?.stop()
            frameProcessor = processor
        }
    }

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera. This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     *
     *
     * While detection is running on a frame, new frames may be received from the camera. As these
     * frames come in, the most recent frame is held onto as pending. As soon as detection and its
     * associated processing is done for the previous frame, detection on the mostly recently received
     * frame will immediately start on the same thread.
     */
    private inner class FrameProcessingRunnable : Runnable {
        // This lock guards all of the member variables below.
        private val lock = Object()
        private var active = true
        // These pending variables hold the state associated with the new frame awaiting processing.
        private var pendingFrameData: ByteBuffer? = null

        /**
         * Releases the underlying receiver. This is only safe to do after the associated thread has
         * completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        internal fun release() = assert(processingThread?.state == Thread.State.TERMINATED)

        /** Marks the runnable as active/not active. Signals any blocked threads to continue.  */
        internal fun setActive(active: Boolean) {
            synchronized(lock) {
                this.active = active
                lock.notifyAll()
            }
        }

        /**
         * Sets the frame incomingPreviewFrame received from the camera. This adds the previous unused frame buffer (if
         * present) back to the camera, and keeps a pending reference to the frame incomingPreviewFrame for future use.
         */
        internal fun setNextFrame(incomingPreviewFrame: ByteArray, camera: Camera) {
            synchronized(lock) {
                if (pendingFrameData != null) {
                    Timber.w("Dropped a frame")
                    camera.addCallbackBuffer(pendingFrameData!!.array())
                    pendingFrameData = null
                }

                if (!bytesToByteBuffer.containsKey(incomingPreviewFrame)) {
                    Log.d(
                        TAG,
                        """Skipping frame. Could not find ByteBuffer associated
                            |with the image incomingPreviewFrame from the camera.""".trimMargin()
                    )
                    return
                }

                pendingFrameData = bytesToByteBuffer[incomingPreviewFrame]
                // Notify the processor thread if it is waiting on the next frame (see below).
                lock.notifyAll()
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames continuously.
         * The next pending frame is either immediately available or hasn't been received yet. Once it
         * is available, we transfer the frame info to local variables and run detection on that frame.
         * It immediately loops back for the next frame without pausing.
         *
         *
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context switching
         * or frame acquisition time latency.
         *
         *
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @SuppressLint("InlinedApi")
        override fun run() {
            var data: ByteBuffer? = null

            while (true) {
                synchronized(lock) {
                    while (active && pendingFrameData == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            lock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }
                    }

                    if (!active) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }
                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear pendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    pendingFrameData?.let {
                        data = it
                    }
                    pendingFrameData = null
                }
                data?.let { dataByteBuffer ->
                    // The code below needs to run outside of synchronization, because this will allow
                    // the camera to add pending frame(s) while we are running detection on the current
                    // frame.
                    try {
                        frameProcessor!!.process(
                            dataByteBuffer,
                            FrameMetadata(
                                previewSize!!.width,
                                previewSize!!.height,
                                rotation,
                                cameraFacing
                            ),
                            graphicOverlayView
                        )
                    } catch (t: Throwable) {
                        Log.e(TAG, "Exception thrown from receiver.", t)
                    } finally {
                        camera!!.addCallbackBuffer(dataByteBuffer.array())
                    }
                }
            }
        }
    }

    /** Cleans up graphicOverlayView and child classes can do their cleanups as well .  */
    private fun cleanScreen() {
        graphicOverlayView.clear()
    }

    companion object {
        private const val TAG = "CameraSource"
        /**
         * The dummy surface texture must be assigned a chosen name. Since we never use an OpenGL context,
         * we can choose any ID we want here. The dummy surface texture is not a crazy hack - it is
         * actually how the camera team recommends using the camera without a preview.
         */
        private const val DUMMY_TEXTURE_NAME = 100
    }
}
