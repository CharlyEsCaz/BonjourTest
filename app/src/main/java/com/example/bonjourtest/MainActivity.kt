package com.example.bonjourtest

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.databinding.DataBindingUtil
import com.example.bonjourtest.adapter.BonjourAdapter
import com.example.bonjourtest.databinding.ActivityMainBinding
import com.example.bonjourtest.models.Device
import java.net.InetAddress
import java.net.ServerSocket


class MainActivity : AppCompatActivity() {

    private lateinit var vBind: ActivityMainBinding
    private var devices = ArrayList<Device>()
    private var adapter: BonjourAdapter? = null
    private lateinit var bonjour: BonjourSettings

    private var mServiceName = ""
    private var nsdManager: NsdManager? = null
    private var mService: NsdServiceInfo? = null
    private val TAG = "BONJOUR_CONNECTION"
    private var SERVICE_TYPE = "_offline_test._tcp."
    private var serverSocket: ServerSocket? = null
    private var mLocalPort = 0

    private lateinit var multicastLock: WifiManager.MulticastLock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vBind = DataBindingUtil.setContentView(this, R.layout.activity_main)
        multicastLock =
        (applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock(
            "mylock"
        )
        setupAdapter()
        initializeServerSocket()
    }


    private fun initializeServerSocket() {

        multicastLock.setReferenceCounted(true)
        multicastLock.acquire()

        serverSocket = ServerSocket(0).also { socket ->
            mLocalPort = socket.localPort
            Log.d(TAG, "Port: $mLocalPort")
            registerService(mLocalPort)
            nsdManager?.discoverServices(
                SERVICE_TYPE,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        }
    }


    private fun registerService(port: Int) {

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = "Juan"
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        nsdManager = (getSystemService(Context.NSD_SERVICE) as NsdManager).apply {
            registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {

        override fun onDiscoveryStarted(regType: String) {
            Log.d(TAG, "Service discovery started")
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            onDeviceFound(service.serviceName)
            Log.d(TAG, "Service Found: ${service.serviceName}")
//            when {
//                service.serviceType != SERVICE_TYPE ->
//                    Log.d(TAG, "Unknown Service Type: ${service.serviceType}")
//                service.serviceName != mServiceName -> {
//
//                    Log.d(TAG, "Same machine: $mServiceName")
//                }
//                service.serviceName.contains("NsdChat") -> nsdManager?.resolveService(
//                    service,
//                    resolveListener
//                )
//            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            Log.e(TAG, "service lost: $service")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.i(TAG, "Discovery stopped: $serviceType")
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
            multicastLock.release()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Discovery failed: Error code:$errorCode")
            nsdManager?.stopServiceDiscovery(this)
            multicastLock.release()
        }
    }

    private val registrationListener = object : NsdManager.RegistrationListener {

        override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
            mServiceName = NsdServiceInfo.serviceName
            Log.d(TAG, "Service registration success")
        }

        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.d(TAG, "Service registration failed: $errorCode")
        }

        override fun onServiceUnregistered(arg0: NsdServiceInfo) {

        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {

        }
    }

    private val resolveListener = object : NsdManager.ResolveListener {

        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {

            Log.e(TAG, "Resolve failed: $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(TAG, "Resolve Succeeded. $serviceInfo")

            if (serviceInfo.serviceName == mServiceName) {
                Log.d(TAG, "Same IP.")
                return
            }
            mService = serviceInfo
            val port: Int = serviceInfo.port
            val host: InetAddress = serviceInfo.host
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

    fun tearDown() {
        nsdManager?.apply {
            unregisterService(registrationListener)
            stopServiceDiscovery(discoveryListener)
            multicastLock.release()
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


}