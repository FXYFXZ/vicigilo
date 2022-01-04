package ru.fxy7ci.schf

import android.app.IntentService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.Context
import android.util.Log
import java.util.concurrent.TimeUnit

// TODO: Rename actions, choose action names that describe tasks that this
private const val ACTION_FOO = "ru.fxy7ci.schf.action.FOO"
private const val ACTION_BAZ = "ru.fxy7ci.schf.action.BAZ"

// TODO: Rename parameters
private const val EXTRA_PARAM1 = "ru.fxy7ci.schf.extra.PARAM1"
private const val EXTRA_PARAM2 = "ru.fxy7ci.schf.extra.PARAM2"
const val EXTRA_PARAM3 = "ru.fxy7ci.schf.extra.PARAM3"

class MyIntentService : IntentService("MyIntentService") {

    lateinit var mBluetoothAdapter: BluetoothAdapter
    lateinit var mBluetoothGatt: BluetoothGatt

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getByteExtra(EXTRA_PARAM1,0)
                val param2 = intent.getIntExtra(EXTRA_PARAM2,0)
                val param3 = intent.getIntExtra(EXTRA_PARAM3,-1)
                handleActionFoo(param1, param2, param3)
            }
            ACTION_BAZ -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM1)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }


    override fun onDestroy() {
        Log.d("MyLog", "DEstroed")
        super.onDestroy()
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionFoo (myTemperature: Byte, myMinutes: Int, myPosID: Int) {

/*
        val bluetoothAdapter: BluetoothAdapter by lazy {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }
        mBluetoothAdapter = bluetoothAdapter

        val device: BluetoothDevice? = mBluetoothAdapter.getRemoteDevice(StoreVals.DeviceAddress)
        if (device == null) {
            Log.d("MyLog", "Can't connect")
            return
        }
*/

        TimeUnit.SECONDS.sleep(3)


        //Log.d("MyLog", "Foo goes $param1 $param2 ")

        val responseIntent = Intent()
        responseIntent.action = StoreVals.MAIN_BRD_BLE
        responseIntent.putExtra(EXTRA_PARAM3, myPosID)
        sendBroadcast(responseIntent)

    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String?, param2: String?) {
        TODO("Handle action Baz")
    }

    companion object {
        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionFoo(context: Context, myTemperature: Byte, myMinutes: Int, myPosID: Int) {
            val intent = Intent(context, MyIntentService::class.java).apply {
                action = ACTION_FOO
                putExtra(EXTRA_PARAM1, myTemperature)
                putExtra(EXTRA_PARAM2, myMinutes)
                putExtra(EXTRA_PARAM3, myPosID)
            }
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, MyIntentService::class.java).apply {
                action = ACTION_BAZ
                putExtra(EXTRA_PARAM1, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }
    }
}