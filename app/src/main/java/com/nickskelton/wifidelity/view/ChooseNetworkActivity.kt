package com.nickskelton.wifidelity.view

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityChooseNetworkBinding
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.activity_choose_network.*
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

class ChooseNetworkActivity : AppCompatActivity() {
    companion object {
        private const val ARG_BITMAP_ID = "bitmap"

        fun start(context: Context, bitmapId: String) {
            val intent = Intent(context, ChooseNetworkActivity::class.java)
            intent.putExtra(ARG_BITMAP_ID, bitmapId)
            context.startActivity(intent)
        }
    }

    private val requiredPermissions =
        arrayOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION)

    private lateinit var rxPermissions: RxPermissions
    private lateinit var binding: ActivityChooseNetworkBinding
    private val adapter = SimpleBlockAdapter()

    private val bitmapId by lazy {
        intent.getStringExtra(ARG_BITMAP_ID)
    }

    private val chooseNetworkViewModel: ChooseNetworkViewModel by viewModel { parametersOf(bitmapId) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_network)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_network)
        binding.setLifecycleOwner(this)
        binding.vm = chooseNetworkViewModel

        binding.vm!!.apply {
            observeNonNull(items) {
                adapter.updateItems(it)
                recyclerMan.scrollToPosition(0)
            }
            observeNonNull(actionNext) {
                startPasswordSelection()
            }
            observeNonNull(actionShowDialog) {
                AlertDialog.Builder(this@ChooseNetworkActivity)
                    .setTitle("No problem")
                    .setMessage("We'll skip the network and you can enter it manually in a moment")
                    .setPositiveButton("Ok") { _, _ ->
                        startPasswordSelection()
                    }
                    .show()
            }
        }

        recyclerMan.adapter = adapter
        recyclerMan.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        retryButton.setOnClickListener { finish() }

        title = "Select your wifi network: "
        setupPermissions()
    }

    private fun startPasswordSelection() {
        startActivity(Intent(this, ChoosePasswordActivity::class.java))
    }

    private fun setupPermissions() {
        rxPermissions = RxPermissions(this)
        val allPermissionsGranted = requiredPermissions.all { rxPermissions.isGranted(it) }
        if (!allPermissionsGranted) {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        val wifiDialog = WifiPermissionDialog()
        wifiDialog.onContinue = {
            rxPermissions
                .request(*requiredPermissions)
                .subscribe {
                    if (it) {
                        Timber.w("User declined permissions")
                    }
                }
        }
        wifiDialog.show(supportFragmentManager, "")
    }
}