package com.example.bonjourtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.net.wifi.WifiManager
import android.util.Log
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.example.bonjourtest.adapter.BonjourAdapter
import com.example.bonjourtest.databinding.ActivityConnectionBinding
import com.example.bonjourtest.models.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.jmdns.JmDNS
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceListener


class ConnectionActivity : AppCompatActivity() {

    private lateinit var vBind: ActivityConnectionBinding
    private var devices = ArrayList<Device>()
    private var adapter: BonjourAdapter? = null

    private var type = "_offline_test._tcp.local."
    //private val type = "_workstation._tcp.local."
    private var jmdns: JmDNS? = null
    private val TAG = "BONJOUR_CONNECTION"
    private var serviceInfo: ServiceInfo? = null
    private lateinit var lock: WifiManager.MulticastLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = DataBindingUtil.setContentView(this, R.layout.activity_connection)

        setupAdapter()
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                setup()
            }
        }
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

    private fun onDeviceLost(name: String) {
        runOnUiThread {
            val device = devices.find { it.name == name }
            devices.remove(device)
            adapter?.devices = devices
            adapter?.notifyDataSetChanged()
        }

    }

    private fun setup() {
        val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        lock = wifi.createMulticastLock("mylockthereturn")
        lock.setReferenceCounted(true)
        lock.acquire()
        try {
            jmdns = JmDNS.create()
            jmdns?.addServiceListener(type, object : ServiceListener {
                override fun serviceResolved(ev: ServiceEvent) {
                    var additions = ""
                    if (ev.info.inetAddresses != null && ev.info.inetAddresses.isNotEmpty()) {
                        additions = ev.info.inetAddresses[0].hostAddress ?: ""
                        notifyUser("Service resolved: ${ev.info.qualifiedName} port: ${ev.info.port} $additions")
                    }
                }

                override fun serviceRemoved(ev: ServiceEvent) {
                    notifyUser("Service removed: " + ev.name)
                    onDeviceLost(ev.name)
                }

                override fun serviceAdded(event: ServiceEvent) {
                    Log.d(TAG, "Service Added: ${event.name} Type: ${event.type}")
                    onDeviceFound(event.name)
                    // Required to force serviceResolved to be called again (after the first search)
                    jmdns?.requestServiceInfo(event.type, event.name, 1)
                }
            })

            val name = "Charly${(0 until 10).random()}"

            serviceInfo = ServiceInfo.create(
                type,
                name,
                0,
                "plain test service from android"
            )
            jmdns?.registerService(serviceInfo)
            Log.d(TAG, "Register as $name")
        } catch (e: Exception) {
            Log.d(TAG, "Exception: ${e.cause}")
        }
    }

    private fun notifyUser(msg: String) {
        Log.d(TAG, msg)
    }


}