package ru.fxy7ci.schf

class StoreVals {

    companion object {
//        const val DeviceAddress = "88:25:83:F1:1D:07" //MAC @work
        const val DeviceAddress = "18:93:D7:51:75:65" //MAC BLE_FST

        const val APP_PREFERENCES = "myAppSettings"

        const val MAIN_BRD_ALARM = "BroadCastOnAlarm"
        const val MAIN_BRD_BLE = "BroadCastOnBLE"


        // BLE
        const val BT_REQUEST_PERMISSION : Int = 89
        // HC-09 main characteristic
        const val BT_MAIN_CHR = "0000ffe1-0000-1000-8000-00805f9b34fb"

    }

}