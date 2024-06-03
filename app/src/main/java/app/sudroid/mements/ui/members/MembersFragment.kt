package app.sudroid.mements.ui.members

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.provider.ContactsContract
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.DATABASE_CSV_SUFFIX
import app.sudroid.mements.DATABASE_STORAGE_DIR
import app.sudroid.mements.PicHelper
import app.sudroid.mements.R
import app.sudroid.mements.MEMBERS_DATABASE_NAME
import app.sudroid.mements.MEMBERS_TABLE_NAME
import app.sudroid.mements.databinding.FragmentMembersBinding
import app.sudroid.mements.ImportExportHelper
import app.sudroid.mements.SharedPrefFavourites
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File

class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding: FragmentMembersBinding get() = _binding!!

    private val _type: String = "type"

    // the DAO to access database
    private lateinit var memberDAO: MemberDAO
    private lateinit var dbMembers: MemberDatabase

    // UI references.
    private lateinit var addMember: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var membersAdapter: MemberAdapter
//    private var searchView: SearchView? = null

    private var isFavedView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // setup Database and get DAO
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        val root: View = binding.root
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        // setup dbMembers and get DAO
        dbMembers =
            databaseBuilder(this.requireContext(), MemberDatabase::class.java, MEMBERS_DATABASE_NAME)
                .allowMainThreadQueries()
                .createFromAsset("db/members.db")
                .build()

        memberDAO = dbMembers.memberDAO!!
        dbMembers.close()

        // initialize views
        addMember = binding.addMember
        addMember.setOnClickListener{ _ ->
            // add new member
            val intent = Intent(this.activity, NewMemberActivity::class.java)
            intent.putExtra(_type, 0)
            startActivity(intent)
        }
        recyclerView = binding.RecyclerView
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(this.context)

        return root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.activity?.menuInflater?.inflate(R.menu.members_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(query: String?): Boolean {
                Timber.tag("onQueryTextChange").d("query: %s", query)
                membersAdapter.filter.filter(query)
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                membersAdapter.filter.filter("")
                return true
            }
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }
        })
        return
    }

    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_favourite -> {
                if (!isFavedView) {
                    Timber.tag("Fav").d("Clicked")
                    val favouritesUidList = SharedPrefFavourites(this.requireContext(), "members", "favourites").get()
                    Timber.tag("Fav List").d(favouritesUidList.toString())
                    var favouritesList = memberDAO.allMembers
                    if (favouritesUidList == null) {
                        favouritesList = null
                    } else {
                        if (favouritesList != null) {
                            favouritesList = favouritesList.filter {favouritesUidList.contains(it?.uid) }
                        }
                    }
                    membersAdapter = MemberAdapter(this.requireContext(), favouritesList)
                    recyclerView.adapter = membersAdapter
                    recyclerView.adapter?.notifyDataSetChanged()
                    item.setIcon(R.drawable.ic_star_filled)
                } else {
                    isFavedView = false
                    item.setIcon(R.drawable.ic_star)
                    onResume()
                }
                isFavedView = !item.isChecked
                item.isChecked = isFavedView
            }
            R.id.menu_restore -> {
                if (!checkStoragePermission()) requestStoragePermission()
                else ImportExportHelper(this.requireContext(), dbMembers).restoreDatabase(
                    MEMBERS_DATABASE_NAME,true)
            }
            R.id.menu_backup -> {
                if (!checkStoragePermission()) requestStoragePermission()
                else ImportExportHelper(this.requireContext(), dbMembers).backupDatabase(
                    MEMBERS_DATABASE_NAME)
            }
            R.id.menu_import_member -> {
                if (!checkContactPermission()) requestContactPermission()
                else {
                    val intent = Intent(
                        Intent.ACTION_PICK,
                        ContactsContract.Contacts.CONTENT_URI
                    )
                    resultLauncher1.launch(intent)
                }
            }
            R.id.menu_import -> {
                if (!checkStoragePermission()) requestStoragePermission()
                //            importAllCSV()
                else {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    val uri: Uri =
                        Uri.parse(getExternalStorageDirectory().path + File.separator + DATABASE_STORAGE_DIR + File.separator)
                    intent.setDataAndType(uri, "*/*")
                    intent.putExtra("members", "")
                    intent = Intent.createChooser(intent, "Choose CSV file ")
                    resultLauncher2.launch(intent)
                }
            }
            R.id.menu_export -> {
                if (!checkStoragePermission()) requestStoragePermission()
//                exportCSV()
                else ImportExportHelper(this.requireContext(), dbMembers).exportCSV(MEMBERS_TABLE_NAME,"","$MEMBERS_DATABASE_NAME$DATABASE_CSV_SUFFIX")
            }
        }
        return false
    }

    private fun requestStoragePermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            try {
                Timber.tag(TAG).d("requestPermission: try")
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                val uri = Uri.fromParts("package", this.requireActivity().packageName, null)
                intent.data = uri
                storageActivityResultLauncher.launch(intent)
            }
            catch (e: Exception){
                Timber.tag(TAG).e(e, "requestPermission: ")
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                storageActivityResultLauncher.launch(intent)
            }
        }
        else{
            //Android is below 11(R)
            this.activity?.let {
                requestPermissions(
                    it,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    //    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestContactPermission() {
        //        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
        if (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this.requireActivity(),
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(this.requireContext())
                builder.setTitle("Read Contacts permission")
                builder.setPositiveButton(android.R.string.ok, null)
                builder.setMessage("Please enable access to contacts.")
//                builder.setOnDismissListener {
//                    requestPermissions(
//                        arrayOf(Manifest.permission.READ_CONTACTS),
//                        CONTACTS_PERMISSION_CODE
//                    )
//                }
                builder.show()
            } else {
                requestPermissions(
                    this.requireActivity(), arrayOf(Manifest.permission.READ_CONTACTS),
                    CONTACTS_PERMISSION_CODE
                )
            }
        }
    }

    private val storageActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        Timber.tag(TAG).d("storageActivityResultLauncher: ")
        //here we will handle the result of our intent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            if (Environment.isExternalStorageManager()){
                //Manage External Storage Permission is granted
                Timber.tag(TAG)
                    .d("storageActivityResultLauncher: Manage External Storage Permission is granted")
                Snackbar.make (this.requireView(), "Permission Granted, Now you can access storage.", Snackbar.LENGTH_LONG).show()
            }
            else{
                //Manage External Storage Permission is denied....
                Timber.tag(TAG)
                    .d("storageActivityResultLauncher: Manage External Storage Permission is denied....")
                Snackbar.make(this.requireView(), "Permission Denied, You cannot access storage.", Snackbar.LENGTH_LONG).show()
            }
        }
        else{
            //Android is below 11(R)
        }
    }

    private fun checkStoragePermission(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            Environment.isExternalStorageManager()
        }
        else{
            //Android is below 11(R)
            val write = this.context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE) }
            val read = this.context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_EXTERNAL_STORAGE) }

            write == PackageManager.PERMISSION_GRANTED && read == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkContactPermission(): Boolean{
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //Android is 11(R) or above
            (ContextCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED )
        }
        else{
            //Android is below 11(R)
            val read = this.context?.let { ContextCompat.checkSelfPermission(it, Manifest.permission.READ_CONTACTS) }
            read == PackageManager.PERMISSION_GRANTED
        }
    }




