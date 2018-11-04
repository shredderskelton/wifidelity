package com.nickskelton.wifidelity.view

import android.app.Dialog
import android.app.DialogFragment
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import com.nickskelton.wifidelity.R

class WifiPermissionDialog() : DialogFragment() {

    var onContinue: (() -> Any)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val dialog = context?.let { ctx ->
            AlertDialog.Builder(ctx)
                    .setView(createView(ctx))
                    .setTitle(R.string.enable_auto_connect)
                    .setNegativeButton(R.string.no_thanks, null)
                    .setPositiveButton(R.string.yes) { dialog, _ ->
                        onContinue?.invoke()
                        dialog.dismiss()
                    }
                    .create()
        }

        return dialog!!
    }

    private fun createView(context: Context): View {
        return LayoutInflater
                .from(context)
                .inflate(R.layout.dialog_wifi_permissions, null)
    }
}