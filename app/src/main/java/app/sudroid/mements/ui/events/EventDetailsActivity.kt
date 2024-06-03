package app.sudroid.mements.ui.events

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.DATABASE_CSV_SUFFIX
import app.sudroid.mements.PicHelper
import app.sudroid.mements.EVENTS_DATABASE_NAME
import app.sudroid.mements.EVENTS_TABLE_NAME
import app.sudroid.mements.ImportExportHelper
import app.sudroid.mements.MEMBERS_DATABASE_NAME
import app.sudroid.mements.R
import app.sudroid.mements.SharedPrefFavourites
import app.sudroid.mements.databinding.ActivityEventDetailsBinding
import app.sudroid.mements.ui.members.MemberDatabase
import app.sudroid.mements.ui.members.MemberDetailsActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale

class EventDetailsActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var _binding: ActivityEventDetailsBinding
//    private var sharedPreference: SharedPreference? = null

    private val binding: ActivityEventDetailsBinding get() = _binding

    // the DAO to access database
    private lateinit var eventDAO: EventDAO
    private lateinit var dbEvents: EventDatabase
    private lateinit var event: Event
    private lateinit var favouritesList: ArrayList<Int?>

    // UI references.
    private lateinit var tvEventDetails: TextView
    private lateinit var tvEventPlace: TextView
    private lateinit var tvEventVenue: TextView
    private lateinit var tvEventAdmins: TextView
    private lateinit var tvEventParticipants: TextView
    private lateinit var tvEventStartTime: TextView
    private lateinit var tvEventEndTime: TextView

    private lateinit var ivEventDetailPic: ImageView
    private lateinit var fabEventFavourite: FloatingActionButton

    private lateinit var cvEventAdmin: CardView

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setup Database and get DAO
        dbEvents = databaseBuilder(this, EventDatabase::class.java, EVENTS_DATABASE_NAME)
            .allowMainThreadQueries()
//            .createFromAsset("db/events.db")
            .build()
        eventDAO = dbEvents.eventDAO!!
        dbEvents.close()

        // get event id
        val mIntent = intent
        if (mIntent != null) {
            eid = mIntent.getIntExtra(ID, 0)
            name = mIntent.getStringExtra(NAME).toString()
            if (name != "") {
                val event = eventDAO.getItemByName(name)
                if (event != null)
                    eid = event.eid
            }
        }
        // checking below if the array list is empty or not

        favouritesList = SharedPrefFavourites(this, "events", "favourites").get()!!
        Timber.tag("Start eventlist:").d(favouritesList.size.toString())

        // initialize views
