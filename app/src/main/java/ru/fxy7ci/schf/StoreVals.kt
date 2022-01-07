package ru.fxy7ci.schf

class StoreVals {

    companion object {
//        const val DeviceAddress = "88:25:83:F1:1D:07" //MAC @work
//        const val DeviceAddress = "18:93:D7:51:75:65" //MAC BLE_FST
        // Changable settings
        var DeviceAddress: String = "E5:F2:E7:A6:15:24" //MAC RMC
        var useBLE: Boolean = true //MAC RMC
        var useNotification: Boolean = false //MAC RMC

        const val MAIN_BRD_ALARM = "BroadCastOnAlarm"
        const val MAIN_BRD_BLE_OK =  "ru.fxy7ci.schf.BroadCastOnBLE_OK"
        const val MAIN_BRD_BLE_ERR = "ru.fxy7ci.schf.BroadCastOnBLE_ERR"
        const val CODE_MAX_TIMERS = 9

        // BLE
        const val BT_REQUEST_PERMISSION : Int = 89
        // HC-09 main characteristic
        const val BT_MAIN_CHR = "0000ffe1-0000-1000-8000-00805f9b34fb" // HC-08 char

        // RMC BLE characterictic
        const val BT_HC = "6e400003-b5a3-f393-e0a9-e50e24dcca9e" // session
        const val BT_HE = "6e400002-b5a3-f393-e0a9-e50e24dcca9e" // command
        const val CCC_DESCRIPTOR_UUID = "00002902-0000-1000-8000-00805f9b34fb"

        // informi pri
    }

    enum class COOK_NOTIFICATION {
        COOK_END,
        COOK_BLE_ERRO,
        COOK_ETAP_NEXT
    }

}

