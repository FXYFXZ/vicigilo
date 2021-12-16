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
        listTimers.add(myVal)
        stopWork()
    }

    fun getTimeToEndEtap(): Int {
        return listTimers[curPos].timeDecMins * 1 //todo  * 10
    }

    fun clearList(){
        listTimers.clear()
    }


    fun startWork(){
        if (listTimers.count() == 0) return
        stopWork()
        //listTimers[0].downCounter = listTimers[0].timeDecMins.toInt() * 10
    }

    fun stopWork(){
        for (timer in listTimers ) timer.downCounter = 0
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