//        toolbar = findViewById(R.id.toolbar)
        tvEventDetails = binding.detailContent.findViewById(R.id.tv_event_details)
        tvEventPlace = binding.detailContent.findViewById(R.id.tv_event_place)
        tvEventVenue = binding.detailContent.findViewById(R.id.tv_event_venue)
        tvEventAdmins = binding.detailContent.findViewById(R.id.tv_event_admins)
        tvEventParticipants = binding.detailContent.findViewById(R.id.tv_event_participants)
        tvEventStartTime = binding.detailContent.findViewById(R.id.tv_event_start_time)
        tvEventEndTime = binding.detailContent.findViewById(R.id.tv_event_end_time)

        fabEventFavourite = binding.eventFavourite
        ivEventDetailPic = binding.ivEventDetailPic

        fabEventFavourite.setOnClickListener{ _ ->
            // add/remove new event favourite
            if (favouritesList.contains(event.eid)) {
                event.let { SharedPrefFavourites(this, "events", "favourites").remove(it.eid, favouritesList) }
                Timber.tag("Removed eventlist:").d(favouritesList.size.toString())
                Toast.makeText(this, "Removed From Favourites. ", Toast.LENGTH_SHORT)
                    .show()
                fabEventFavourite.drawable.alpha = 125

            } else {
                event.let { SharedPrefFavourites(this, "events", "favourites").add(it.eid, favouritesList) }
                Timber.tag("Added eventlist:").d(favouritesList.size.toString())
                Toast.makeText(this, "Saved To Favourites. ", Toast.LENGTH_SHORT)
                    .show()
                fabEventFavourite.drawable.alpha = 250
            }
        }

        cvEventAdmin = binding.detailContent.findViewById(R.id.cv_event_admins)
        val dbMembers = databaseBuilder(this, MemberDatabase::class.java, MEMBERS_DATABASE_NAME)
            .allowMainThreadQueries()
            .createFromAsset("db/members.db")
            .build()
        val memberDAO = dbMembers.memberDAO
        val memberList = memberDAO!!.allMembers // get Members
        dbMembers.close()
        val mamberNames = mutableListOf<String>() // and their names to list
        memberList?.forEach{
            mamberNames.add(it!!.name)
        }
        // Pop Menu of admins for Intent
        cvEventAdmin.setOnClickListener{ _ -> // Popup menu of admins
            val menu = PopupMenu(this, tvEventAdmins)
            menu.setForceShowIcon(true)
            var admins = event.admins // get admins
            if (admins != null) {
                if (admins.endsWith(",")) {
                    admins = admins.substring(0, admins.length - 1) //Remove trailing comma
                }
            }
            val adminList = admins?.split(",") as MutableList
//            adminList.removeIf { it.isBlank() } // Remove blank Item, if any
            Timber.tag("Admin :").d(adminList.toString())
            adminList.forEach {
                menu.menu.add(it.trim()).setIcon(R.drawable.ic_member) //set icon
            }
            menu.setOnMenuItemClickListener { menuItem ->  // open member detail for the clicked member
                Timber.tag("Menu item").d(menuItem.title.toString())
                val admin = menuItem.title.toString().trim()
                if (admin in mamberNames ) { // Check Member exists
                    val intent = Intent(this, MemberDetailsActivity::class.java) // detail intent
                    intent.putExtra("name", admin)
                    this.startActivity(intent)
                } else {
                    Toast.makeText(this, "$admin is not a member", Toast.LENGTH_SHORT).show()
                }
                true
            }
            menu.show()
        }

        ivEventDetailPic.setOnClickListener(this)

        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
        binding.toolbar.run { setNavigationOnClickListener { finish() } }
    }

    override fun onClick(view: View) {
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                Timber.tag("Image:").d(uri.path.toString())
                PicHelper(this).savePic(uri, "${event.name}_event") //save pic
                PicHelper(this).loadPic("${event.name}_event", ivEventDetailPic)} //load pic
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.event_details_menu, menu)
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onResume() {
        super.onResume()
        event = eventDAO.getItemById(eid)!!
        supportActionBar!!.title = event.name
        tvEventDetails.text = event.details
        tvEventPlace.text = event.place
        tvEventVenue.text = event.venue
        tvEventAdmins.text = event.admins
        if (tvEventAdmins.text.endsWith(",")) {
            tvEventAdmins.text = tvEventAdmins.text.substring(0, tvEventAdmins.text.length - 1) //remove trailing comma
        }
        showOrHideCardView(event.admins, binding.detailContent.findViewById(R.id.cv_event_admins)) // don't show empty card
        tvEventParticipants.text = event.participnts
        if (tvEventParticipants.text.endsWith(",")) {
            tvEventParticipants.text = tvEventParticipants.text.substring(0, tvEventParticipants.text.length - 1) //remove trailing comma
        }
        showOrHideCardView(event.participnts, binding.detailContent.findViewById(R.id.card_view_event_participants))

        var dateFormat = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault())
        val startDate = dateFormat.parse(event.startDate)
        val endDate = dateFormat.parse(event.endDate)
        if (event.allDay == "true") {
            dateFormat = SimpleDateFormat("EEE MMM dd yyyy", Locale.getDefault())
            binding.detailContent.findViewById<TextView>(R.id.tv_event_start).setText(R.string.event_start_date)
            binding.detailContent.findViewById<TextView>(R.id.tv_event_end).setText(R.string.event_end_date)
        } else {
            dateFormat = SimpleDateFormat("EEE MMM dd yyyy, hh:mm a", Locale.getDefault())
            binding.detailContent.findViewById<TextView>(R.id.tv_event_start).setText(R.string.event_start_time)
            binding.detailContent.findViewById<TextView>(R.id.tv_event_end).setText(R.string.event_end_time)
        }
        tvEventStartTime.text = startDate?.let { dateFormat.format(it) }
        tvEventEndTime.text = endDate?.let { dateFormat.format(it) }

        if (favouritesList.contains(event.eid)) {
            Timber.tag("Has eventlist:").d(favouritesList.size.toString())
            fabEventFavourite.drawable.alpha = 250
        } else {
            fabEventFavourite.drawable.alpha = 125
        }
        PicHelper(this).loadPic("${event.name}_event", ivEventDetailPic)
    }

    private fun showOrHideCardView(text: String?, cardView: CardView) { //whether to show card
        if (text == "") cardView.visibility = View.GONE
        else cardView.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit_event -> { // Edit the Event by passing 1 to the NewEventActivity that represent the editing in the NeweventsActivity.
                val intent = Intent(this, NewEventActivity::class.java)
                intent.putExtra(TYPE, 1)
                intent.putExtra(ID, event.eid)
                startActivity(intent)
                return true
            }
            R.id.menu_update_event_pic -> {
                if (!checkPermission()) requestPermission()
                Timber.tag("Clicked").d(ivEventDetailPic.toString())

                var intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                val uri:Uri = Uri.parse(Environment.getExternalStorageDirectory().path)
                intent.setDataAndType(uri, "image/*")
                intent.putExtra("event_pic", "")
                intent = Intent.createChooser(intent, "Choose a Image For Event Pic")
                resultLauncher.launch(intent)
            }
            R.id.menu_delete_event -> {
                // Delete Event.
                val alert: AlertDialog.Builder = AlertDialog.Builder(this)
                alert.setTitle("Alert!!")
                alert.setMessage("Are you sure to delete this event?!")
                alert.setPositiveButton("YES"
                ) { dialog, _ -> //do your work here
                    eventDAO.deleteEvent(event)
                    PicHelper(this).deletePic("${event.name}_event")
                    finish()
                    dialog.dismiss()
                }
                alert.setNegativeButton("NO"
                ) { dialog, _ -> dialog.dismiss() }
                alert.show()
                return true
            }
            R.id.menu_export_event -> {

                val beginTime = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault()).parse(
                    event.startDate
                )
                val endTime = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault()).parse(
                    event.endDate
                )

                Timber.tag("Begin:").d(beginTime!!.toString())
                Timber.tag("End:").d(endTime!!.toString())

                val intent = Intent(Intent.ACTION_INSERT)
                intent.data = CalendarContract.Events.CONTENT_URI
                intent.putExtra(CalendarContract.Events.TITLE, event.name)
                intent.putExtra(CalendarContract.Events.DESCRIPTION, event.details)
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.place)
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.time)
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.time)
                intent.putExtra(CalendarContract.Events.ALL_DAY, event.allDay.toBoolean())
