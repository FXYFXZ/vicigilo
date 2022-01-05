package ru.fxy7ci.schf

import android.app.IntentService
import android.bluetooth.*
import android.content.Intent
import android.content.Context
import android.os.SystemClock
import android.util.Log
import java.util.*

private const val ACTION_FOO = "ru.fxy7ci.schf.action.FOO"
private const val EXTRA_PARAM1 = "ru.fxy7ci.schf.extra.PARAM1"
private const val EXTRA_PARAM2 = "ru.fxy7ci.schf.extra.PARAM2"
const val BLE_PROC_PARAM_ID = "ru.fxy7ci.schf.extra.PARAM3"

// konstatnoj
// colontituls
private const val STA: Byte = 0x55    // start byte
private const val STP = 0xAA.toByte() // stop
// commands
private const val PING: Byte = 0x01
private const val MD_MULTI: Byte = 0x02 // set multicook
private const val GO: Byte = 0x03       // start cooking
private const val FINISH: Byte = 0x04   // finish
private const val COOK: Byte = 0x05     // set mode
private const val STATUS: Byte = 0x06   // get status

class MyIntentService : IntentService("MyIntentService") {

    lateinit var mBluetoothAdapter: BluetoothAdapter
    lateinit var mBluetoothGatt: BluetoothGatt
    private var btCharHC: BluetoothGattCharacteristic? = null
    private var btCharHE: BluetoothGattCharacteristic? = null
    private var callBackFlag = false
    private var charReady = false

    private var seqv: Byte = 2

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getByteExtra(EXTRA_PARAM1,0)
                val param2 = intent.getIntExtra(EXTRA_PARAM2,0)
                val param3 = intent.getIntExtra(BLE_PROC_PARAM_ID,-1)
                setJob(param1, param2, param3)
            }
        }
    }

    override fun onDestroy() {
        Log.d("MyLog", "DEstroed")
        super.onDestroy()
    }

    private fun setJob (myTemperature: Byte, myMinutes: Int, myPosID: Int) {
        val bluetoothAdapter: BluetoothAdapter by lazy {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothManager.adapter
        }
        mBluetoothAdapter = bluetoothAdapter
        val device: BluetoothDevice? = mBluetoothAdapter.getRemoteDevice(StoreVals.DeviceAddress)
        if (device == null) {
            informMain(false, myPosID)
            return
        }
        Log.d("MyLog", "try coonect")

        // get characteristic
        charReady = false
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback)
        for (tmW in 1..200){
            SystemClock.sleep(50)
            if (charReady) break
        }
        if (!charReady) {
            informMain(false, myPosID)
            return
        }
        // Char estas pretaj ------- PRETAJ
        val descriptor =
            btCharHC!!.getDescriptor(UUID.fromString(StoreVals.CCC_DESCRIPTOR_UUID))
        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
        callBackFlag = false
        mBluetoothGatt.writeDescriptor(descriptor)
        if (waitCallBackFlag()) {
            val SET_PASS = byteArrayOf(
                STA, seqv++, 0xFF.toByte(), 0x79, 0xA2.toByte(), 0x9C.toByte(),
                0x39, 0x38, 0xA2.toByte(), 0x44, 0xD5.toByte(), STP
            )
            btCharHE!!.value = SET_PASS
            mBluetoothGatt.writeCharacteristic(btCharHE)
            if (waitCallBackFlag()){
                btCharHE!!.value = byteArrayOf(0x55, seqv++, PING, 0xAA.toByte())  // ping
                mBluetoothGatt.writeCharacteristic(btCharHE)
                if (waitCallBackFlag()){
                    if (myTemperature >= 35) {
                        // set up cook mode ----
                        val COMMAND = byteArrayOf(
                            STA,
                            seqv++,  // head
                            COOK,
                            MD_MULTI,  //

                            (myTemperature / 100).toByte(),
                            myTemperature.mod(100).toByte(),  // temperature

                            (myMinutes / 60).toByte(),
                            myMinutes.mod(60).toByte(),  // time BCD

                            0,
                            0,
                            1,  // malsciita
                            STP
                        )

                        btCharHE!!.value = COMMAND
                        mBluetoothGatt.writeCharacteristic(btCharHE)
                        waitCallBackFlag()
                        btCharHE!!.value = byteArrayOf(STA, seqv++, GO, STP) // GO
                        mBluetoothGatt.writeCharacteristic(btCharHE)
                        waitCallBackFlag()
                    }
                    else {// stop
                        btCharHE!!.value = byteArrayOf(STA, seqv++, FINISH, STP) // Stop
                        mBluetoothGatt.writeCharacteristic(btCharHE)
                        waitCallBackFlag()
                    }
                    //todo проверка по статусу
//                            val QUERY = byteArrayOf(0x55, 0x01, STATUS, 0xAA.toByte())
//                            for (I in 0..10) {
//                                QUERY[1] = seqv++
//                                btCharHE!!.value = QUERY
//                                mBluetoothGatt!!.writeCharacteristic(btCharHE)
//                                SystemClock.sleep(1000)
//                            }
                    informMain(true, myPosID)
                }
            }
        }
        mBluetoothGatt.disconnect()
    }

    private fun waitCallBackFlag() : Boolean{
        for (tmW in 1..200){
            SystemClock.sleep(50)
            if (callBackFlag) {
                callBackFlag = false // por sekva fojo
                return true
            }
        }
        return false
    }

    // Implements callback methods for GATT events that the app cares about.
    // For example, connection change and services discovered.
    private val mGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mBluetoothGatt.discoverServices() // ===  что у нас есть? ===\
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS )  callBackFlag = true
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) callBackFlag = true
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




    private fun informMain(isSuccess: Boolean, curPos: Int){
        val responseIntent = Intent()
        if (isSuccess)
            responseIntent.action = StoreVals.MAIN_BRD_BLE_OK
        else
            responseIntent.action = StoreVals.MAIN_BRD_BLE_ERR
        responseIntent.putExtra(BLE_PROC_PARAM_ID, curPos)
        sendBroadcast(responseIntent)

    }



    // helper to start service ---------------------------------------------------------------------
    companion object {
        @JvmStatic
        fun setServJob(context: Context, myTemperature: Byte, myMinutes: Int, myPosID: Int) {
            val intent = Intent(context, MyIntentService::class.java).apply {
                action = ACTION_FOO
                putExtra(EXTRA_PARAM1, myTemperature)
                putExtra(EXTRA_PARAM2, myMinutes)
                putExtra(BLE_PROC_PARAM_ID, myPosID)
            }
            context.startService(intent)
        }
    }
} // CLASS END