package com.nickskelton.wifidelity.extensions

import android.content.Context
import android.content.Intent
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

inline fun <reified T : AppCompatActivity> Context.start(extras: ((Intent) -> Intent)) {
    val intent = Intent(this, T::class.java)
    val intentWithExtras = extras(intent)
    ActivityCompat.startActivity(this, intentWithExtras, null)
}
