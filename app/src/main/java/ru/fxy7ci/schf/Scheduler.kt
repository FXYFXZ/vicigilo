package ru.fxy7ci.schf

val scheduler = Scheduler()

// Класс манипуляции с расписанием

class Scheduler() {
    private var listTimers : MutableList<TimerHolder> = mutableListOf(TimerHolder())
    private var curPos: Int = -1

    init {
        listTimers.clear()
    }


    fun onAlarm() {
        curPos++

    }

    fun add (myVal: TimerHolder) {
        if (isOn()) return
        listTimers.add(myVal)
        stopWork()
    }

    fun getTimeToEndEtap(): Int {
        return listTimers[curPos].timeDecMins * 10
    }

    fun clearList(){
        listTimers.clear()
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