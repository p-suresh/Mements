package app.sudroid.mements

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import app.sudroid.mements.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import timber.log.Timber


const val EVENTS_DATABASE_NAME = "events.db"
const val MEMBERS_DATABASE_NAME = "members.db"
const val DATABASE_BACKUP_SUFFIX = "-bkp"
const val DATABASE_CSV_SUFFIX = ".csv"
const val SQLITE_WALFILE_SUFFIX = "-wal"
const val SQLITE_SHMFILE_SUFFIX = "-shm"
var DATABASE_STORAGE_DIR = "/Download/Mements/"
var MEMBER_SWAP_NAME_WITH_NICKNAME = false
const val EVENTS_TABLE_NAME = "events"
const val MEMBERS_TABLE_NAME = "members"

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = PreferenceManager.getDefaultSharedPreferences(this)
        val editor: SharedPreferences.Editor = settings.edit()
        val firstRun: Boolean = settings.getBoolean("firstrun", true) // For First Time Run

        if (firstRun) {
            Timber.tag("Running: ").i("first time")
            editor.putBoolean("firstrun", false)
            editor.apply()
            editor.commit()
            val intent = Intent(
                this,
                FirstRunActivity::class.java
            )
            startActivity(intent)
        } else {
            Timber.tag("Running: ").i("not first time")
        }

        when (settings.getString("theme", "System")) {
            "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_members, R.id.nav_events
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Read Navigation Header from Preferences
        val header: View = navView.getHeaderView(0)
        val title = header.findViewById<View>(R.id.textView1) as TextView
        title.text = settings.getString("title", resources.getString(R.string.nav_header_title))
        val subtitle = header.findViewById<View>(R.id.textView2) as TextView
        subtitle.text = settings.getString("subtitle", resources.getString(R.string.nav_header_subtitle))

        // Handle BackPress
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    val id: Int? = navController.currentDestination?.id
                    if ((id == R.id.nav_events)) // these can finish
                        finish()
                    else {
                        //Removing this callback
                        remove()
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
//        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
