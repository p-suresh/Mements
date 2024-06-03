package app.sudroid.mements

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText

class FirstRunActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var etTitle:TextInputEditText
    private lateinit var etSubtitle:TextInputEditText
    private lateinit var btnOK:Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_time_run)

        etTitle = findViewById(R.id.et_title)
        etSubtitle = findViewById(R.id.et_subtitle)
        btnOK = findViewById(R.id.btn_ok)
        btnOK.setOnClickListener(this)
        // Defaults
        etTitle.setText(R.string.nav_header_title)
        etSubtitle.setText(R.string.nav_header_subtitle)
    }

    override fun onClick(view: View?) {
        if (view === btnOK) {
            // Set Group Title and Details
            val settings = PreferenceManager.getDefaultSharedPreferences(this)
            val editor: SharedPreferences.Editor = settings.edit()
            editor.putString("title", etTitle.text.toString())
            editor.putString("subtitle", etSubtitle.text.toString())
            editor.apply()
            editor.commit()
            // Update Header too
            val navigationView = this.findViewById<NavigationView>(R.id.nav_view)
            val headerView = navigationView?.getHeaderView(0)
            val title = headerView?.findViewById<View>(R.id.textView1) as TextView
            val subTitle = headerView.findViewById<View>(R.id.textView2) as TextView
            title.text = etTitle.text.toString()
            subTitle.text = etSubtitle.text.toString()

            finish()
        }
    }
}
