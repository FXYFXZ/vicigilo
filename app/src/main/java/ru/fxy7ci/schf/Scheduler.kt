package ru.fxy7ci.schf
// Класс манипуляции с расписанием

class Scheduler() {
    private var listTimers : MutableList<TimerHolder> = mutableListOf(TimerHolder())

    init {
        listTimers.clear()
    }

    fun add (myVal: TimerHolder) {
        listTimers.add(myVal)
        stopWork()
    }

    fun getCurTemp(): Byte{
        return 20
    }


    fun clearList(){
        listTimers.clear()
    }


    fun startWork(){
        if (listTimers.count() == 0) return
        stopWork()
        listTimers[0].downCounter = listTimers[0].timeDecMins.toInt() * 10
    }

    fun stopWork(){
        for (timer in listTimers ) timer.downCounter = 0
    }

    fun decTime(): ScheduleEvents {
        var cntTimers = 0
        for (timer in listTimers ) {
            cntTimers++
            if (timer.downCounter != 0){
                if (--timer.downCounter == 0) {
                    if (cntTimers != listTimers.count()) {
                        // заряжаем следующий отсчет
                        listTimers[cntTimers].downCounter =
                            listTimers[cntTimers].timeDecMins.toInt() * 10
                        return ScheduleEvents.SHDL_FINSTAGE
                    }
                    else
                        return ScheduleEvents.SHDL_FIN_ALL
                }
                return ScheduleEvents.SHDL_NORMAL
            }
        }
        return ScheduleEvents.SHDL_FIN_ALL
    }

    fun getList(): MutableList<TimerHolder>  {
        return listTimers
    }

    //TODO функции записи чтения с диска



    enum class ScheduleEvents {
        SHDL_NORMAL,
        SHDL_FINSTAGE,
        SHDL_FIN_ALL

    }



}