//    @Deprecated("Deprecated in Java")
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        //        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == STORAGE_PERMISSION_CODE){
//            if (grantResults.isNotEmpty()){
//                //check each permission if granted or not
//                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
//                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED
//
//                if (write && read){
//                    //External Storage Permission granted
//                    Log.d(TAG, "onRequestPermissionsResult: External Storage Permission granted")
//                    Snackbar.make (this.requireView(), "Permission Granted, Now you can access storage.", Snackbar.LENGTH_LONG).show()
//                }
//                else{
//                    //External Storage Permission denied...
//                    Log.d(TAG, "onRequestPermissionsResult: External Storage Permission denied...")
//                    Snackbar.make(this.requireView(), "Permission Denied, You cannot access storage.", Snackbar.LENGTH_LONG).show()
//                }
//            }
//        }
//        if (requestCode == CONTACTS_PERMISSION_CODE){
//            if (grantResults.isNotEmpty()){
//                //check each permission if granted or not
//                val read = grantResults[0] == PackageManager.PERMISSION_GRANTED
//
//                if (read){
//                    //Contacts Permission Permission granted
//                    Log.d(TAG, "onRequestPermissionsResult: Contacts Permission granted")
//                    Snackbar.make (this.requireView(), "Permission Granted, Now you can access contacts.", Snackbar.LENGTH_LONG).show()
//                }
//                else{
//                    //Contacts Permission Permission denied...
//                    Log.d(TAG, "onRequestPermissionsResult: Contacts Permission denied...")
//                    Snackbar.make(this.requireView(), "Permission Denied, You cannot access contacts.", Snackbar.LENGTH_LONG).show()
//                }
//            }
//        }
//    }
//
//    private fun toast(message: String){
//        Toast.makeText(this.context, message, Toast.LENGTH_SHORT).show()
//    }

    @SuppressLint("Range")
    private var resultLauncher1 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val uri = data?.data
