package app.sudroid.mements

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.lang.reflect.Type

class SharedPrefFavourites(context: Context, prefName:String, private val keyName:String) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(prefName,
        AppCompatActivity.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    fun get():ArrayList<Int?>? {
        val arrayList: ArrayList<Int?>?
        val json = sharedPreferences.getString(keyName, null)
        val type: Type = object : TypeToken<ArrayList<Int?>?>() {}.type
        arrayList = if (json?.isNotEmpty() == true) {
            Gson().fromJson(json, type)
        } else {
            ArrayList()
        }
        return arrayList
    }
    fun add(uid:Int, arrayList: ArrayList<Int?>?) {
        uid.let { arrayList?.add(it) }
        Timber.tag("Adding to:").d(arrayList.toString())
        save(arrayList)
    }
    fun remove(uid:Int, arrayList: ArrayList<Int?>?) {
        arrayList!!.remove(uid)
        Timber.tag("Removing From:").d(arrayList.toString())
        save(arrayList)
    }
    private fun save(arrayList: ArrayList<Int?>?) {
        val json: String = Gson().toJson(arrayList)
        Timber.tag("Saving json:").d(json)
        editor.putString(keyName, json)
        editor.apply()
    }
}