package com.example.bluetoothdemo

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.*
import android.service.controls.Control
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothdemo.databinding.ActivityControlBinding
import org.jetbrains.anko.toast
import java.io.IOException
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.*

class ControlActivity: AppCompatActivity() {

    private lateinit var controlBinding: ActivityControlBinding

    companion object {
        //        var m_myUUID: UUID = UUID.fromString("00001106-0000-1000-8000-00805f9b34fb")
        var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        var m_bluetoothSocket: BluetoothSocket? = null
        lateinit var m_progress: ProgressDialog
        lateinit var m_bluetoothAdapter: BluetoothAdapter
        var m_isConnected: Boolean = false
        lateinit var m_address: String
        private lateinit var mBluetoothService: MyBluetoothService.ConnectedThread
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controlBinding = ActivityControlBinding.inflate(layoutInflater)
        setContentView(controlBinding.root)
        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS).toString()


        val handler = object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: Message) {
                val data: ByteArray = msg.obj as ByteArray
                val message: String = String(data)
                toast(message)
            }
        }


        val listener: BluetoothResult = object: BluetoothResult {
            override fun processFinish() {
                if(m_bluetoothSocket != null){
                    mBluetoothService = MyBluetoothService(handler).ConnectedThread(m_bluetoothSocket!!)
                    mBluetoothService.start()
                }
            }
        }

        ConnectToDevice(this, listener).execute()

        controlBinding.buttonSendCommand.setOnClickListener {
//            sendCommand("Hello World")
            mBluetoothService.write("hello JMS".toByteArray())

        }
        controlBinding.buttonDisconnect.setOnClickListener { disconnect() }

        controlBinding.buttonReceive.setOnClickListener {
            if(m_bluetoothSocket != null){
                mBluetoothService.run()
                toast("Data receive started")
            }
        }

    }

    private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                toast(e.toString())
            }
        }
        finish()
    }

    private class ConnectToDevice(context: Context, listener: BluetoothResult) : AsyncTask<Void, Void, String>() {
        private var listener: BluetoothResult
        private var connectSuccess: Boolean = true
        private val context: Context


        init {
            this.context = context
            this.listener = listener
        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context, "Connecting...", "wait")
        }

        override fun doInBackground(vararg params: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }

            } catch (e: Exception) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                Log.i("data", "Could not connect")
            } else {
                m_isConnected = true
            }
            listener.processFinish()
            m_progress.dismiss()
        }
    }

//    // Declare the Handler as a static class.
//    class MyHandler(private val outerClass: WeakReference<ControlActivity>) : Handler() {
//
//        override fun handleMessage(msg: Message?) {
//            // Your logic code here.
//            // ...
//            if (msg == null){
//                return
//            }else{
//                Toast.makeText(this, "hello world", Toast.LENGTH_SHORT).show();
//            }
//
//            // Make all references to members of the outer class
//            // using the WeakReference object.
////            outerClass.get()?.outerVariable
////            outerClass.get()?.outerMethod()
//        }
//    }
}