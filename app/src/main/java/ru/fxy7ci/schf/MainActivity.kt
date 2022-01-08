package ru.fxy7ci.schf

import android.Manifest
import android.app.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager

//todo странные мелькания

class MainActivity : AppCompatActivity() {  // ========================================== MAIN =====
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdapter : TimeGridAdapter
    private lateinit var sp: SharedPreferences
    // Alarmo
    private lateinit var mAlarmManager : AlarmManager
    private lateinit var mIntent: Intent
    private lateinit var mPendingIntent: PendingIntent

    private lateinit var finTime: Calendar // время завершения процесса

    companion object {
        const val SETT_NAME = "mySettings"
        const val SETT_MAIN_LIST = "mainlist"
    }

    override fun onCreate(savedInstanceState: Bundle?) { // =============================== CREATE
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MyLog", "create")

        finTime = Calendar.getInstance()

        getPermissions()

        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mIntent = Intent(this, MyBroadcastReceiver::class.java)

        sp = getSharedPreferences(SETT_NAME, Context.MODE_PRIVATE)
        timerAdapter = TimeGridAdapter(this, scheduler.getList())
        binding.lvTimers.adapter = timerAdapter
        fillSpinner()
        eventsMake()
        stateLoad()

        val myIntentFilter = IntentFilter()
        myIntentFilter.addAction(StoreVals.MAIN_BRD_ALARM)
        myIntentFilter.addAction(StoreVals.MAIN_BRD_BLE_OK)
        myIntentFilter.addAction(StoreVals.MAIN_BRD_BLE_ERR)
        registerReceiver(broadcastReceiver,  myIntentFilter)

        registerForContextMenu(binding.lvTimers)
        updateMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.topmenu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item = menu!!.findItem(R.id.clearTimers)
        item.isVisible = !scheduler.isOn()
        val item2 = menu!!.findItem(R.id.settings)
        item2.isVisible = !scheduler.isOn()

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onStart() {  // START
        super.onStart()
        updateMenu()
        timerAdapter.notifyDataSetChanged()
        binding.lyNewRecipe.visibility = View.GONE
    }

    // Events ----------------------------------------------------------------------------- EVENTS
    private fun eventsMake(){
        binding.btnAdd.setOnClickListener{
            if (binding.edTemperature.text.isNotEmpty() &&
                binding.edTime.text.isNotEmpty()
            ){
                val temperature = binding.edTemperature.text.toString().toInt()
                val timeMinutes = binding.edTime.text.toString().toInt()

                if (temperature in  30..100  && timeMinutes in 1..500) {
                    scheduler.add(TimerHolder(temperature.toByte(),timeMinutes))
                    binding.spRecipes.setSelection(0)
                    timerAdapter.notifyDataSetChanged()
                    stateSave()
                    updateMenu()
                }
                binding.edTemperature.text.clear()
                binding.edTime.text.clear()
                binding.edTemperature.requestFocus()
            }else {
                Toast.makeText(this, resources.getString(R.string.msg_noval), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnStart.setOnClickListener {
            goStart()
        }

        binding.btnStop.setOnClickListener {
            stopCook()
        }

        binding.btnAddRecipe.setOnClickListener{
            saveRecipe()
        }

        // Recipe change
        binding.spRecipes.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != 0) { // top is empty
                    stateLoad(binding.spRecipes.adapter.getItem(position).toString())
                    timerAdapter.notifyDataSetChanged()
                    stateSave()
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
                binding.spRecipes.visibility = View.GONE
                binding.edRecipeName.requestFocus()
            }
            R.id.clearTimers -> {
                scheduler.clearList()
                binding.spRecipes.setSelection(0)
                timerAdapter.notifyDataSetChanged()
                stateSave()
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
            R.id.settings -> {
                goSettings()
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

    private fun goSettings(){
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    private fun goStart() {
        if (scheduler.isOn()) return
        if (!scheduler.startWork()) return
        finTime = Calendar.getInstance()
        finTime.add(Calendar.MINUTE, scheduler.getMinutesTillEnd())
        setNewAlarm()
    }

    private fun setNewAlarm() {
        if (scheduler.isOn()) {
            // есть время...
            val curPos = scheduler.getCurPos()
            val curTM = timerAdapter.getItem(curPos) as TimerHolder
            MyIntentService.setServJob(this, curTM.temperature, curTM.timeMins, curPos)
            startAlarm(curTM.timeMins)
        }
        timerAdapter.notifyDataSetChanged()
        updateMenu()
        stateSave()
    }

    // Остановка всех процессов
    private fun stopCook(){
        scheduler.stopWork()
        timerAdapter.notifyDataSetChanged()
        updateMenu()
        stateSave()
        mAlarmManager.cancel(mPendingIntent)
    }

    private fun launchTime(myItemID : Int) {
        val tmr = timerAdapter.getItem(myItemID) as TimerHolder
        MyIntentService.setServJob(this, tmr.temperature, tmr.timeMins, myItemID)
    }

    private fun deleteTime(myItemID : Int) {
       scheduler.delete(myItemID)
       timerAdapter.notifyDataSetChanged()
       binding.spRecipes.setSelection(0)
       stateSave()
       updateMenu()
    }

    //
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                StoreVals.MAIN_BRD_ALARM -> {
                    scheduler.advance()
                    if (scheduler.isOn()) {
                        setNewAlarm()
                        informiPri(StoreVals.COOK_NOTIFICATION.COOK_ETAP_NEXT)
                    }
                    else {
                        timerAdapter.notifyDataSetChanged()
                        updateMenu()
                        stateSave()
                        informiPri(StoreVals.COOK_NOTIFICATION.COOK_END)
                    }
                }
                StoreVals.MAIN_BRD_BLE_OK, StoreVals.MAIN_BRD_BLE_ERR  -> {
                    val possID = intent.getIntExtra(BLE_PROC_PARAM_ID,-1)
                    val timer = timerAdapter.getItem(possID)  as TimerHolder
                    if (intent.action == StoreVals.MAIN_BRD_BLE_ERR) {
                        timer.isMissed = true
                        informiPri(StoreVals.COOK_NOTIFICATION.COOK_BLE_ERRO)
                    }
                    else {
                        timer.isMissed = false
                    }
                    timerAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun startAlarm(myMinutes: Int){
        val calendar = Calendar.getInstance()
        val reqCode = calendar.get(Calendar.MINUTE) * 100 + calendar.get(Calendar.SECOND)

        mPendingIntent = PendingIntent.getBroadcast(
            this, reqCode, mIntent, PendingIntent.FLAG_IMMUTABLE
                    or PendingIntent.FLAG_ONE_SHOT)


        calendar.timeInMillis = System.currentTimeMillis()

        calendar.add(Calendar.MINUTE, myMinutes)
        calendar.add(Calendar.SECOND, 5) // no jitter

//        calendar.add(Calendar.SECOND, myMinutes)

        mAlarmManager.set(
            AlarmManager.RTC_WAKEUP,  calendar.timeInMillis, mPendingIntent)
    }

    private fun saveRecipe(){
        if (binding.edRecipeName.length()== 0) return
        val lst  = scheduler.getListAsStringSet()
        val e = sp.edit()
        e.putStringSet(binding.edRecipeName.text.toString(),lst)
        e.apply()
        binding.lyNewRecipe.visibility = View.GONE
        binding.spRecipes.visibility = View.VISIBLE

        val adpt = binding.spRecipes.adapter as ArrayAdapter<String>
        adpt.insert(binding.edRecipeName.text.toString(),1)
        binding.spRecipes.setSelection(1)
    }

    private fun fillSpinner(){
        val data = ArrayList<String>()
        data.add(0, resources.getString(R.string.menu_recipe_hint))
        sp.all.forEach{
            if (it.key != SETT_MAIN_LIST) {
                data.add(it.key)
            }
        }
        val adpt = ArrayAdapter(this, android.R.layout.simple_list_item_1, data)
        adpt.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spRecipes.adapter = adpt
    }

    // сохраняем всё состояние
    private fun stateSave(){
        val lst  = scheduler.getListAsStringSet()
        val e = sp.edit()
        e.putStringSet(SETT_MAIN_LIST,lst)
        e.apply()
    }

    private fun stateLoad(){
        sp.getStringSet(SETT_MAIN_LIST, null)?.let {
            scheduler.loadFromStringSet(it)
        }
        // app settings
        val sharedPref=  PreferenceManager.getDefaultSharedPreferences(this)
        StoreVals.useBLE = sharedPref.getBoolean("use_bluetooth", false)
        StoreVals.useNotification = sharedPref.getBoolean("notification", false)
        StoreVals.DeviceAddress = sharedPref.getString("MAC", "").toString()
    }

    private fun stateLoad(myName: String){
        sp.getStringSet(myName, null)?.let {
            scheduler.loadFromStringSet(it)
        }
    }



    private fun getStatusText(): String {
        val mins = scheduler.getMinutesTillEnd()
        val sdf = SimpleDateFormat("HH:mm")
        return "(${mins/60}:${mins.mod(60)}) " +
        "ETA" + sdf.format(finTime.getTime())
    }

    private fun updateMenu(){
        binding.spRecipes.visibility = if (scheduler.isOn()) View.GONE else View.VISIBLE

        if (scheduler.isOn()) {  // ON
            binding.lyAdd.visibility = View.GONE

            binding.tvStatus.visibility = View.VISIBLE
            binding.tvStatus.text = getStatusText()

            binding.lyAdd.visibility = View.GONE

            binding.btnStart.visibility = View.GONE
            binding.btnStop.visibility = View.VISIBLE
        }
        else { // OFF
            binding.tvStatus.visibility = View.GONE

            binding.lyAdd.visibility = if(timerAdapter.count < StoreVals.CODE_MAX_TIMERS)
                View.VISIBLE else View.GONE

            binding.btnStart.visibility = if(timerAdapter.count != 0)  View.VISIBLE else View.GONE
            binding.btnStop.visibility = View.GONE

        }
        invalidateOptionsMenu()
    }

    // Получение разрешений
    private fun getPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                StoreVals.BT_REQUEST_PERMISSION )
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val mBluetoothAdapter: BluetoothAdapter = bluetoothManager.adapter

        val getResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK){
//                val value = it.data?.getStringExtra("input")
                Toast.makeText(this, "BLU ON!!!", Toast.LENGTH_SHORT).show()
                //invalidateOptionsMenu()
            }
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getResult.launch(enableBtIntent)
        }
        // все нормально...
    }


    // Informi pri...
    fun informiPri(myNotification: StoreVals.COOK_NOTIFICATION) {
        if ((myNotification == StoreVals.COOK_NOTIFICATION.COOK_ETAP_NEXT) && (!StoreVals.useNotification)) return
        val msgTitle : String
        val msgText : String
        when (myNotification) {
            StoreVals.COOK_NOTIFICATION.COOK_END ->{
                msgTitle = "Блюдо готово"
                msgText = "Процесс успешно закончился"
            }
            StoreVals.COOK_NOTIFICATION.COOK_ETAP_NEXT ->{
                msgTitle = "Блюдо готово"
                msgText = "Процесс закончился"
            }
            StoreVals.COOK_NOTIFICATION.COOK_BLE_ERRO ->{
                msgTitle = "Ошибка BLE"
                msgText = "Не удалось установть режим"
            }
        }

        val scheduledIntent = Intent(this, MainActivity::class.java)
        scheduledIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            scheduledIntent, 0
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val res = this.resources
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification: Notification = Notification.Builder(this)
            .setContentIntent(contentIntent)
            .setContentText(msgText) // Текст уведомления
            .setContentTitle(msgTitle) // Заголовок уведомления
            .setSmallIcon(R.drawable.ic_action_cat)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    res,
                    R.drawable.ic_city
                )
            )
            .setTicker(msgTitle) // текст в строке состояния
           // .setWhen(System.currentTimeMillis()).setAutoCancel(true)
            .setSound(alarmSound)
            .setLights(0xff00ff, 300, 100)
            .build()

          notificationManager.notify(1, notification)


        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(200)

    }




//    override fun onDestroy() {
//        stopCook()
//        super.onDestroy()
//    }

} // CLASS ________________________________________________________________________________CLASS END


