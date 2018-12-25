package com.nickskelton.wifidelity.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import timber.log.Timber

class DetectionOverlay : View {

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initView()
    }

    constructor(context: Context) : super(context) {
        initView()
    }

    private fun initView() {
        this.setWillNotDraw(false)
    }

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
    }

    var rectangles: Array<RectF?> = emptyArray()
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (isInEditMode) {
            canvas?.drawRect(
                Rect(
                    width / 4,
                    height / 4,
                    3 * width / 4,
                    3 * height / 4
                ), paint
            )
        } else {
            rectangles.forEach {
                if (it != null) {
                    Timber.d("Drawing Rect: $it")
                    canvas?.drawRect(it, paint)
                }
            }
        }
    }
}