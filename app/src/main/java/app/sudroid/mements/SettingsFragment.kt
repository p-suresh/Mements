package app.sudroid.mements

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.*
import app.sudroid.mements.databinding.FragmentSettingsBinding
import app.sudroid.mements.ui.members.NewMemberActivity
import com.google.android.material.navigation.NavigationView

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding: FragmentSettingsBinding  get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        parentFragmentManager.beginTransaction().replace(R.id.container, PreferenceFragment()).commit()
    }

    class PreferenceFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.settings, rootKey)

            val sharedPreferences = preferenceManager.sharedPreferences
            val settingsEditor = sharedPreferences?.edit()

            val dbStorageDir = findPreference<EditTextPreference>("db_storage_dir")
            val headerTitle = findPreference<EditTextPreference>("title")
            val headerSubtitle = findPreference<EditTextPreference>("subtitle")

            val nameSwapToggle =
                findPreference<SwitchPreference>("members_name_swap_toggle")

            val prefTheme = findPreference<ListPreference>("theme")
            val prefAbout = findPreference<Preference>("about")

            dbStorageDir!!.setOnPreferenceChangeListener { _, newValue ->
                if (newValue.toString() != "") {
                    settingsEditor?.putString("db_storage_dir", newValue as String )
                    settingsEditor?.apply()
                    DATABASE_STORAGE_DIR = "/Download/$newValue"
                }
                true
            }

            headerTitle!!.setOnPreferenceChangeListener { _, newValue ->
                val navigationView = activity?.findViewById<NavigationView>(R.id.nav_view)
                val headerView = navigationView?.getHeaderView(0)
                val title = headerView?.findViewById<View>(R.id.textView1) as TextView
                title.text = newValue.toString()
                true
            }

            headerSubtitle!!.setOnPreferenceChangeListener { _, newValue ->
                val navigationView = activity?.findViewById<NavigationView>(R.id.nav_view)
                val headerView = navigationView?.getHeaderView(0)
                val subTitle = headerView?.findViewById<View>(R.id.textView2) as TextView
                subTitle.text = newValue.toString()
                true
            }

            nameSwapToggle!!.setOnPreferenceChangeListener { _, newValue ->
                val swapName = newValue as Boolean
                MEMBER_SWAP_NAME_WITH_NICKNAME = if (swapName) {
                    settingsEditor?.putBoolean("members_name_swap_toggle", true)
                    true
                } else {
                    settingsEditor?.putBoolean("members_name_swap_toggle", false)
                    false
                }
                settingsEditor?.apply()
                true
            }

            prefTheme!!.setOnPreferenceChangeListener { _, newValue ->
                settingsEditor?.putString("theme", newValue as String)
                settingsEditor?.apply()
                when (newValue) {
                    "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "System" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }
            prefAbout!!.setOnPreferenceClickListener {
                val intent = Intent(this.context, AboutActivity::class.java)
                startActivity(Intent.createChooser(intent, "About"))
                true
            }
        }
    }
}