//            val filePath = data?.data?.path
            if (uri != null) {
                Timber.tag("Importing:").d("One")
                val member = ImportExportHelper(this.requireContext(), dbMembers).importVCF(uri)
                memberDAO.insertMember(member)
                val cursor = this.context?.contentResolver?.query(uri, null, null, null, null)
                var memberProfilePic: String? = null
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        memberProfilePic =
                            cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                    }
                }

                if (memberProfilePic != null) {
                    Timber.tag("Profile Pic:").d(memberProfilePic)
                    this.context?.let {
                        PicHelper(this.requireContext()).savePic(
                            Uri.parse(
                                memberProfilePic
                            ), "${member.fullName}_profile"
                        )
                    }
                }
                cursor?.close()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private var resultLauncher2 = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                Timber.tag("Importing:").d("All")
//                importCSV(uri)
                memberDAO.let {
                    ImportExportHelper(this.requireContext(), dbMembers).importMemberCSV(uri,it)
                }
            }
            membersAdapter.notifyDataSetChanged()
            this.onResume()
        }
    }

//
//    private fun importCSV(uri: Uri) {
//        val filePath = uri.path.toString()
//        val fileName = filePath.substring(filePath.lastIndexOf("/")+1)
//        if (!fileName.endsWith(".csv")) {
//            Toast.makeText(
//                this.context,
//                "${uri.path}: Not a .csv File!? Aborting",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        } else if (!fileName.startsWith("member")) {
//            Toast.makeText(
//                this.context,
//                "${uri.path}: Not a member .csv File!? Aborting",
//                Toast.LENGTH_SHORT
//            ).show()
//            return
//        }
//        val inputStream = this.context?.contentResolver?.openInputStream(uri)
//        val bufferReader = inputStream?.bufferedReader()
//        val csvParser = CSVParser(bufferReader, CSVFormat.DEFAULT)
//        for (csvRecord in csvParser) {
//            val member = Member(0, csvRecord.get(1), csvRecord.get(2), csvRecord.get(3), csvRecord.get(4), csvRecord.get(5), csvRecord.get(6), csvRecord.get(7), csvRecord.get(8), csvRecord.get(9), csvRecord.get(10), csvRecord.get(11), csvRecord.get(12), csvRecord.get(13))
//            memberDAO.insertMember(member)
//        }
//        inputStream?.close()
//    }

    override fun onResume() {
        super.onResume()
        // retrieve all data that was written into the database
        val membersList = memberDAO.allMembers
        membersList?.reversed()
        // set the data into the recycler View
        membersAdapter = MemberAdapter(this.requireContext(), membersList)
        recyclerView.adapter = membersAdapter
    }

    companion object{
        //PERMISSION request constant, assign any value
        const val STORAGE_PERMISSION_CODE = 100
        const val CONTACTS_PERMISSION_CODE =101
        const val TAG = "PERMISSION:"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
