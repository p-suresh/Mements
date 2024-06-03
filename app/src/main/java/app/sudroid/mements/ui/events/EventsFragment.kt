package app.sudroid.mements.ui.events

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.getExternalStorageDirectory
import android.provider.Settings
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room.databaseBuilder
import app.sudroid.mements.DATABASE_CSV_SUFFIX
import app.sudroid.mements.DATABASE_STORAGE_DIR
import app.sudroid.mements.EVENTS_DATABASE_NAME
import app.sudroid.mements.EVENTS_TABLE_NAME
import app.sudroid.mements.R
import app.sudroid.mements.databinding.FragmentEventsBinding
import app.sudroid.mements.ImportExportHelper
import app.sudroid.mements.SharedPrefFavourites
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File


class EventsFragment : Fragment() {

    /* Database level */

    private  var _binding: FragmentEventsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding: FragmentEventsBinding get() = _binding!!

    private val _type: String = "type"

    // the DAO to access database
    private lateinit var eventDAO: EventDAO
    private lateinit var dbEvents: EventDatabase

    // UI references.
    private lateinit var addevent: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var eventsAdapter: EventAdapter
//    private var searchView: SearchView? = null

    private var isFavedView = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // setup Database and get DAO
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        @Suppress("DEPRECATION")
        setHasOptionsMenu(true)

        // setup Database and get DAO
        dbEvents =
            databaseBuilder(this.requireContext(), EventDatabase::class.java, EVENTS_DATABASE_NAME)
                .allowMainThreadQueries()
    //                .createFromAsset("db/events.db")
                .build()


        eventDAO = dbEvents.eventDAO!!

        // initialize views
        addevent = binding.addEvent
        addevent.setOnClickListener{ _ ->
            // add new event
            val intent = Intent(this.activity, NewEventActivity::class.java)
            intent.putExtra(_type, 0)
            startActivity(intent)
        }
        recyclerView = binding.eventsRecyclerView
        recyclerView.setHasFixedSize(false)
        recyclerView.layoutManager = LinearLayoutManager(this.context)

        return root
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        this.activity?.menuInflater?.inflate(R.menu.events_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return true
            }
            override fun onQueryTextChange(query: String?): Boolean {
                Timber.tag("onQueryTextChange").d("query: %s", query)
                eventsAdapter.filter.filter(query)
                return true
            }
        })
        searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                eventsAdapter.filter.filter("")
                return true
            }
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }
        })
        return
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Deprecated("Deprecated in Java")
    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_event_favourite -> {
                if (!isFavedView) {
//                    Log.d("Fav", "Clicked")
                    val favouritesEidList = SharedPrefFavourites(this.requireContext(), "events", "favourites").get()
//                    Log.d("Fav List", favouritesEidList.toString())
                    var favouritesList = eventDAO.allEvents
                    if (favouritesEidList == null) {
                        favouritesList = null
                    } else {
                        if (favouritesList != null) {
                            favouritesList = favouritesList.filter {favouritesEidList.contains(it?.eid) }
                        }
                    }
                    eventsAdapter = EventAdapter(this.requireContext(), favouritesList)
                    recyclerView.adapter = eventsAdapter
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
                else ImportExportHelper(this.requireContext(), dbEvents).restoreDatabase(EVENTS_DATABASE_NAME,true)
            }
            R.id.menu_backup -> {
                if (!checkStoragePermission()) requestStoragePermission()
                else ImportExportHelper(this.requireContext(), dbEvents).backupDatabase(
                    EVENTS_DATABASE_NAME)
            }

            R.id.menu_import -> {
                if (!checkStoragePermission()) requestStoragePermission()
                //            importAllCSV()
                else {
                    var intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    val uri: Uri =
                        Uri.parse(getExternalStorageDirectory().path + File.separator + DATABASE_STORAGE_DIR + File.separator)
                    intent.setDataAndType(uri, "*/*")
                    intent.putExtra("events", "")
                    intent = Intent.createChooser(intent, "Choose CSV file ")
                    resultLauncher.launch(intent)
                }
            }
            R.id.menu_export -> {
                if (!checkStoragePermission()) requestStoragePermission()
//                exportCSV()
                else ImportExportHelper(this.requireContext(), dbEvents).exportCSV(EVENTS_TABLE_NAME,"","$EVENTS_TABLE_NAME$DATABASE_CSV_SUFFIX")
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

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
    //        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE){
            if (grantResults.isNotEmpty()){
                //check each permission if granted or not
                val write = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val read = grantResults[1] == PackageManager.PERMISSION_GRANTED

                if (write && read){
                    //External Storage Permission granted
                    Timber.tag(TAG)
                        .d("onRequestPermissionsResult: External Storage Permission granted")
                    Snackbar.make (this.requireView(), "Permission Granted, Now you can access storage.", Snackbar.LENGTH_LONG).show()
                }
                else{
                    //External Storage Permission denied...
                    Timber.tag(TAG)
                        .d("onRequestPermissionsResult: External Storage Permission denied...")
                    Snackbar.make(this.requireView(), "Permission Denied, You cannot access storage.", Snackbar.LENGTH_LONG).show()
                }
            }
        }
        if (requestCode == CONTACTS_PERMISSION_CODE){
            if (grantResults.isNotEmpty()){
                //check each permission if granted or not
                val read = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (read){
                    //Contacts Permission Permission granted
                    Timber.tag(TAG).d("onRequestPermissionsResult: Contacts Permission granted")
                    Snackbar.make (this.requireView(), "Permission Granted, Now you can access contacts.", Snackbar.LENGTH_LONG).show()
                }
                else{
                    //Contacts Permission Permission denied...
                    Timber.tag(TAG).d("onRequestPermissionsResult: Contacts Permission denied...")
                    Snackbar.make(this.requireView(), "Permission Denied, You cannot access contacts.", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val uri = data?.data
            if (uri != null) {
                Timber.tag("Importing:").d("All")
                eventDAO.let {
                    ImportExportHelper(this.requireContext(), dbEvents).importEventCSV(uri, it)
                }
            }
        }
    }

    // Can exit
    override fun onAttach(context: Context) {
        super.onAttach(context)
        val callback: OnBackPressedCallback =
            object : OnBackPressedCallback(true)
            {
                override fun handleOnBackPressed() {
                    activity?.finish()
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            callback
        )
    }

    override fun onResume() {
        super.onResume()
        // retrieve all data that was written into the database
        val eventsList = eventDAO.allEvents
        eventsList?.reversed()
        // set the data into the recycler View
        eventsAdapter = EventAdapter(this.requireContext(), eventsList)
        recyclerView.adapter = eventsAdapter
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
