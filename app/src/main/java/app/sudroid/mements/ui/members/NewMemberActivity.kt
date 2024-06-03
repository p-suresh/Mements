package app.sudroid.mements.ui.members

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.MEMBERS_DATABASE_NAME
import app.sudroid.mements.R
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NewMemberActivity : AppCompatActivity(), View.OnClickListener {

    private var isEditing = false
    var id = 0
    private var type = 0

    // UI references.
    private lateinit var toolbar: Toolbar
    private lateinit var etName: TextInputEditText
    private lateinit var etFullName: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etAddress: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etNickName: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etDob: TextInputEditText
    private lateinit var etBio: TextInputEditText
    private lateinit var etFamily: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var etFacebook: TextInputEditText
    private lateinit var etWeb: TextInputEditText
    private lateinit var ivContactApp: ImageView

    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    // the DAO to access database
    private lateinit var memberDAO: MemberDAO
    private lateinit var dbMembers: MemberDatabase
    private lateinit var member: Member
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_member)

        // setup Database and get DAO
        dbMembers = databaseBuilder(this, MemberDatabase::class.java, MEMBERS_DATABASE_NAME)
            .allowMainThreadQueries()
            .createFromAsset("db/members.db")
            .build()
        memberDAO = dbMembers.memberDAO!!
        dbMembers.close()
        // initialize views
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = null
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        toolbar.run { this.setNavigationOnClickListener { finish() } }
        etName = findViewById(R.id.et_add_member_name)
        etFullName = findViewById(R.id.et_add_member_full_name)
        etPhone = findViewById(R.id.et_add_member_phone)
        etAddress = findViewById(R.id.et_add_member_address)
        etEmail = findViewById(R.id.et_add_member_email)
        etNickName = findViewById(R.id.et_add_member_nick_name)
        etDescription = findViewById(R.id.et_add_member_description)
        etDob = findViewById(R.id.et_add_member_dob)
        etBio = findViewById(R.id.et_add_member_bio)
        etFamily = findViewById(R.id.et_add_member_family)
        etContact = findViewById(R.id.et_add_member_contact)
        ivContactApp = findViewById(R.id.iv_contact_app)
        etFacebook = findViewById(R.id.et_add_member_facebook)
        etWeb = findViewById(R.id.et_add_member_web)

        btnSave = findViewById(R.id.btn_add_member_save)
        btnSave.setOnClickListener(this)
        btnCancel = findViewById(R.id.btn_add_member_cancel)
        btnCancel.setOnClickListener(this)

        etBio.setOnClickListener(this)
        etDob.setOnClickListener(this)

        // get the intent and check if it was editing existing member or create a new one
        val mIntent = intent
        if (mIntent != null) {
            id = mIntent.getIntExtra(ID, 0)
            type = mIntent.getIntExtra(TYPE, 0)
            if (type == 1) {
                isEditing = true
                member = memberDAO.getItemById(id)!!
                supportActionBar!!.title = "Edit Member"
                etName.setText(member.name)
                etFullName.setText(member.fullName)
                etPhone.setText(member.phone)
                etAddress.setText(member.address)
                etEmail.setText(member.email)
                etNickName.setText(member.nickName)
                etDescription.setText(member.description)
                etDob.setText(member.dob)
                etBio.setText(member.bio)
                etFamily.setText(member.family)
                etContact.setText(member.contact)
                etFacebook.setText(member.facebook)
                etWeb.setText(member.web)
            } else {
                supportActionBar!!.title = "Add Member"
            }
        }

        // Messaging/Contact App Pref
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        when (settings.getString("messaging_app", "WhatsApp")) {
            "WhatsApp" -> {
                ivContactApp.setImageResource(R.drawable.ic_whatsapp)
//                etSocial.setHint(R.string.whatsapp)
            }
            "Signal" -> {
                ivContactApp.setImageResource(R.drawable.ic_signal)
//                etSocial.setHint (R.string.signal)
            }
            "Telegram" -> {
                ivContactApp.setImageResource(R.drawable.ic_telegram)
//                etSocial.setHint (R.string.signal)
            }
        }
    }

    override fun onClick(view: View) {
        if (view === btnCancel) {
            // finish the activity
            finish()
        }
        if (view === btnSave) {
            // set member and check if it was valid
            setMember()
            finish()
        }
        if (view === etDob) {
            // Set DOB
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
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }
    }

    private fun isInvalid(text: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateFormat.isLenient = false
            dateFormat.parse(text)
            false
        } catch (e: ParseException){
            Timber.tag("Tag").d(e.toString())
            true
        }
    }

    private fun setMember() {
        val nameStr = etName.text.toString().trim()
        val fullNameStr = etFullName.text.toString().trim()
        val phoneStr = etPhone.text.toString().trim()
        val addressStr = etAddress.text.toString().trim()
        val emailStr = etEmail.text.toString().trim()
        val nickNameStr = etNickName.text.toString().trim()
        val descriptionStr = etDescription.text.toString().trim()
        val dobStr = etDob.text.toString().trim()
        val bioStr = etBio.text.toString().trim()
        val familyStr = etFamily.text.toString().trim()
        val contactStr = etContact.text.toString().trim()
        val facebookStr = etFacebook.text.toString().trim()
        val webStr = etWeb.text.toString().trim()

        if (dobStr.isNotEmpty()) {
            if (isInvalid(dobStr)) {
                Toast.makeText(this, "Enter a valid date in dd/mm/yyyy format", Toast.LENGTH_SHORT)
                    .show()
                return
            }
        }

        if (nameStr.isEmpty()) {
            etName.error = "cannot be empty"
            return
        }
        if (fullNameStr.isEmpty()) {
            etFullName.error = "cannot be empty"
            return
        }
        if (phoneStr.isEmpty()) {
            etPhone.error = "cannot be empty"
            return
        }
        if (addressStr.isEmpty()) {
            etAddress.error = "cannot be empty"
            return
        }

        if (isEditing) {
            val member = Member(
                member.uid, nameStr, fullNameStr, phoneStr, addressStr, emailStr, nickNameStr, descriptionStr, dobStr, bioStr, familyStr,contactStr,facebookStr,webStr)
            memberDAO.updateMember(member)
            finish()
        } else {
            val member = Member(0, nameStr, fullNameStr, phoneStr, addressStr, emailStr, nickNameStr, descriptionStr, dobStr, bioStr, familyStr,contactStr,facebookStr,webStr)
            memberDAO.insertMember(member)
            finish()
        }
    }

    companion object {
        /**
         * type to identity the intent if its for new member or to edit member
         * id instance for the intent
         */
        private const val ID = "uid"
        private const val TYPE = "type"
    }
}