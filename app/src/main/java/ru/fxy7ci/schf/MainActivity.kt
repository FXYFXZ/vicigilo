package ru.fxy7ci.schf

import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import java.util.*
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.view.Menu
import android.view.MenuInflater
import android.widget.Toast
import android.content.SharedPreferences





class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdaptor : TimeGridAdapter
    lateinit var sp: SharedPreferences

    companion object {
        const val SETT_NAME = "mySettings"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "channelID"
    }

    // база
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sp = getSharedPreferences(SETT_NAME, Context.MODE_PRIVATE);
        timerAdaptor = TimeGridAdapter(this, scheduler.getList())
        binding.lvTimers.adapter = timerAdaptor
        updateMenu()
        eventsMake()
        registerReceiver(broadcastReceiver,  IntentFilter("INTERNET_LOST")) //TODO rename
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.topmenu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    // Events ---------------------------------------
    private fun eventsMake(){
        binding.btnAdd.setOnClickListener{
            if (binding.edTemperature.text.isNotEmpty() &&
                binding.edTime.text.isNotEmpty()
            ){
                val temperature = binding.edTemperature.text.toString().toInt()
                val timeDecs = binding.edTime.text.toString().toInt()

                if (temperature in  25..100  && timeDecs in 1..255    ) {
                    scheduler.add(TimerHolder(temperature.toByte(),timeDecs))
                    timerAdaptor.notifyDataSetChanged()
                    updateMenu()
                }
                binding.edTemperature.text.clear()
                binding.edTime.text.clear()
                binding.edTemperature.requestFocus()
            }else {
                Toast.makeText(this, resources.getString(R.string.msg_noval), Toast.LENGTH_SHORT).show()
            }
        }

        //todo в одну кнопку
        binding.btnStart.setOnClickListener {
            goStart()
        }
        binding.btnStop.setOnClickListener(){
            stopCook()
        }


        // Recipe change
//        binding.spRecipes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                binding.tvSelected.text = "Selected ${position} ${id}"
//                makeTimers(position)
//                updateMenu()
//            }
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//            }
//        }

    }







    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Alarmilo ekis...
            val nextMins = scheduler.getTimeToEndEtap()
            if (nextMins !=0) startAlarm(nextMins)
            timerAdaptor.notifyDataSetChanged()
            updateMenu()
        }
    }

    override fun onStart() {
        super.onStart()
        timerAdaptor.notifyDataSetChanged()
        updateMenu()
    }

    override fun onPause() {
        saveData()
        super.onPause()
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
        // calendar.add(Calendar.SECOND, myMinutes)
        calendar.add(Calendar.MINUTE, myMinutes)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,  calendar.timeInMillis, pendingIntent)

    }

    // сохраняем всё состояние
    private fun saveData(){
        val lst: MutableSet<String> = MutableList()
        lst.add("RJirf")
        val e = sp.edit()
        e.putStringSet("timers",lst)
        e.apply()
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
            binding.lyAdd.visibility = View.GONE
        }
        else {
            binding.btnStart.visibility =  View.VISIBLE
            binding.btnStop.visibility = View.GONE
            binding.lyAdd.visibility = View.VISIBLE
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

}