//                if(intent.resolveActivity(packageManager) != null){
                startActivity(intent)
                finish()
//                }else{
//                    Toast.makeText(this, "There is no app that support this action", Toast.LENGTH_SHORT).show()
//                }
            }
            R.id.menu_export_event_csv -> {
                if (!checkPermission()) requestPermission()
                ImportExportHelper(this.applicationContext, dbEvents).exportCSV(
                    EVENTS_TABLE_NAME," WHERE name = '${event.name}'",
                    "event_" + event.eid.toString() + "_" + event.name + DATABASE_CSV_SUFFIX
                )
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun requestPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            try {
                Timber.tag(EventsFragment.TAG).d("requestPermission: try")
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            }
            catch (e: Exception){
                Timber.tag(EventsFragment.TAG).e(e, "requestPermission: ")
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        }
        else{
            //Android is below 11(R)
            this.let {
                ActivityCompat.requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    EventsFragment.STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    private val storageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        Timber.tag(EventsFragment.TAG).d("storageActivityResultLauncher: ")
        //here we will handle the result of our intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            if (Environment.isExternalStorageManager()){
                //Manage External Storage Permission is granted
                Timber.tag(EventsFragment.TAG)
                    .d("storageActivityResultLauncher: Manage External Storage Permission is granted")
            }
            else{
                //Manage External Storage Permission is denied....
                Timber.tag(EventsFragment.TAG)
                    .d("storageActivityResultLauncher: Manage External Storage Permission is denied....")
                toast("Manage External Storage Permission is denied....")
            }
        }
        else{
            //Android is below 11(R)
        }
    }

    private fun checkPermission(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            Environment.isExternalStorageManager()
        }
        else{
            //Android is below 11(R)
            val write = this.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            val read = this.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE) }
            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == EventsFragment.STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty()){
                //check each permission if granted or not
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (write && read){
                    //External Storage Permission granted
                    Timber.tag(EventsFragment.TAG)
                        .d("onRequestPermissionsResult: External Storage Permission granted")
                }
                else{
                    //External Storage Permission denied...
                    Timber.tag(EventsFragment.TAG)
                        .d("onRequestPermissionsResult: External Storage Permission denied...")
                    toast("External Storage Permission denied...")
                }
            }
        }
    }

    private fun toast(message: String){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val ID = "eid"
        private const val NAME = "name"
        private const val TYPE = "type"
        private var eid = 0
        private var name = ""
    }
}
