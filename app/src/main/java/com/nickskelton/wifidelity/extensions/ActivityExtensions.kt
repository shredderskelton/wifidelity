package com.nickskelton.wifidelity.extensions

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat

inline fun <reified T : AppCompatActivity> Context.start(extras: ((Intent) -> Intent)) {
    val intent = Intent(this, T::class.java)
    val intentWithExtras = extras(intent)
    ActivityCompat.startActivity(this, intentWithExtras, null)
}

fun Drawable.toBitmap(): Bitmap {

    if (this is BitmapDrawable) {
        return bitmap
    }

    val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)

    return bitmap
}

fun View.addBottomMargin(bottomMargin: Int) {
    val params = layoutParams as CoordinatorLayout.LayoutParams
    params.bottomMargin += bottomMargin
    layoutParams = params
}
