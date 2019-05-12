package com.nickskelton.wifidelity.view.video.frame

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Camera
import android.util.Log
import android.view.Surface
import android.view.WindowManager

import com.google.android.gms.common.images.Size

/**
 * If the absolute difference between a preview size aspect ratio and a picture size aspect ratio
 * is less than this tolerance, they are considered to be the same aspect ratio.
 */
private const val ASPECT_RATIO_TOLERANCE = 0.01f

/**
 * Selects the most suitable preview and picture size, given the desired width and height.
 *
 *
 * Even though we only need to find the preview size, it's necessary to find both the preview
 * size and the picture size of the camera together, because these need to have the same aspect
 * ratio. On some hardware, if you would only set the preview size, you will get a distorted
 * image.
 *
 * @param camera the camera to select a preview size from
 * @param desiredWidth the desired width of the camera preview frames
 * @param desiredHeight the desired height of the camera preview frames
 * @return the selected preview and picture size pair
 */
fun Camera.selectSizePair(
    desiredWidth: Int,
    desiredHeight: Int
): SizePair? {
    val validPreviewSizes = generateValidPreviewSizeList()
    // The method for selecting the best size is to minimize the sum of the differences between
    // the desired values and the actual values for width and height.  This is certainly not the
    // only way to select the best size, but it provides a decent tradeoff between using the
    // closest aspect ratio vs. using the closest pixel area.
    var selectedPair: SizePair? = null
    var minDiff = Integer.MAX_VALUE
    for (sizePair in validPreviewSizes) {
        val size = sizePair.preview
        val diff =
            Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight)
        if (diff < minDiff) {
            selectedPair = sizePair
            minDiff = diff
        }
    }

    return selectedPair
}

/**
 * Generates a list of acceptable preview sizes. Preview sizes are not acceptable if there is not
 * a corresponding picture size of the same aspect ratio. If there is a corresponding picture size
 * of the same aspect ratio, the picture size is paired up with the preview size.
 *
 *
 * This is necessary because even if we don't use still pictures, the still picture size must
 * be set to a size that is the same aspect ratio as the preview size we choose. Otherwise, the
 * preview images may be distorted on some devices.
 */
private fun Camera.generateValidPreviewSizeList(): List<SizePair> {
    val supportedPreviewSizes = parameters.supportedPreviewSizes
    val supportedPictureSizes = parameters.supportedPictureSizes
    val validPreviewSizes = ArrayList<SizePair>()
    for (previewSize in supportedPreviewSizes) {
        val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()
        // By looping through the picture sizes in order, we favor the higher resolutions.
        // We choose the highest resolution in order to support taking the full resolution
        // picture later.
        for (pictureSize in supportedPictureSizes) {
            val pictureAspectRatio =
                pictureSize.width.toFloat() / pictureSize.height.toFloat()
            if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                validPreviewSizes.add(
                    SizePair(
                        previewSize,
                        pictureSize
                    )
                )
                break
            }
        }
    }
    // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
    // of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
    // still account for it.
    if (validPreviewSizes.size == 0) {
        Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size")
        for (previewSize in supportedPreviewSizes) {
            // The null picture size will let us know that we shouldn't set a picture size.
            validPreviewSizes.add(
                SizePair(
                    previewSize,
                    null
                )
            )
        }
    }
    validPreviewSizes.forEach {
        Log.w(TAG, "Valid preview size: $it")
    }
    return validPreviewSizes
}

/**
 * Selects the most suitable preview frames per second range, given the desired frames per second.
 *
 * @param camera the camera to select a frames per second range from
 * @param desiredPreviewFps the desired frames per second for the camera preview frames
 * @return the selected preview frames per second range
 */
@SuppressLint("InlinedApi")
fun Camera.selectPreviewFpsRange(desiredPreviewFps: Float): IntArray? {
    // The camera API uses integers scaled by a factor of 1000 instead of floating-point frame
    // rates.
    val desiredPreviewFpsScaled = (desiredPreviewFps * 1000.0f).toInt()
    // The method for selecting the best range is to minimize the sum of the differences between
    // the desired value and the upper and lower bounds of the range.  This may select a range
    // that the desired value is outside of, but this is often preferred.  For example, if the
    // desired frame rate is 29.97, the range (30, 30) is probably more desirable than the
    // range (15, 30).
    var selectedFpsRange: IntArray? = null
    var minDiff = Integer.MAX_VALUE
    val previewFpsRangeList = parameters.supportedPreviewFpsRange
    for (range in previewFpsRangeList) {
        val deltaMin =
            desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
        val deltaMax =
            desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
        val diff = Math.abs(deltaMin) + Math.abs(deltaMax)
        if (diff < minDiff) {
            selectedFpsRange = range
            minDiff = diff
        }
    }
    return selectedFpsRange
}

/**
 * Stores a preview size and a corresponding same-aspect-ratio picture size. To avoid distorted
 * preview images on some devices, the picture size must be set to a size that is the same aspect
 * ratio as the preview size or the preview may end up being distorted. If the picture size is
 * null, then there is no picture size with the same aspect ratio as the preview size.
 */
class SizePair(
    previewSize: Camera.Size,
    pictureSize: Camera.Size?
) {
    val preview: Size = Size(previewSize.width, previewSize.height)
    val picture: Size? = pictureSize?.let { Size(it.width, it.height) }

    override fun toString(): String = "Preview: $preview, Picture: $picture"
}

/**
 * Calculates the correct rotation for the given camera id and sets the rotation in the
 * parameters. It also sets the camera's display orientation and rotation.
 *
 * @param parameters the camera parameters for which to set the rotation
 * @param cameraId the camera id to set rotation based on
 */
fun Camera.setRotation(context: Context, parameters: Camera.Parameters, cameraId: Int): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    var degrees = 0
    when (windowManager.defaultDisplay.rotation) {
        Surface.ROTATION_0 -> degrees = 0
        Surface.ROTATION_90 -> degrees = 90
        Surface.ROTATION_180 -> degrees = 180
        Surface.ROTATION_270 -> degrees = 270
        else -> Log.e(TAG, "Bad rotation value: ${windowManager.defaultDisplay.rotation}")
    }
    val cameraInfo = Camera.CameraInfo()
    Camera.getCameraInfo(cameraId, cameraInfo)
    val angle: Int
    val displayAngle: Int
    if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        angle = (cameraInfo.orientation + degrees) % 360
        displayAngle = (360 - angle) % 360 // compensate for it being mirrored
    } else { // back-facing
        angle = (cameraInfo.orientation - degrees + 360) % 360
        displayAngle = angle
    }

    setDisplayOrientation(displayAngle)
    parameters.setRotation(angle)

    // This corresponds to the rotation constants.
    return angle / 90
}

/**
 * Gets the id for the camera specified by the direction it is facing. Returns -1 if no such
 * camera was found.
 *
 * @param facing the desired camera (front-facing or rear-facing)
 */
fun getIdForRequestedCamera(facing: Int): Int {
    val cameraInfo = Camera.CameraInfo()
    for (i in 0 until Camera.getNumberOfCameras()) {
        Camera.getCameraInfo(i, cameraInfo)
        if (cameraInfo.facing == facing) {
            return i
        }
    }
    return -1
}

private const val TAG = "CameraExtensions"