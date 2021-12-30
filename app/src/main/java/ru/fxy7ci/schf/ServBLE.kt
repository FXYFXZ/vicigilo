package ru.fxy7ci.schf

// Служба связи по BLE

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import java.util.*


class ServBLE : Service() {
    private var theJob = TimerHolder(20,0)
    var mBinder = MyBinder()


//    lateinit var mBluetoothManager: BluetoothManager
    lateinit var mBluetoothAdapter: BluetoothAdapter

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

    fun getJob(myJob:TimerHolder){
        if(theJob.timeMins != 0 ) return // system's busy
        theJob = myJob
        runTask()
        Log.d("MyLog", "got job")
    }


    fun initAdapter(myAdapter: BluetoothAdapter){
        mBluetoothAdapter = myAdapter
    }

    // ===================================================================================MAIN JOB
    private fun runTask(){
        Thread {
            Log.d("MyLog", "соединение")
            SystemClock.sleep(1000)
            Log.d("MyLog", "передача")
            SystemClock.sleep(1000)
            theJob.timeMins = 0
            Log.d("MyLog", "EK")

        }.start()
    }



}