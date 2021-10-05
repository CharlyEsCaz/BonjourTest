package com.example.bonjourtest

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.bonjourtest.adapter.BonjourAdapter
import com.example.bonjourtest.databinding.ActivityMainBinding
import com.example.bonjourtest.models.Device

class WiFiDirectActivity : AppCompatActivity() {


    private val TAG = "BONJOUR_CONNECTION"
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var serviceRequest: WifiP2pDnsSdServiceRequest

    private lateinit var vBind: ActivityMainBinding
    private var devices = ArrayList<Device>()
    private var adapter: BonjourAdapter? = null

    private val buddies = mutableMapOf<String, String>()

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = DataBindingUtil.setContentView(this, R.layout.activity_main)
        setupAdapter()

        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(this, mainLooper, null)

        startRegistration()
        discoverService()

        manager.discoverServices(
            channel,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Discovering services")
                }

                override fun onFailure(code: Int) {
                    Log.d(TAG, "P2P Failure. $code")
                    // Command failed. Check for P2P_UNSUPPORTED, ERROR, or BUSY
                    when (code) {

                        WifiP2pManager.P2P_UNSUPPORTED -> {
                            Log.d(TAG, "P2P isn't supported on this device.")
                        }
                    }
                }
            }
        )
    }

    private fun discoverService() {

        val txtListener = WifiP2pManager.DnsSdTxtRecordListener { fullDomain, record, device ->
            Log.d(TAG, "DnsSdTxtRecord full domain -$fullDomain")
            Log.d(TAG, "DnsSdTxtRecord Service -${device.deviceName}")
            record["buddyname"]?.also {
                buddies[device.deviceAddress] = it
            }
        }

        val servListener =
            WifiP2pManager.DnsSdServiceResponseListener { instanceName, registrationType, resourceType ->
                // Update the device name with the human-friendly version from
                // the DnsTxtRecord, assuming one arrived.
                resourceType.deviceName =
                    buddies[resourceType.deviceAddress] ?: resourceType.deviceName

                Log.d(TAG, resourceType.toString())
                Log.d(TAG, resourceType.deviceName)

                // Add to the custom adapter defined specifically for showing
                // wifi devices.

                Log.d(TAG, "onBonjourServiceAvailable $instanceName")
                onDeviceFound(instanceName)
            }

        manager.setDnsSdResponseListeners(channel, servListener, txtListener)

        serviceRequest = WifiP2pDnsSdServiceRequest.newInstance()
        manager.addServiceRequest(
            channel,
            serviceRequest,
            object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "Service Request Added")
                }

                override fun onFailure(code: Int) {
                    // Command failed.  Check for P2P_UNSUPPORTED, ERROR, or BUSY
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    private fun startRegistration() {

        val name = "Charly${(0 until 10).random()}"
        val record: Map<String, String> = mapOf(
            "listenport" to "0",
            "buddyname" to name,
            "available" to "visible"
        )

        val serviceInfo =
            WifiP2pDnsSdServiceInfo.newInstance(name, "_offline._tcp", record)

        manager.addLocalService(channel, serviceInfo, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "Registration succes as $name")
            }

            override fun onFailure(arg0: Int) {
                Log.d(TAG, "Registration failed: $arg0")
            }
        })
    }

    private fun setupAdapter() {
        adapter = BonjourAdapter(devices)
        vBind.rvUsers.adapter = adapter
    }

    private fun onDeviceFound(name: String) {
        runOnUiThread {
            val device = Device(name)
            devices.add(device)
            adapter?.devices = devices
            adapter?.notifyDataSetChanged()
        }

    }


}