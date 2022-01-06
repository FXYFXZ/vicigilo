package ru.fxy7ci.schf

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

//
class TimerHolder (
    var temperature: Byte = 30, // temperature set if 0 -> turn device off
    var timeMins: Int = 10, // time for etap
    var isMissed: Boolean = false)

// Адаптор для добавления, рисования и работы с таймерами
class TimeGridAdapter (var context: Context, var timerList: List<TimerHolder>) : BaseAdapter() {
    var inFlater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return timerList.count()
    }

    override fun getItem(position: Int): Any {
        return timerList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val newView: View
        if (convertView == null) {
            newView = inFlater.inflate(R.layout.timerrow, null)
        } else newView = convertView
        val ordNum = newView.findViewById<View>(R.id.ordnum) as TextView
        ordNum.text = (position + 1).toString()

        val temperature = newView.findViewById<View>(R.id.tvTemperature) as TextView
        temperature.text = timerList[position].temperature.toString() + "º"

        val timeSet = newView.findViewById<View>(R.id.tvTimeSet) as TextView
        val minText = context.getResources().getString(R.string.txt_minutes)
            .format(timerList[position].timeMins)
        timeSet.text = minText

        val progress = newView.findViewById<View>(R.id.progressBar) as ProgressBar
        val imgErr = newView.findViewById<View>(R.id.imgErr) as ImageView

        if (scheduler.isOn() && scheduler.getCurPos() == position) {
            progress.visibility = View.VISIBLE
        } else {
            progress.visibility = View.GONE
        }
        imgErr.visibility =  if (timerList[position].isMissed) View.VISIBLE else View.GONE
        return newView

    }

}