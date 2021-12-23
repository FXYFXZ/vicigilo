package ru.fxy7ci.schf

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.widget.Toast
import android.content.SharedPreferences
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import android.widget.ArrayAdapter


class MainActivity : AppCompatActivity() {  // ========================================== MAIN =====
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdapter : TimeGridAdapter
    private lateinit var sp: SharedPreferences
    private var supressRecChange = false
    companion object {
        const val SETT_NAME = "mySettings"
        const val SETT_MAIN_LIST = "mainlist"

        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "channelID"
    }

    // база
    override fun onCreate(savedInstanceState: Bundle?) { // =============================== CREATE
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        sp = getSharedPreferences(SETT_NAME, Context.MODE_PRIVATE)
        timerAdapter = TimeGridAdapter(this, scheduler.getList())
        binding.lvTimers.adapter = timerAdapter
        fillSpinner()
        eventsMake()
        loadData()
        registerReceiver(broadcastReceiver,  IntentFilter("INTERNET_LOST")) //TODO rename
        registerForContextMenu(binding.lvTimers)
        updateMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.topmenu,menu)
        //todo запреты во время работы
        return super.onCreateOptionsMenu(menu)
    }

    // Events ----------------------------------------------------------------------------- EVENTS
    private fun eventsMake(){
        binding.btnAdd.setOnClickListener{
            if (binding.edTemperature.text.isNotEmpty() &&
                binding.edTime.text.isNotEmpty()
            ){
                val temperature = binding.edTemperature.text.toString().toInt()
                val timeDecs = binding.edTime.text.toString().toInt()

                if (temperature in  25..100  && timeDecs in 1..255    ) {
                    scheduler.add(TimerHolder(temperature.toByte(),timeDecs))
                    timerAdapter.notifyDataSetChanged()
                    saveData()
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

        binding.btnStop.setOnClickListener {
            stopCook()
        }

        binding.btnAddRecipe.setOnClickListener{
            saveRecipe()
        }

        binding.btnShowAdd.setOnClickListener{
            binding.lyAdd.visibility = View.VISIBLE
            binding.btnShowAdd.visibility = View.GONE
        }

        // Recipe change
        binding.spRecipes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (supressRecChange) {
                    supressRecChange = false
                }
                else {
                    loadData(binding.spRecipes.adapter.getItem(position).toString())
                    timerAdapter.notifyDataSetChanged()
                    saveData()
                    updateMenu()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuSaveList -> {
                binding.lyNewRecipe.visibility = View.VISIBLE
                binding.edRecipeName.text.clear()
                binding.edRecipeName.requestFocus()
            }
            R.id.clearTimers -> {
                scheduler.clearList()
                timerAdapter.notifyDataSetChanged()
                saveData()
                updateMenu()
            }
            R.id.menuDeleteList -> {
                val adpt = binding.spRecipes.adapter
                if (adpt.count != 0) {
                    val txt = binding.spRecipes.selectedItem.toString()
                    val e = sp.edit()
                    e.remove(txt)
                    e.apply()
                    fillSpinner()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateContextMenu(
        menu: ContextMenu?,
        v: View?,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        if (v?.id == R.id.lvTimers){
//            val info = menuInfo as AdapterContextMenuInfo
//            val position = info.position
            menu?.add(0,0,1,R.string.сm_launch)
            if (!scheduler.isOn()) menu?.add(0,1,1,R.string.cm_delete)
        }
        else
            super.onCreateContextMenu(menu, v, menuInfo)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if  (item.groupId == 0) {
            val info = item.menuInfo as AdapterContextMenuInfo
            val index = info.position
            when (item.itemId) {
                0 -> launchTime(index)
                1 -> deleteTime(index)
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun launchTime(myItemID : Int) {
        Toast.makeText(this, "Launch: $myItemID", Toast.LENGTH_SHORT).show()
    }

    private fun deleteTime(myItemID : Int) {
       scheduler.delete(myItemID)
       timerAdapter.notifyDataSetChanged()
       saveData()
       updateMenu()
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Alarmilo ekis...
            val nextMins = scheduler.getTimeToEndEtap()
            if (nextMins !=0) startAlarm(nextMins)
            timerAdapter.notifyDataSetChanged()
            updateMenu()
        }
    }

    override fun onStart() {
        super.onStart()
        updateMenu()
        binding.lyNewRecipe.visibility = View.GONE
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun startAlarm(myMinutes: Int){
        val mAlarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, MyScheduledReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = System.currentTimeMillis()
        // calendar.add(Calendar.SECOND, myMinutes)
        calendar.add(Calendar.MINUTE, myMinutes)
        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,  calendar.timeInMillis, pendingIntent)
    }

    private fun saveRecipe(){
        if (binding.edRecipeName.length()== 0) return
        val lst  = scheduler.getListAsStringSet()
        val e = sp.edit()
        e.putStringSet(binding.edRecipeName.text.toString(),lst)
        e.apply()
        binding.lyNewRecipe.visibility = View.GONE
    }


    // сохраняем всё состояние
    private fun saveData(){
        val lst  = scheduler.getListAsStringSet()
        val e = sp.edit()
        e.putStringSet(SETT_MAIN_LIST,lst)
        e.apply()
    }

    private fun loadData(){
        sp.getStringSet(SETT_MAIN_LIST, null)?.let {
            scheduler.loadFromStringSet(it)
        }
    }

    private fun loadData(myName: String){
        sp.getStringSet(myName, null)?.let {
            scheduler.loadFromStringSet(it)
        }
    }

    private fun fillSpinner(){
        val mList : MutableSet<String> = mutableSetOf()
        sp.all.forEach{
            if (it.key != SETT_MAIN_LIST) {
                mList.add(it.key)
            }
        }
        val data = mList.toTypedArray()
        val adpt = ArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        binding.spRecipes.adapter = adpt
        supressRecChange = true
    }

    private fun notificateMe(){
        // Создаём уведомление
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_action_cat)
            .setContentTitle("Напоминание")
            .setContentText("Пора переключить")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        builder.setDefaults(Notification.DEFAULT_SOUND)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build()) // посылаем уведомление
        }
    }

    private fun updateMenu(){

        binding.tvStatus.visibility = if (scheduler.isOn()) View.VISIBLE else View.GONE
        binding.spRecipes.visibility = if (!scheduler.isOn()) View.VISIBLE else View.GONE

        if (timerAdapter.count == 0){
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.GONE
            binding.lyAdd.visibility = View.VISIBLE
            return
        }
        else{
            binding.lyAdd.visibility = View.GONE
        }

        if (scheduler.isOn()){
            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
            binding.lyAdd.visibility = View.GONE
        }
        else {
            binding.btnStart.visibility =  View.VISIBLE
            binding.btnStop.visibility = View.GONE
            if (binding.lyAdd.visibility != View.VISIBLE)
                binding.btnShowAdd.visibility = View.VISIBLE

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
        timerAdapter.notifyDataSetChanged()
        updateMenu()
        saveData()
    }

    // Остановка всех процессов
    private fun stopCook(){
        scheduler.stopWork()
        timerAdapter.notifyDataSetChanged()
        updateMenu()
        saveData()
    }

}


