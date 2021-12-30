package ru.fxy7ci.schf

// Служба связи по BLE

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.*


class ServBLE : Service() {
    private var theJob = TimerHolder(20,0)
    var mBinder = MyBinder()

    lateinit var mBluetoothAdapter: BluetoothAdapter
    lateinit var mBluetoothGatt: BluetoothGatt
    private var charReady = false
    lateinit var btChar: BluetoothGattCharacteristic

    inner class MyBinder : Binder() {
        fun getService() : ServBLE {
            return this@ServBLE
        }
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d("MyLog", "on bind")
        return mBinder

//        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("MyLog", "on UN bind")
        return super.onUnbind(intent)
    }

    fun getJob(myJob:TimerHolder) {
        if (theJob.timeMins != 0) return // system's busy
        theJob = myJob
        if (connect()) {
            runTask()
        }
    }

    // ===================================================================================MAIN JOB
    private fun runTask(){
        Thread {
            SystemClock.sleep(5000)
            if (charReady) {


                Log.d("MyLog", "передача")
                SystemClock.sleep(1000)


                Log.d("MyLog", "рассоединение")
                disconnect()

                SystemClock.sleep(1000)

            }

            theJob.timeMins = 0
            Log.d("MyLog", "EK")

        }.start()
    }


    private fun connect(): Boolean {
        val device: BluetoothDevice? = mBluetoothAdapter.getRemoteDevice(StoreVals.DeviceAddress)
        if (device == null) {
            Log.d("MyLog", "Can't connect")
            return false
        }
        Log.d("MyLog", device.toString())
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        return true
    }

    fun disconnect() {
        mBluetoothGatt.disconnect()
        mBluetoothGatt.close()
    }
    // Implements callback methods for GATT events that the app cares about.
    // For example, connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices() // ===  что у нас есть? ===\
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                charReady = locateCharacteristic()
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {

        }
    }

    // копаемся в службах и соединяемся с нужной характеристикой
    private fun locateCharacteristic() : Boolean {
        val gattServices = mBluetoothGatt.services ?: return false
        for (gattService in gattServices) {
//          Log.d("MyLog", "Service:" + gattService.toString());
            // типа "android.bluetooth.BluetoothGattService@e81fae4"
            val gattCharacteristics = gattService.characteristics
            for (gattCharacteristic in gattCharacteristics) {
//                Log.d("MyLog", "Char:" + gattCharacteristic.getUuid().toString());
                if (gattCharacteristic.uuid.toString() == StoreVals.BT_MAIN_CHR) {
                    btChar = gattCharacteristic
                    return true
                }
            }
        }
        return false // искали, ничего нашли
    }

}