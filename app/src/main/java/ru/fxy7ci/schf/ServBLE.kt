package ru.fxy7ci.schf

// Служба связи по BLE

import android.app.Service
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.*


class TheJob (
    var temperature: Byte = 20,  // заданная температура
    var timeMins: Int = 0)



class ServBLE : Service() {
    var mBinder = MyBinder()
    private var theJob = TheJob()
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
//        Log.d("MyLog", "on bind")
        return mBinder

//        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("MyLog", "on UN bind")
        return super.onUnbind(intent)
    }

    fun getJob(myJob:TimerHolder) {
        if (theJob.timeMins != 0) return // system's busy
        theJob.timeMins = myJob.timeMins
        theJob.temperature = myJob.temperature
        if (connect()) {
            runTask()
        }
        //todo анализ флагов выполнения
        theJob.timeMins = 0
    }

    // ===================================================================================MAIN JOB
    private fun runTask(){
        Thread {
            // ждём подключения к характеристикам
            charReady = false
            for (tmW in 1..200){
                SystemClock.sleep(50)
                if (charReady) break
            }

            if (charReady) {

                Log.d("MyLog", "передача " + theJob.temperature)
                sendChar()
                SystemClock.sleep(3000)

                Log.d("MyLog", "рассоединение")
                disconnect()
            }

            Log.d("MyLog", "Stop Thread")
        }.start()
    }

    private fun connect(): Boolean {
        val device: BluetoothDevice? = mBluetoothAdapter.getRemoteDevice(StoreVals.DeviceAddress)
        if (device == null) {
            Log.d("MyLog", "Can't connect")
            return false
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        return true
    }

    fun disconnect() {
        mBluetoothGatt.disconnect()
        //mBluetoothGatt.close()
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


    // времянка - потом меняем на RMC
    private fun sendChar() {
        val value = ByteArray(6)
        value[0] = 3
        value[4] = 0xAB.toByte()
        value[5] = 0xBA.toByte()

        when(theJob.temperature) {
            25.toByte() -> {
                value[1] = 128.toByte()
                value[2] = 255.toByte()
                value[3] = 128.toByte()
            }

            26.toByte() -> {
                value[1] = 255.toByte()
                value[2] = 255.toByte()
                value[3] = 255.toByte()
            }

            27.toByte() -> {
                value[1] = 128.toByte()
                value[2] = 128.toByte()
                value[3] = 255.toByte()
            }

            28.toByte() -> {
                value[1] = 128.toByte()
                value[2] = 255.toByte()
                value[3] = 255.toByte()
            }

            else -> {
                value[1] = 0.toByte()
                value[2] = 0.toByte()
                value[3] = 0.toByte()
            }
        }

        btChar.value = value
        mBluetoothGatt.writeCharacteristic(btChar)
    }








}