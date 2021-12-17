package ru.fxy7ci.schf

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import java.util.*
import android.content.BroadcastReceiver
import android.content.IntentFilter


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdaptor : TimeGridAdapter
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

        binding.btnStart.setOnClickListener {
            goStart()
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


        registerReceiver(broadcastReceiver,  IntentFilter("INTERNET_LOST")) //TODO rename


    }


    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Alarmilo ekis...
            scheduler.stopWork()
            updateMenu()
            // internet lost alert dialog method call from here...



        }
    }

    override fun onStart() {
        super.onStart()
        updateMenu()
//        TODO("Перерасчет оставшегося времени ")
    }

    private fun startAlarm(myMinutes: Int){
        val mAlarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyScheduledReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0,
            intent, PendingIntent.FLAG_ONE_SHOT
        )
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        calendar.add(Calendar.SECOND, myMinutes)
        // TODO calendar.add(Calendar.MINUTE, myMinutes)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,  calendar.timeInMillis, pendingIntent)

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

        if (scheduler.isOn()){
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
        if (scheduler.isOn()) return
        if (!scheduler.startWork()) return
        startAlarm(scheduler.getTimeToEndEtap())
        timerAdaptor.notifyDataSetChanged()
        updateMenu()
    }

    // Остановка всех процессов
    private fun stopCook(){
        scheduler.stopWork()
        timerAdaptor.notifyDataSetChanged()
        updateMenu()
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
                scheduler.add(TimerHolder(52,2))
                scheduler.add(TimerHolder(65,3))
            }

            2->{
                scheduler.add(TimerHolder(52,6))
                scheduler.add(TimerHolder(65,5))
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


