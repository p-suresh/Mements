package app.sudroid.mements.ui.members

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.sudroid.mements.EVENTS_DATABASE_NAME
import app.sudroid.mements.PicHelper
import app.sudroid.mements.R
import app.sudroid.mements.SharedPrefFavourites
import app.sudroid.mements.ui.events.EventDatabase
import java.io.File
import java.util.Locale
import de.hdodenhof.circleimageview.CircleImageView
import timber.log.Timber

class MemberAdapter(private val context: Context, private val membersList: List<Member?>?) :
    RecyclerView.Adapter<MemberAdapter.ViewHolder>(), Filterable {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    //    private var membersFilteredList: MutableList<Member?> = membersList!!
    var membersFilteredList = ArrayList<Member>()

    init {
        membersFilteredList = membersList as ArrayList<Member>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.item_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // get the data by position
        val member = membersFilteredList[position]
        val favouritesList = SharedPrefFavourites(this.context, "members", "favourites").get()

        if (member.name.isNotEmpty()) {
            // get the first letter from the first name and convert it to image
            val directory = ContextWrapper(context).getDir("pp", AppCompatActivity.MODE_PRIVATE)
            val imageName = "${member.fullName}_profile.jpg"
            val file = File(directory, imageName)
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                val iStream = this.context.contentResolver.openInputStream(uri)
                val bitmapImage = BitmapFactory.decodeStream(iStream)
                iStream?.close()
                holder.civAvatar.setImageBitmap(Bitmap.createScaledBitmap(bitmapImage, 48, 48, false)) // from image
            } else {
                holder.civAvatar.setImageBitmap(PicHelper(this.context).getAvatar(this.context,1, member.name[0])) //shape 1 for circle
            }
        }
        // Swap Name and NickName if Set
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val swapNameWithNickName = settings.getBoolean("members_name_swap_toggle", false)

        if (swapNameWithNickName and (member.nickName?.isNotEmpty() == true)) {
            holder.tvName.text = member.nickName
            holder.tvNickName.text = member.name
        } else {
            holder.tvName.text = member.name
            holder.tvNickName.text = member.nickName
        }

        holder.tvFullName.text = member.fullName

        val dbEvents = Room.databaseBuilder(this.context, EventDatabase::class.java, EVENTS_DATABASE_NAME)
            .allowMainThreadQueries()
//            .createFromAsset("db/events.db")
            .build()
        val eventDAO = dbEvents.eventDAO
        val eventsList = eventDAO!!.allEvents
        var adminsList = mutableListOf<String>()
        if (eventsList != null) {
            for (i in eventsList.indices) {
                var eventAdmins = eventsList[i]?.admins
                if (eventAdmins != "") {
                    if (eventAdmins != null) {
                        if (eventAdmins.endsWith(","))
                            eventAdmins = eventAdmins.substring(0, eventAdmins.length - 1) // trailing comma removed
                    }
                }
                val admins = eventAdmins?.split(",") as MutableList // split list upon comma
                admins.removeAll(listOf(null, " ", ","))
                for (j in admins.indices)
                    admins[j].let { adminsList.add(it.trim()) }
                adminsList.remove(" ")
                Timber.tag("Admins Size:").d(adminsList.size.toString())
                Timber.tag("Admins:").d(adminsList.toString())
            }
        }

        adminsList = adminsList.distinct().toMutableList()
        if (adminsList.contains(member.name)) {
            Timber.tag("Member is admin:").d(member.name)
            holder.ivHasEvent.visibility = View.VISIBLE
        } else {
            holder.ivHasEvent.visibility = View.GONE
        }

        if (favouritesList?.contains(member.uid) == true) {
            Timber.tag("Has Memberlist:").d(favouritesList.size.toString())
            holder.ivFavourite.drawable.alpha = 250
        } else {
            holder.ivFavourite.drawable.alpha = 25
        }

        holder.layout.setOnClickListener {
            val intent = Intent(context, MemberDetailsActivity::class.java)
            intent.putExtra("uid", member.uid)
            context.startActivity(intent)
        }
        dbEvents.close()
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charSearch = constraint.toString()
                membersFilteredList = if (charSearch.isEmpty()) {
                    membersList as ArrayList<Member>
                } else {
                    val resultList = ArrayList<Member>()
                    for (row in membersList!!) {
                        if (row != null) {
                            if (row.name.lowercase(Locale.ROOT)
                                    .startsWith(constraint.toString().lowercase(Locale.ROOT))) {
                                resultList.add(row)
                                Timber.tag("Row").d(row.uid.toString())
                            }
                        }
                    }
                    resultList
                }
                val filterResults = FilterResults()
                filterResults.values = membersFilteredList
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                membersFilteredList = results?.values as ArrayList<Member>
                notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return membersFilteredList.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ui
        var civAvatar: CircleImageView = itemView.findViewById(R.id.iv_member_avatar)
        var tvName: TextView = itemView.findViewById(R.id.tv_member_name)
        var tvFullName: TextView = itemView.findViewById(R.id.tv_member_full_name)
        var tvNickName: TextView = itemView.findViewById(R.id.tv_member_nick_name)
        var ivHasEvent: ImageView = itemView.findViewById(R.id.iv_member_has_event)
        var ivFavourite: ImageView = itemView.findViewById(R.id.iv_member_favourite)
        var layout: RelativeLayout = itemView.findViewById(R.id.Members_List_Row)
    }
}
