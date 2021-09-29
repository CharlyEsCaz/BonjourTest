package com.example.bonjourtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.example.bonjourtest.adapter.BonjourAdapter
import com.example.bonjourtest.databinding.ActivityMainBinding
import com.example.bonjourtest.models.Device


class MainActivity : AppCompatActivity(), BonjourSettings.BonjourActions {

    private lateinit var vBind: ActivityMainBinding
    private var devices = ArrayList<Device>()
    private var adapter: BonjourAdapter? = null
    private lateinit var bonjour: BonjourSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setupAdapter()
        bonjour = BonjourSettings("Charly", this, this)
        bonjour.initializeServerSocket()
    }

    private fun setupAdapter() {
        adapter = BonjourAdapter(devices)
        vBind.rvUsers.adapter = adapter
    }

    override fun onDeviceFound(name: String) {
        runOnUiThread {
            val device = Device(name)
            devices.add(device)
            adapter?.devices = devices
            adapter?.notifyDataSetChanged()
        }

    }

    override fun onDeviceLost(name: String) {

        runOnUiThread {
            val device = devices.find { it.name == name }
            devices.remove(device)
            adapter?.devices = devices
            adapter?.notifyDataSetChanged()
        }

    }


}