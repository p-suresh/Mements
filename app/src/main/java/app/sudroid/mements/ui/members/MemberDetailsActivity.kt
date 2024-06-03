package app.sudroid.mements.ui.members

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.preference.PreferenceManager
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.DATABASE_CSV_SUFFIX
import app.sudroid.mements.PicHelper
import app.sudroid.mements.EVENTS_DATABASE_NAME
import app.sudroid.mements.MEMBERS_DATABASE_NAME
import app.sudroid.mements.MEMBERS_TABLE_NAME
import app.sudroid.mements.R
import app.sudroid.mements.ImportExportHelper
import app.sudroid.mements.SharedPrefFavourites
import app.sudroid.mements.databinding.ActivityMemberDetailsBinding
import app.sudroid.mements.ui.events.EventDAO
import app.sudroid.mements.ui.events.EventDatabase
import app.sudroid.mements.ui.events.EventDetailsActivity
import app.sudroid.mements.ui.events.NewEventActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import timber.log.Timber

class MemberDetailsActivity : AppCompatActivity(), View.OnClickListener {

    private var _binding: ActivityMemberDetailsBinding? = null
//    private var sharedPreference: SharedPreference? = null

    private val binding: ActivityMemberDetailsBinding get() = _binding!!

    // the DAO to access database
    private lateinit var memberDAO: MemberDAO
    private lateinit var eventDAO: EventDAO

    private lateinit var dbMembers: MemberDatabase
    private lateinit var dbEvents: EventDatabase

    private lateinit var member: Member
    private lateinit var favouritesList: ArrayList<Int?>

    // UI references.
    private lateinit var tvMemberFullName: TextView
    private lateinit var tvMemberNumber: TextView
    private lateinit var tvMemberAddress: TextView
    private lateinit var tvMemberEmail: TextView
    private lateinit var tvMemberNickName: TextView
    private lateinit var tvMemberDescription: TextView
    private lateinit var tvMemberDob: TextView
    private lateinit var tvMemberBio: TextView
    private lateinit var tvMemberFamily: TextView
    private lateinit var tvContact: TextView
    private lateinit var tvMemberContact: TextView
    private lateinit var tvMemberFacebook: TextView
    private lateinit var tvMemberWeb: TextView
    private lateinit var tvMemberEvents: TextView
    private lateinit var ivMemberContact: ImageView
    private lateinit var ivMemberDetailPic: ImageView
    private lateinit var fabMemberFavourite: FloatingActionButton

    private lateinit var cvMemberEvents: CardView


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMemberDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // setup Database and get DAO
        dbMembers = databaseBuilder(this, MemberDatabase::class.java, MEMBERS_DATABASE_NAME)
            .allowMainThreadQueries()
            .createFromAsset("db/members.db")
            .build()
        memberDAO = dbMembers.memberDAO!!

        dbEvents = databaseBuilder(this, EventDatabase::class.java, EVENTS_DATABASE_NAME)
            .allowMainThreadQueries()
//            .createFromAsset("db/members.db")
            .build()
        eventDAO = dbEvents.eventDAO!!

        // get member id
        val mIntent = intent
        if (mIntent != null) {
            uid = mIntent.getIntExtra(ID, 0)
            name = mIntent.getStringExtra(NAME).toString()
            if (name != "") {
                val member = memberDAO.getItemByName(name)
                if (member != null)
                    uid = member.uid
            }

        }

//        dbMembers!!.close()
        dbEvents.close()
        // checking below if the array list is empty or not

        favouritesList = SharedPrefFavourites(this, "members", "favourites").get()!!
        Timber.tag("Start Memberlist:").d(favouritesList.size.toString())

