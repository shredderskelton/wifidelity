package com.nickskelton.wifidelity.view

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityChooseNetworkBinding
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.nickskelton.wifidelity.viewmodel.observeNonNull
import com.tbruyelle.rxpermissions2.RxPermissions
import timber.log.Timber
import kotlinx.android.synthetic.main.activity_choose_network.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class ChooseNetworkActivity : AppCompatActivity() {

    private val requiredPermissions =
        arrayOf(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION)

    private lateinit var rxPermissions: RxPermissions
    private lateinit var binding: ActivityChooseNetworkBinding
    private val adapter = SimpleBlockAdapter()

    private val chooseNetworkViewModel: ChooseNetworkViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_network)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_network)
        binding.setLifecycleOwner(this)
        binding.vm = chooseNetworkViewModel

        binding.vm!!.apply {
            observeNonNull(items) {
                adapter.updateItems(it)
            }
            observeNonNull(actionNext) {
                startPasswordSelection()
            }
        }

        recyclerMan.adapter = adapter
        recyclerMan.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        retryButton.setOnClickListener { finish() }

        setTitle("Select your wifi network: ")
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