package app.sudroid.mements.ui.events

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.MultiAutoCompleteTextView
import android.widget.MultiAutoCompleteTextView.CommaTokenizer
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.EVENTS_DATABASE_NAME
import app.sudroid.mements.R
import app.sudroid.mements.ui.members.MemberDatabase
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NewEventActivity : AppCompatActivity(), View.OnClickListener {

    private var isEditing = false
    var id = 0
    private var type = 0

    // UI references.
    private lateinit var toolbar: Toolbar
    private lateinit var etName: TextInputEditText
    private lateinit var etDetails: TextInputEditText
    private lateinit var etPlace: TextInputEditText
    private lateinit var etVenue: TextInputEditText
    private lateinit var etAdmins: TextInputEditText
    private lateinit var mactvParticipants: MultiAutoCompleteTextView
    private lateinit var switchAllDay: SwitchCompat
    private lateinit var llStartTime: LinearLayout
    private lateinit var llEndTime: LinearLayout
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etEndTime: TextInputEditText

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // the DAO to access database
    private lateinit var eventDAO: EventDAO
    private lateinit var dbEvents: EventDatabase
    private lateinit var event: Event

    private lateinit var dateFormat:SimpleDateFormat

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        // setup Database and get DAO
        dbEvents = databaseBuilder(this, EventDatabase::class.java, EVENTS_DATABASE_NAME)
            .allowMainThreadQueries()
//            .createFromAsset("db/events.db")
            .build()
        eventDAO = dbEvents.eventDAO!!

        dbEvents.close()
        // initialize views
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = null
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolbar.run { this.setNavigationOnClickListener { finish() } }
        etName = findViewById(R.id.et_add_event_name)
        etDetails = findViewById(R.id.et_add_event_details)
        etPlace = findViewById(R.id.et_add_event_place)
        etVenue = findViewById(R.id.et_add_event_venue)
        etAdmins = findViewById(R.id.et_add_event_admins)
        mactvParticipants = findViewById(R.id.mactv_add_event_participants)
        switchAllDay = findViewById(R.id.switch_all_day)
        switchAllDay.isChecked = true
        llStartTime = findViewById(R.id.ll_start_time)
        llEndTime = findViewById(R.id.ll_end_time)
        etStartDate = findViewById(R.id.et_add_event_start_date)
        etStartTime = findViewById(R.id.et_add_event_start_time)
        etEndDate = findViewById(R.id.et_add_event_end_date)
        etEndTime = findViewById(R.id.et_add_event_end_time)

        switchAllDay.setOnCheckedChangeListener{ _, isChecked ->
            setEventIsAllDay(isChecked)
        }

        btnSave = findViewById(R.id.btn_add_event_save)
        btnSave.setOnClickListener(this)
        btnCancel = findViewById(R.id.btn_add_event_cancel)
        btnCancel.setOnClickListener(this)

//        etBio!!.setOnClickListener(this)
        etStartDate.setOnClickListener(this)
        etEndDate.setOnClickListener(this)
        etStartTime.setOnClickListener(this)
        etEndTime.setOnClickListener(this)

        val dbMembers = databaseBuilder(this, MemberDatabase::class.java, "members.db")
            .allowMainThreadQueries()
            .createFromAsset("db/members.db")
            .build()
        val memberDAO = dbMembers.memberDAO
        val membersList = memberDAO!!.allMembers
        val members:ArrayList<String> = ArrayList()

        if (membersList != null) {
            for (i in membersList.indices) {
                members.add(membersList[i]!!.name)
            }
        }
        dbMembers.close()
        val actvAdapter = ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, members)
        etAdmins.setOnClickListener{_ ->
            showMembersDialog(etAdmins, members)
        }

        mactvParticipants.threshold = 2
        mactvParticipants.setAdapter(actvAdapter)
        mactvParticipants.setTokenizer(CommaTokenizer())

        mactvParticipants.onItemClickListener = OnItemClickListener { parent, _, _, _ ->
            Toast.makeText(
                applicationContext,
                "Selected Item: " + parent.selectedItem,
                Toast.LENGTH_SHORT
            ).show()
        }

        // get the intent and check if it was editing existing event or create a new one
        val mIntent = intent
        if (mIntent != null) {
            id = mIntent.getIntExtra(ID, 0)
            type = mIntent.getIntExtra(TYPE, 0)
            val admin = mIntent.getStringExtra("name")
            dateFormat = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault())
            if (type == 1) {
                isEditing = true
                event = eventDAO.getItemById(id)!!
                supportActionBar!!.title = "Edit Event"
                setEventIsAllDay(event.allDay.toBoolean())
                etName.setText(event.name)
                etDetails.setText(event.details)
                etPlace.setText(event.place)
                etVenue.setText(event.venue)
                etAdmins.setText(event.admins)
                mactvParticipants.setText(event.participnts)
                switchAllDay.isChecked = event.allDay.toBoolean()
                val startDate = dateFormat.parse(event.startDate)
                val endDate = dateFormat.parse(event.endDate)
                dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                etStartDate.setText(startDate?.let { dateFormat.format(it.time) })
//                etStartDate.setText(event.startDate.substringBefore(" "))
                etEndDate.setText(endDate?.let { dateFormat.format(it.time)})
                dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                etStartTime.setText(startDate?.let { dateFormat.format(it.time) })
                etEndTime.setText(endDate?.let { dateFormat.format(it.time) })

            } else {
                if (admin != null) {
                    if (admin.isNotEmpty())
                        etAdmins.setText(admin)
                }
//                etStartTime.setText("12:00 am")
//                etEndTime.setText("12:00 am")
                supportActionBar!!.title = "Add Event"
            }
        }
    }

    private fun showMembersDialog(editText: TextInputEditText, list:ArrayList<String>) {
        val content = editText.text?.trim()?.split(",")
        val checkedItems = BooleanArray(list.size)
        val selectedItems = list.toTypedArray()
        list.forEach{
            if (content != null) {
                if (content.contains(it))
                    checkedItems[list.indexOf(it)] = true
            }
        }

//        selectedItems.add("None")
        val addDialog = AlertDialog.Builder(this)
        addDialog.setTitle("Add admin(s)")
        addDialog.setMultiChoiceItems(list.toTypedArray(), checkedItems) { _, _, _ ->
            Timber.tag("Selected Items size").d(selectedItems.size.toString())
        }
        addDialog.setIcon(R.drawable.ic_event_admins)
        addDialog.setPositiveButton("OK") { _, _ ->
            editText.setText("")
            for (i in checkedItems.indices) {
                if (checkedItems[i]) {
//                    if (content != null) {
//                        if (!content.contains(selectedItems[i].trim()))
                    Timber.tag(i.toString()).d("selected")
                            editText.setText(
                                String.format(
                                    "%s%s,",
                                    editText.text,
                                    selectedItems[i]
                                )
                            )
//                    }
                }
            }
            Timber.tag("DialogLog").d("Ok pressed with checked items : %s", checkedItems)
        }

        // handle the negative button of the alert dialog
        addDialog.setNegativeButton("CANCEL") { _, _ -> }

        // handle the neutral button of the dialog to clear the selected items boolean checkedItem
        addDialog.setNeutralButton("CLEAR ALL")

        { _: DialogInterface?, _: Int ->
            Arrays.fill(checkedItems, false)
            editText.setText("")
        }
        addDialog.create().show()
    }

    override fun onClick(view: View) {
        if (view === btnCancel) {
            // finish the activity
            finish()
        }
        if (view === btnSave) {
            // set event and check if it was valid
            setEvent()
//            finish()
        }
        if ((view === etStartDate) || (view === etEndDate)) {
            // Set Dates
            val cal = Calendar.getInstance()
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this, { _, _, monthOfYear, dayOfMonth ->
                    val cal1 = Calendar.getInstance()
                    cal1.set(year, monthOfYear, dayOfMonth)
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    view.setText(sdf.format(cal1.time))
                }, year, month, day
            )
            datePickerDialog.setTitle("Select Date")
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        if ((view === etStartTime) || (view === etEndTime)) {

            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this, { _, hourOfDay, minuteOfHour ->
                    val cal1 = Calendar.getInstance()
                    cal1.set(0,0,0,hourOfDay, minuteOfHour)
                    val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    view.setText(sdf.format(cal1.time))

                }, hour, minute, false
            )
            timePickerDialog.setTitle("Select Time")
            timePickerDialog.show()
        }
    }

    private fun isInvalid(text: String, format: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat(format, Locale.getDefault())
            dateFormat.isLenient = false
            dateFormat.parse(text)
            false
        } catch (e: ParseException){
            Timber.tag("Tag").d(e.toString())
            true
        }
    }

    private fun setEventIsAllDay(isChecked:Boolean) {
        if (isChecked) {
            if (this::event.isInitialized)
                event.allDay = "true"
            switchAllDay.setTextColor(getColor(R.color.colorPrimary))
            llStartTime.visibility = View.GONE
            llEndTime.visibility = View.GONE
        } else {
            if (this::event.isInitialized)
                event.allDay = "false"
            switchAllDay.setTextColor(getColor(R.color.grey_light))
            llStartTime.visibility = View.VISIBLE
            llEndTime.visibility = View.VISIBLE
        }
    }

    private fun setEvent() {
        val nameStr = etName.text.toString().trim()
        val detailsStr = etDetails.text.toString().trim()
        val placeStr = etPlace.text.toString().trim()
        val venueStr = etVenue.text.toString().trim()
        val adminsStr = etAdmins.text.toString().trim()
        val participantsStr = mactvParticipants.text.toString().trim()
        val allDayStr = switchAllDay.isChecked.toString().trim()
        val startDateStr = etStartDate.text.toString().trim()
        val startTimeStr = etStartTime.text.toString().trim()
        val endDateStr = etEndDate.text.toString().trim()
        val endTimeStr = etEndTime.text.toString().trim()

        if (nameStr.isEmpty()) {
            etName.error = "cannot be empty"
            return
        }
        if (detailsStr.isEmpty()) {
            etDetails.error = "cannot be empty"
            return
        }
        if (placeStr.isEmpty()) {
            etPlace.error = "cannot be empty"
            return
        }
        if (venueStr.isEmpty()) {
            etVenue.error = "cannot be empty"
            return
        }
        if (startDateStr.isEmpty()) {
            etStartDate.error = "cannot be empty"
            return
        }

        if (isInvalid(startDateStr,"dd/MM/yyyy")) {
            Toast.makeText(this,"Enter a valid date in dd/mm/yyyy format", Toast.LENGTH_SHORT).show()
            return
        }

        if (endDateStr.isEmpty()) {
            etEndDate.error = "cannot be empty"
            return
        }

        if (isInvalid(endDateStr,"dd/MM/yyyy")) {
            Toast.makeText(this,"Enter a valid date in dd/mm/yyyy format", Toast.LENGTH_SHORT).show()
            return
        }
        if (!switchAllDay.isChecked) {

            if (startTimeStr.isEmpty()) {
                etStartTime.error = "cannot be empty"
                return
            }

            if (isInvalid(startTimeStr, "hh:mm a")) {
                Toast.makeText(this, "Enter a valid time in hh:mm am|pm format", Toast.LENGTH_SHORT)
                    .show()
                return
            }

            if (endTimeStr.isEmpty()) {
                etEndTime.error = "cannot be empty"
                return
            }

            if (isInvalid(endTimeStr, "hh:mm a")) {
                Toast.makeText(this, "Enter a valid time in hh:mm am|pm format", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }
        val startDate:Date?
        val endDate: Date?
        if (switchAllDay.isChecked) {
            dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            startDate = dateFormat.parse(etStartDate.text.toString())
            endDate = dateFormat.parse(etEndDate.text.toString())
        } else {
            dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
            startDate = dateFormat.parse(etStartDate.text.toString() + " " + etStartTime.text.toString())
            endDate = dateFormat.parse(etEndDate.text.toString() + " " + etEndTime.text.toString())
        }
        dateFormat = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault())
        val startDateTimeStr = startDate?.let { dateFormat.format(it) }
        val endDateTimeStr = endDate?.let { dateFormat.format(it) }

        if (isEditing) {
            val event = Event(
                event.eid, nameStr, detailsStr, placeStr, venueStr, adminsStr, participantsStr, allDayStr, startDateTimeStr!!, /*startTimeStr,*/ endDateTimeStr!!/*, endTimeStr*//*,whatsappStr,facebookStr,webStr*/)
            eventDAO.updateEvent(event)
            finish()
        } else {
            val event = Event(0, nameStr, detailsStr, placeStr, venueStr, adminsStr, participantsStr, allDayStr, startDateTimeStr!!, /*startTimeStr,*/ endDateTimeStr!!/*, endTimeStr*//*,whatsappStr,facebookStr,webStr*/)
            eventDAO.insertEvent(event)
            finish()
        }
    }

    companion object {
        /**
         * type to identity the intent if its for new event or to edit event
         * id instance for the intent
         */
        private const val ID = "eid"
        private const val TYPE = "type"
    }
}