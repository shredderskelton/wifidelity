package com.nickskelton.wifidelity.extensions

import io.fotoapparat.preview.Frame
import io.fotoapparat.preview.FrameProcessor
import timber.log.Timber

class TextRecognitionFrameProcessor : FrameProcessor {

    private var busy = false
    private var droppedFrameRate = 0

private val dropFrameCounter = DropFrameCounter()

    override fun process(frame: Frame) {
        if (busy) {
            droppedFrameRate++
            return
        }
        busy = true
        Timber.d("Dropped Frames: ${this.droppedFrameRate}")
        droppedFrameRate = 0
    }
}

class DropFrameCounter {
    private var busy = false
    private var droppedFrameRate = 0

    fun lock(): Boolean {
        if (busy) {
            droppedFrameRate++
            return false
        }
        busy = true
        Timber.d("Dropped Frames: ${this.droppedFrameRate}")
        droppedFrameRate = 0
        return true
    }

    fun unlock() {
        busy = false
    }
}