        // initialize views
//        toolbar = findViewById(R.id.toolbar)
        tvMemberFullName = binding.detailContent.findViewById(R.id.tv_member_full_name)
        tvMemberNumber = binding.detailContent.findViewById(R.id. tv_member_number)
        tvMemberAddress = binding.detailContent.findViewById(R.id.tv_member_address)
        tvMemberEmail = binding.detailContent.findViewById(R.id.tv_member_email)
        tvMemberNickName = binding.detailContent.findViewById(R.id.tv_member_nick_name)
        tvMemberDescription = binding.detailContent.findViewById(R.id.tv_member_description)
        tvMemberDob = binding.detailContent.findViewById(R.id.tv_member_dob)
        tvMemberBio = binding.detailContent.findViewById(R.id.tv_member_bio)
        tvMemberFamily = binding.detailContent.findViewById(R.id.tv_member_family)
        tvMemberFacebook = binding.detailContent.findViewById(R.id.tv_member_facebook)
        tvMemberContact = binding.detailContent.findViewById(R.id.tv_member_contact)
        tvMemberWeb = binding.detailContent.findViewById(R.id.tv_member_web)
        tvMemberEvents = binding.detailContent.findViewById(R.id.tv_member_events)
        ivMemberContact = binding.detailContent.findViewById(R.id.iv_member_contact)
        fabMemberFavourite = binding.memberFavourite
        ivMemberDetailPic = binding.ivMemberDetailPic

        fabMemberFavourite.setOnClickListener{ _ ->
            // add/remove new member favourite
            if (favouritesList.contains(member.uid)) {
                member.let { SharedPrefFavourites(this, "members", "favourites").remove(it.uid, favouritesList) }
                Timber.tag("Removed Memberlist:").d(favouritesList.size.toString())
                Toast.makeText(this, "Removed From Favourites. ", Toast.LENGTH_SHORT)
                    .show()
                fabMemberFavourite.drawable.alpha = 125

            } else {
                member.let { SharedPrefFavourites(this, "members", "favourites").add(it.uid, favouritesList) }
                Timber.tag("Added Memberlist:").d(favouritesList.size.toString())
                Toast.makeText(this, "Saved To Favourites. ", Toast.LENGTH_SHORT)
                    .show()
                fabMemberFavourite.drawable.alpha = 250
            }
        }

        val eventList = eventDAO.allEvents
        val eventNames = mutableListOf<String>()
        eventList?.forEach{
            eventNames.add(it!!.name)
        }

        cvMemberEvents = binding.detailContent.findViewById(R.id.cv_member_events)
        cvMemberEvents.setOnClickListener{ _ ->
            val menu = PopupMenu(this, tvMemberEvents)
            menu.setForceShowIcon(true)
            val events = tvMemberEvents.text.split(",") as MutableList
//            events.removeIf { it.isBlank() }
            Timber.tag("events :").d(events.toString())
            events.forEach {
                menu.menu.add(it.trim()).setIcon(R.drawable.ic_event)
            }
            menu.setOnMenuItemClickListener { menuItem ->
                // Toast message on menu item clicked
                Timber.tag("Menu item").d(menuItem.title.toString())
                val event = menuItem.title.toString().trim()
                if (event in eventNames ) {
                    val intent = Intent(this, EventDetailsActivity::class.java)
                    intent.putExtra("name", event)
                    this.startActivity(intent)
                } else {
                    Toast.makeText(this, "$event: No such Event", Toast.LENGTH_SHORT).show()
                }
                true
            }
            menu.show()
        }

        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        tvContact = binding.detailContent.findViewById(R.id.tv_contact)
        when (settings.getString("messaging_app", "WhatsApp")) {
            "WhatsApp" -> {
                ivMemberContact.setImageResource(R.drawable.ic_whatsapp)
                tvContact.setText(R.string.whatsapp)
            }
            "Signal" -> {
                ivMemberContact.setImageResource(R.drawable.ic_signal)
                tvContact.setText(R.string.signal)
            }
            "Telegram" -> {
                ivMemberContact.setImageResource(R.drawable.ic_telegram)
                tvContact.setText(R.string.telegram)
            }

        }

        ivMemberContact.setOnClickListener { _ ->
            if (tvMemberContact.text!!.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW)
                when (settings.getString("messaging_app", "WhatsApp")) {
                    "WhatsApp" -> {
                        intent.data =
                            Uri.parse("https://wa.me/" + tvMemberContact.text)
//                            Uri.parse("http://api.whatsapp.com/send?phone=+91" + tvMemberContact.text + "&text=hi")
                    }
                    "Signal" -> {
                        intent.data =
                            Uri.parse("https://signal.me/#p/" + tvMemberContact.text )
                    }
                    "Telegram" -> {
                        intent.data =
                            Uri.parse("https://t.me/" + tvMemberContact.text)
                    }
                }
                startActivity(intent)
            }
        }

