package ru.fxy7ci.schf

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdaptor : TimeGridAdapter
    val scheduler = Scheduler()
    private lateinit var mHandler: Handler
    private var theTimerGoesOn = false
    lateinit var  mPendingIntent: PendingIntent
    companion object {
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "channelID"
    }


    // база
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        makeSpinner()

        timerAdaptor = TimeGridAdapter(this, scheduler.getList())
        binding.lvTimers.adapter = timerAdaptor

        updateMenu()
        // События

        binding.btnStart.setOnClickListener(){
            //goStart()
            startAlarm()
        }

        // on Change
        binding.spCity.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                binding.tvSelected.text = "Selected ${position} ${id}"
                makeTimers(position)
                updateMenu()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        binding.btnStop.setOnClickListener(){
            stopCook()
        }

    }

    private fun startAlarm(){
        val mAlarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, MyScheduledReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            intent, PendingIntent.FLAG_ONE_SHOT
        )
        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                    + 10 * 1000, pendingIntent
        )


 /*
        val myIntent = Intent(
            this@MainActivity,
            ServiceClock::class.java
        )

        mPendingIntent = PendingIntent.getService(
            this@MainActivity, 0,
            myIntent, 0
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.SECOND, 10)

        alarmManager[AlarmManager.RTC_WAKEUP, calendar.timeInMillis] = mPendingIntent

        Toast.makeText(
            this@MainActivity, "Устанавливаем сигнализацию",
            Toast.LENGTH_LONG
        ).show()
*/
    }




    private fun notificateMe(){
        // Создаём уведомление
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_cat)
            .setContentTitle("Напоминание")
            .setContentText("Пора покормить кота")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        builder.setDefaults(Notification.DEFAULT_SOUND)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build()) // посылаем уведомление
        }
    }



    private fun updateMenu(){

        if (timerAdaptor.count == 0){
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            return
        }

        if (theTimerGoesOn){
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        }
        else {
            binding.btnStart.visibility =  View.VISIBLE
            binding.btnStop.visibility = View.GONE
        }
    }



    override fun onDestroy() {
        // убираем все процессы, освобождаем ресурсы
        stopCook()
        super.onDestroy()
    }

    private fun goStart() {
        if (theTimerGoesOn) return
        scheduler.startWork()
        timerAdaptor.notifyDataSetChanged()

        // заводим поток
        mHandler = Handler(Looper.getMainLooper())
        mHandler.post(updateTextTask)
        theTimerGoesOn = true
        updateMenu()
    }

    // Остановка всех процессов
    private fun stopCook(){
        mHandler.removeCallbacks(updateTextTask)
        scheduler.stopWork()
        timerAdaptor.notifyDataSetChanged()
        theTimerGoesOn = false
        updateMenu()
    }

    private val updateTextTask = object : Runnable {
        override fun run() {
            decInterval()
            mHandler.postDelayed(this, 1000)
        }
    }

    private fun decInterval () {
        when (scheduler.decTime()) {
            Scheduler.ScheduleEvents.SHDL_NORMAL -> timerAdaptor.notifyDataSetChanged()
            Scheduler.ScheduleEvents.SHDL_FINSTAGE -> {
                notificateMe()
                timerAdaptor.notifyDataSetChanged()
            }
            Scheduler.ScheduleEvents.SHDL_FIN_ALL -> {
                stopCook() //
                timerAdaptor.notifyDataSetChanged()
            }

        }

        Log.d("MyLog", "dec")
    }


    // Test procedure
    private fun makeTimers(myPosition : Int){
        scheduler.stopWork()
        scheduler.clearList()

        when(myPosition){
            0->{
                scheduler.add(TimerHolder(52,1))
            }

            1->{
                scheduler.add(TimerHolder(52,1))
                scheduler.add(TimerHolder(65,2))
            }

            2->{
                scheduler.add(TimerHolder(52,1))
                scheduler.add(TimerHolder(65,2))
                scheduler.add(TimerHolder(72,3))
                scheduler.add(TimerHolder(82,4))
            }
        }

        timerAdaptor.notifyDataSetChanged()


//        timerAdaptor = TimeGridAdapter(this, scheduler.getList())
//        binding.lvTimers.adapter = timerAdaptor
    }

    // комбобокс
    private fun makeSpinner() {
        //val cities = resources.getStringArray(R.array.arCities)
        val lstWeek = Arrays.asList("Пшеница", "Гречка", "Чечевица")

        val adapter = CustomAdapter(this, lstWeek)

        binding.spCity.adapter = adapter
            //binding.spCity.setPromptId(ЭЭ)
//        binding.spCity.setSelection(2, true)

    }

} // CLASS


class CustomAdapter(var context: Context, var countryNames: List<String>) :
    BaseAdapter() {
    var inFlater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int {
        return countryNames.size
    }

    override fun getItem(i: Int): Any {
        return countryNames[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    override fun getView(i: Int, view: View?, viewGroup: ViewGroup): View {
        val newView : View
        if  (view == null) {
            newView = inFlater.inflate(R.layout.boxrow, null)
        }
        else newView = view
        val icon = newView.findViewById<View>(R.id.icon) as ImageView
        val names = newView.findViewById<View>(R.id.town) as TextView
        if (i==1)
            icon.setImageResource(R.drawable.ic_launcher_foreground)
        else
            icon.setImageResource(R.drawable.ic_city)
        names.text = countryNames[i]
        return newView
    }
}