package ru.fxy7ci.schf

// BLE service ============================================================================

import android.app.Service
import android.bluetooth.*
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
    //lateinit var btChar: BluetoothGattCharacteristic
    private var btCharHC: BluetoothGattCharacteristic? = null
    private var btCharHE: BluetoothGattCharacteristic? = null

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
        Log.d("MyLog", "on UNbind")
        return super.onUnbind(intent)
    }

    fun getJob(myJob:TimerHolder) {
        Log.d("MyLog", "got Job" + myJob.temperature)
        if (theJob.timeMins != 0) return // system's busy
        theJob.timeMins = myJob.timeMins
        theJob.temperature = myJob.temperature
        if (connect()) {
            runTask()
        }
    }

    // ===================================================================================MAIN JOB
    private fun runTask(){
        // colontituls
        val STA: Byte = 0x55    // start byte
        val STP = 0xAA.toByte() // stop
        // commands
        val PING: Byte = 0x01
        val MD_MULTI: Byte = 0x02 // set multicook
        val GO: Byte = 0x03       // start cooking
        val FINISH: Byte = 0x04   // finish
        val COOK: Byte = 0x05     // set mode
        val STATUS: Byte = 0x06   // get status
        var seqv: Byte = 2

        Thread {
            // wait until characteristics found
            charReady = false
            for (tmW in 1..200){
                SystemClock.sleep(50)
                if (charReady) break
            }

            if (charReady) {
                Log.d("MyLog", "char found")
                val descriptor =
                    btCharHC!!.getDescriptor(UUID.fromString(StoreVals.CCC_DESCRIPTOR_UUID))
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (mBluetoothGatt.writeDescriptor(descriptor)) Log.d("MyLog", "set descriptor")
                SystemClock.sleep(200)
                val SET_PASS = byteArrayOf(
                    STA, seqv++, 0xFF.toByte(), 0x79, 0xA2.toByte(), 0x9C.toByte(),
                    0x39, 0x38, 0xA2.toByte(), 0x44, 0xD5.toByte(), STP
                )
                btCharHE!!.value = SET_PASS
                if (mBluetoothGatt.writeCharacteristic(btCharHE)) Log.d("MyLog", "sent pass")
                SystemClock.sleep(200)

                // ping ---
                btCharHE!!.value = byteArrayOf(0x55, seqv++, PING, 0xAA.toByte())  // ping
                mBluetoothGatt.writeCharacteristic(btCharHE)
                SystemClock.sleep(500)
                Log.d("MyLog", theJob.timeMins.toString() )
                //.mod(60).toByte()
                if (theJob.temperature >= 35) {
                    // set up cook mode ----
                    val COMMAND = byteArrayOf(
                        STA,
                        seqv++,  // head
                        COOK,
                        MD_MULTI,  //

                        (theJob.temperature / 100).toByte(),
                        theJob.temperature.mod(100).toByte(),  // temperature

                        (theJob.timeMins / 60).toByte(),
                        theJob.timeMins.mod(60).toByte(),  // time BCD

                        0,
                        0,
                        1,  // malsciita
                        STP
                    )
                    // simple run
                    Log.d("MyLog", Arrays.toString(COMMAND) )
                    btCharHE!!.value = COMMAND
                    mBluetoothGatt.writeCharacteristic(btCharHE)
                    SystemClock.sleep(1000)

                    btCharHE!!.value = byteArrayOf(STA, seqv++, GO, STP) // GO
                    mBluetoothGatt.writeCharacteristic(btCharHE)
                }
                else {
                    // stop
                    btCharHE!!.value = byteArrayOf(STA, seqv++, FINISH, STP) // Stop
                    mBluetoothGatt.writeCharacteristic(btCharHE)
                }

                // todo проверяем по статусу
                /*
                val QUERY = byteArrayOf(0x55, 0x01, STATUS, 0xAA.toByte())
                for (I in 0..10) {
                    QUERY[1] = seqv++
                    btCharHE!!.value = QUERY
                    mBluetoothGatt!!.writeCharacteristic(btCharHE)
                    SystemClock.sleep(1000)
                }
                */
                theJob.timeMins = 0

                Log.d("MyLog", "disconnect")
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

    private fun disconnect() {
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
                Log.d("MyLog", Arrays.toString(characteristic.value))
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
           //todo Log.d("MyLog", Arrays.toString(characteristic.value))
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
                if (gattCharacteristic.uuid.toString() == StoreVals.BT_HC) {
                    btCharHC = gattCharacteristic
                    mBluetoothGatt.setCharacteristicNotification(btCharHC, true)
                }
                if (gattCharacteristic.uuid.toString() == StoreVals.BT_HE) {
                    btCharHE = gattCharacteristic
                    mBluetoothGatt.setCharacteristicNotification(btCharHE, true)
                }
            }
        }

        return (btCharHC != null) && (btCharHE != null)
    }






}