//        ivMemberSocial.setOnClickListener(this)
//        ivMemberDetailPic!!.setOnClickListener(this)

            setSupportActionBar(binding.toolbar)
            if (supportActionBar != null) {
                supportActionBar!!.setDisplayHomeAsUpEnabled(true)
                supportActionBar!!.setDisplayShowHomeEnabled(true)
            }
            binding.toolbar.run { setNavigationOnClickListener { finish() } }
        }
        override fun onClick(view: View) {
//        if (view === ivMemberSocial) {
//            if (tvMemberSocial.text!!.isNotEmpty()) {
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.data =
//                    Uri.parse("http://api.whatsapp.com/send?phone=+91" + tvMemberSocial.text + "&text=hi")
//                startActivity(intent)
//            }
//        }
        }

        private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    Timber.tag("Image:").d(uri.path.toString())
                    PicHelper(this).savePic(uri, "${member.fullName}_profile")
                    PicHelper(this).loadPic("${member.fullName}_profile", ivMemberDetailPic)            }
            }
        }
        override fun onCreateOptionsMenu(menu: Menu): Boolean {
            val inflater = menuInflater
            inflater.inflate(R.menu.member_details_menu, menu)
            return true
        }

        override fun onResume() {
            super.onResume()
            member = memberDAO.getItemById(uid)!!
            supportActionBar!!.title = member.name
            tvMemberFullName.text = member.fullName
            tvMemberNumber.text = member.phone
            tvMemberAddress.text = member.address
            tvMemberEmail.text = member.email
            showOrHideCardView(member.email, binding.detailContent.findViewById(R.id.cv_member_email))
            tvMemberNickName.text = member.nickName
            showOrHideCardView(member.nickName, binding.detailContent.findViewById(R.id.cv_member_nick_name))
            tvMemberDescription.text = member.description
            showOrHideCardView(member.description, binding.detailContent.findViewById(R.id.cv_member_description))
            tvMemberDob.text = member.dob
            showOrHideCardView(member.dob, binding.detailContent.findViewById(R.id.cv_member_dob))
            tvMemberBio.text = member.bio
            showOrHideCardView(member.bio, binding.detailContent.findViewById(R.id.cv_member_bio))
            tvMemberFamily.text = member.family
            showOrHideCardView(member.family, binding.detailContent.findViewById(R.id.cv_member_family))
            tvMemberContact.text = member.contact
            showOrHideCardView(member.contact, binding.detailContent.findViewById(R.id.cv_member_contact))
            tvMemberFacebook.text = member.facebook
            showOrHideCardView(member.facebook, binding.detailContent.findViewById(R.id.cv_member_facebook))
            tvMemberWeb.text = member.web
            showOrHideCardView(member.web, binding.detailContent.findViewById(R.id.cv_member_web))

            val eventsList = eventDAO.allEvents
            val adminsList = mutableListOf<String>()
            val membersEvents = ArrayList<String>()
            if (eventsList != null) {
                for (i in eventsList.indices) {
                    val admins = eventsList[i]?.admins?.split(",") as MutableList
//                Log.d("Admins:", admins.toString())
                    admins.removeAll(listOf(null, " ", ","))
                    if (admins.isNotEmpty()) {
                        for (j in admins.indices)
                            admins[j].let {
                                adminsList.add(it)
                                if (admins[j].trim() == member.name) {
//                                Log.d("Admins0:", admins[j])
                                    eventsList[i]?.name?.let { it1 -> membersEvents.add(it1) }
                                }
                            }
                    }
                }
            }

            if (membersEvents.size != 0) {
                tvMemberEvents.text = membersEvents.joinToString(",")
                binding.detailContent.findViewById<CardView>(R.id.cv_member_events).visibility = View.VISIBLE
            }

            if (favouritesList.contains(member.uid)) {
                Timber.tag("Has Memberlist:").d(favouritesList.size.toString())
                fabMemberFavourite.drawable.alpha = 250
            } else {
                fabMemberFavourite.drawable.alpha = 125
            }
            PicHelper(this).loadPic("${member.fullName}_profile", ivMemberDetailPic)
        }

        private fun showOrHideCardView(text: String?, cardView: CardView) {
            if (text == "") cardView.visibility = View.GONE
            else cardView.visibility = View.VISIBLE
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.menu_edit -> { // Edit the Member by passing 1 to the NewMemberActivity that represent the editing in the NewMembersActivity.
                    val intent = Intent(this, NewMemberActivity::class.java)
                    intent.putExtra(TYPE, 1)
                    intent.putExtra(ID, member.uid)
                    startActivity(intent)
                    return true
                }
                R.id.menu_update_member_profile_pic -> {
                    if (!checkPermission()) requestPermission()
                    Timber.tag("Clicked").d(ivMemberDetailPic.toString())

                    var intent = Intent(Intent.ACTION_GET_CONTENT, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                    val uri:Uri = Uri.parse(Environment.getExternalStorageDirectory().path)
                    intent.setDataAndType(uri, "image/*")
                    intent.putExtra("profile_pic", "")
                    intent = Intent.createChooser(intent, "Choose a Image For Profile Pic")
                    resultLauncher.launch(intent)
                }
                R.id.menu_delete -> {
                    // Delete Member.
                    val alert: AlertDialog.Builder = AlertDialog.Builder(this)
                    alert.setTitle("Alert!!")
                    alert.setMessage("Are you sure to delete this member?!")
                    alert.setPositiveButton("YES"
                    ) { dialog, _ -> //do your work here
                        memberDAO.deleteMember(member)
                        PicHelper(this).deletePic("${member.fullName}_profile")
                        finish()
                        dialog.dismiss()
                    }
                    alert.setNegativeButton("NO"
                    ) { dialog, _ -> dialog.dismiss() }
                    alert.show()
                    return true
                }
                R.id.menu_add_member_event -> {
                    val intent = Intent(this, NewEventActivity::class.java)
                    intent.putExtra(TYPE, 0)
                    intent.putExtra(NAME, member.name)
                    startActivity(intent)
                }
                    R.id.menu_export_member_csv -> {
                    if (!checkPermission()) requestPermission()
                    else ImportExportHelper(this.applicationContext, dbMembers).exportCSV(
                        MEMBERS_TABLE_NAME," WHERE name = '${member.name}'",
                        "member_" + member.uid.toString() + "_" + member.name + DATABASE_CSV_SUFFIX
                    )
                }
            }
            return super.onOptionsItemSelected(item)
        }

        private fun requestPermission(){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                //Android is 11(R) or above
                try {
                    Timber.tag(MembersFragment.TAG).d("requestPermission: try")
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                    val uri = Uri.fromParts("package", this.packageName, null)
                    intent.data = uri
                    storageActivityResultLauncher.launch(intent)
                }
                catch (e: Exception){
                    Timber.tag(MembersFragment.TAG).e(e, "requestPermission: ")
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
                        MembersFragment.STORAGE_PERMISSION_CODE
                    )
                }
            }
        }

        private val storageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            Timber.tag(MembersFragment.TAG).d("storageActivityResultLauncher: ")
            //here we will handle the result of our intent
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                //Android is 11(R) or above
                if (Environment.isExternalStorageManager()){
                    //Manage External Storage Permission is granted
                    Timber.tag(MembersFragment.TAG)
                        .d("storageActivityResultLauncher: Manage External Storage Permission is granted")
                }
                else{
                    //Manage External Storage Permission is denied....
                    Timber.tag(MembersFragment.TAG)
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
            if (requestCode == MembersFragment.STORAGE_PERMISSION_CODE){
                if (grantResults.isNotEmpty()){
                    //check each permission if granted or not
                    val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
                    if (write && read){
                        //External Storage Permission granted
                        Timber.tag(MembersFragment.TAG)
                            .d("onRequestPermissionsResult: External Storage Permission granted")
                    }
                    else{
                        //External Storage Permission denied...
                        Timber.tag(MembersFragment.TAG)
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
            private const val ID = "uid"
            private const val NAME = "name"
            private const val TYPE = "type"
            private var uid = 0
            private var name = ""
        }
    }
