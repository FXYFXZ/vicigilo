package ru.fxy7ci.schf

import android.util.Log
import java.io.IOException
import java.lang.Exception


val scheduler = Scheduler()

// Класс манипуляции с расписанием

class Scheduler() {
    private var listTimers : MutableList<TimerHolder> = mutableListOf(TimerHolder())
    private var curPos: Int = -1

    init {
        listTimers.clear()
    }

    fun getCurPos() = curPos


    fun add (myVal: TimerHolder) {
        if (isOn()) return
        if (listTimers.count() >= 10) return //todo если будет больше, переделать функцию сохранения
        listTimers.add(myVal)
        stopWork()
    }

    fun delete( myPosition:Int) {
        if (isOn()) return
        listTimers.removeAt(myPosition)
    }

    // сколько минут до следущего этапа
    fun getTimeToEndEtap(): Int {
        if (curPos > listTimers.count()-1) {
            curPos = -1
            return 0
        }
        return listTimers[curPos++].timeMins
    }

    fun clearList(){
        listTimers.clear()
        stopWork()
    }

    // export списка для сохранения
    fun getListAsStringSet(): MutableSet<String> {
        val mList : MutableSet<String> = mutableSetOf()
        var itemNum = 0
        for (item in listTimers) {
            val wm = if(itemNum == curPos) "1" else "0" // aktiva
            mList.add("${itemNum++};${item.temperature};${item.timeMins};${wm}")
        }
        return mList
    }

    fun loadFromStringSet(mySet: MutableSet<String>) {
        clearList()
        if (mySet.count()==0) return
        var cPos = 0
        try {
            for (str in mySet.sorted()){ // до 10
                val sArray: List<String> = str.split(";")
                listTimers.add(TimerHolder(sArray[1].toByte(), sArray[2].toInt()))
                if(sArray[3] == "1") curPos = cPos
                cPos++
            }
        } catch (i: Exception){
            clearList()
        }
    }

    fun startWork(): Boolean{
        if (listTimers.count() == 0) return false
        curPos = 0 // стартовая позиция
        return true
    }

    fun stopWork(){
        curPos = -1
    }


    fun getList()= listTimers
    fun isOn() = curPos != -1

    //TODO функции записи чтения с диска

    // загрузка
    // сохранение
    enum class ScheduleEvents {
        SHDL_NORMAL,
        SHDL_FINSTAGE,
        SHDL_FIN_ALL
    }

}