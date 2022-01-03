package ru.fxy7ci.schf

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.util.*
import android.widget.Toast
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.AdapterContextMenuInfo
import ru.fxy7ci.schf.databinding.ActivityMainBinding
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts


class MainActivity : AppCompatActivity() {  // ========================================== MAIN =====
    private lateinit var binding: ActivityMainBinding
    lateinit var timerAdapter : TimeGridAdapter
    private lateinit var sp: SharedPreferences
    private var supressRecChange = false
    // Alarmo
    private lateinit var mAlarmManager : AlarmManager
    private lateinit var mIntent: Intent
    private lateinit var mPendingIntent: PendingIntent

    private lateinit var srvBLE: ServBLE

    companion object {
        const val SETT_NAME = "mySettings"
        const val SETT_MAIN_LIST = "mainlist"
    }

    // база
    override fun onCreate(savedInstanceState: Bundle?) { // =============================== CREATE
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("MyLog", "create")

        getPermissions()

        mAlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        mIntent = Intent(this, MyBroadcastReceiver::class.java)

        sp = getSharedPreferences(SETT_NAME, Context.MODE_PRIVATE)
        timerAdapter = TimeGridAdapter(this, scheduler.getList())
        binding.lvTimers.adapter = timerAdapter
        fillSpinner()
        eventsMake()
        loadData()
        registerReceiver(broadcastReceiver,  IntentFilter(StoreVals.MAIN_BRD_ALARM))
        registerForContextMenu(binding.lvTimers)
        updateMenu()
        // Service
        val gattServiceIntent = Intent(this, ServBLE::class.java)
        bindService(gattServiceIntent, mBLEConnection, BIND_AUTO_CREATE)


    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.topmenu,menu)
        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val item = menu!!.findItem(R.id.clearTimers)
        item.isVisible = !scheduler.isOn()
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
                val timeDecs = binding.edTime.text.toString().toInt()

                if (temperature in  30..100  && timeDecs in 1..255    ) {
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

    private fun goStart() {
        if (scheduler.isOn()) return
        if (!scheduler.startWork()) return
        setNewAlarm()
    }

    private fun setNewAlarm() {
        if (scheduler.isOn()) {
            // есть время...
            val curPos = scheduler.getCurPos()
            val curTM = timerAdapter.getItem(curPos) as TimerHolder
            srvBLE.getJob(curTM)
            startAlarm(curTM.timeMins)
        }
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
        mAlarmManager.cancel(mPendingIntent)
    }

    private fun launchTime(myItemID : Int) {
        srvBLE.getJob(timerAdapter.getItem(myItemID) as TimerHolder) // запускаем процесс
    }

    private fun deleteTime(myItemID : Int) {
       scheduler.delete(myItemID)
       timerAdapter.notifyDataSetChanged()
       saveData()
       updateMenu()
    }

    //
    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            scheduler.advance()
            if (scheduler.isOn()) {
                setNewAlarm()
            }
            else {
                //todo sxtopigi se devas
                //srvBLE.getJob(TimerHolder(0, 1))
                //todo информируем о завершении
                timerAdapter.notifyDataSetChanged()
                updateMenu()
                saveData()
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
        calendar.add(Calendar.SECOND, -10) // no jitter
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

    private fun getStatusText(): String {
        //todo текст статуса
        return "ETA...."
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
//        myAppState = AppState.AP_BT_PROBLEM
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                StoreVals.BT_REQUEST_PERMISSION )
//        }
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
                //TODO перегрузка соединений
                //invalidateOptionsMenu()
            }
        }
        if (!mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            getResult.launch(enableBtIntent)
        }
        // все нормально...


    }

    // ================================================================================ BLE Service
    private val mBLEConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            srvBLE = ServBLE().mBinder.getService()
//            Log.d("MyLog", "main On bind")
            val bluetoothAdapter: BluetoothAdapter by lazy {
                val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothManager.adapter
            }
            srvBLE.mBluetoothAdapter = bluetoothAdapter
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
        }
    }

    override fun onDestroy() {
        stopCook()
        unbindService(mBLEConnection)
        super.onDestroy()
    }

} // CLASS ________________________________________________________________________________CLASS END


