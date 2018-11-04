package com.nickskelton.wifidelity.view

import android.Manifest
import android.arch.lifecycle.Observer
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import com.nickskelton.wifidelity.R
import com.nickskelton.wifidelity.databinding.ActivityChooseNetworkBinding
import com.nickskelton.wifidelity.view.adapter.BlockListItem
import com.nickskelton.wifidelity.view.adapter.SimpleBlockAdapter
import com.tbruyelle.rxpermissions2.RxPermissions
import org.koin.android.viewmodel.ext.android.viewModel
import timber.log.Timber
import kotlinx.android.synthetic.main.activity_choose_network.*

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
        binding.vm!!.items.observe(this, Observer<List<BlockListItem>> { adapter.updateItems(it!!) })

        recyclerMan.adapter = adapter
        recyclerMan.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        retryButton.setOnClickListener { finish() }

        setTitle("Select your wifi network: ")
//        bindView()
//        bindViewModel()
        setupPermissions()
    }

//
//    private fun bindView() {
//        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
//        recyclerView.adapter = adapter
//        retryButton.setOnClickListener { finish() }
//    }
//
//    private fun bindViewModel() {
//        binding = DataBindingUtil.setContentView(this, R.layout.activity_choose_network)
//        binding.setLifecycleOwner(this)
//        binding.vm = chooseNetworkViewModel
//        binding.vm!!.items.observe(this, Observer<List<BlockListItem>> { adapter.updateItems(it!!) })
//    }
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
        wifiDialog.show(fragmentManager, "")
    }
//
////
////        RxView.clicks(findViewById(R.id.connectButton))
////                .compose(RxPermissions(this).ensure(Manifest.permission.ACCESS_WIFI_STATE, Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_COARSE_LOCATION))
////                .subscribe({ granted ->
////                    if (granted)
////                        wifiManager.scan()
////                })
//
//    private fun onNewResults(result: DetectionResult?) {
//        when (result) {
//            is DetectionResult.Success -> adapter.updateItems(result.blocks)
//            is DetectionResult.Failed -> Toast.makeText(this, "Error: ${result.exception.localizedMessage}", Toast.LENGTH_LONG).show()
//        }
//    }
}