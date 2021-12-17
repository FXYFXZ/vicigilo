package ru.fxy7ci.schf

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ProgressBar
import android.widget.TextView

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
        //TODO("Not yet implemented")
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val newView : View
        if  (convertView == null) {
            newView = inFlater.inflate(R.layout.timerrow, null)
        }
        else newView = convertView
        val ordNum = newView.findViewById<View>(R.id.ordnum ) as TextView
          ordNum.text = (position+1).toString()

        val temperature = newView.findViewById<View>(R.id.tvTemperature) as TextView
        temperature.text = timerList[position].temperature.toString() + "º"

        val timeSet = newView.findViewById<View>(R.id.tvTimeSet) as TextView
        val minText = context.getResources().getString(R.string.txt_minutes).format(timerList[position].timeDecMins*10)
        timeSet.text = minText

        val progress = newView.findViewById<View>(R.id.progressBar) as ProgressBar
//        if (timerList[position].downCounter != 0) {
//            progress.visibility = View.VISIBLE
//            progress.progress = (timerList[position].downCounter*100)/(timerList[position].timeDecMins*10)
//        }
//        else
//            progress.visibility = View.GONE
        return newView

    }

}