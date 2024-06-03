package app.sudroid.mements.ui.events

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
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.sudroid.mements.MEMBERS_DATABASE_NAME
import app.sudroid.mements.PicHelper
import app.sudroid.mements.R
import app.sudroid.mements.SharedPrefFavourites
import app.sudroid.mements.ui.members.MemberDatabase
import com.google.android.material.imageview.ShapeableImageView
import timber.log.Timber
import java.io.File
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(private val context: Context, private val eventsList: List<Event?>?) :
    RecyclerView.Adapter<EventAdapter.ViewHolder>(), Filterable {
    private val mInflater: LayoutInflater = LayoutInflater.from(context)
    //    private var eventsFilteredList: MutableList<Event?> = eventsList!!
    var eventsFilteredList = ArrayList<Event>()

    init {
        eventsFilteredList = eventsList as ArrayList<Event>
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = mInflater.inflate(R.layout.item_event, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // get the data by position
        val event = eventsFilteredList[position]
        val favouritesList = SharedPrefFavourites(this.context, "events", "favourites").get()

        if (event.name.isNotEmpty()) {
            // get the first letter from the first name and convert it to image
            val directory = ContextWrapper(context).getDir("pp", AppCompatActivity.MODE_PRIVATE)
            val imageName = "${event.name}_event.jpg"
            val file = File(directory, imageName)
            if (file.exists()) {
                val uri = Uri.fromFile(file)
                val iStream = this.context.contentResolver.openInputStream(uri)
                val bitmapImage = BitmapFactory.decodeStream(iStream)
                iStream?.close()
                holder.sivAvatar.setImageBitmap(Bitmap.createScaledBitmap(bitmapImage, 48, 48, false))
            } else {
                holder.sivAvatar.setImageBitmap(PicHelper(this.context).getAvatar(this.context,2, event.name[0])) //shape 2 for squrcle
            }
        }

        holder.tvName.text = event.name
        holder.tvDetails.text = event.details

        var dateFormat = SimpleDateFormat("dd-MM-yyyy-HH-mm-ss.SSSZ", Locale.getDefault())
        val startDate = try {
            dateFormat.parse(event.startDate)
        } catch (e: ParseException){
            Timber.tag("Tag").d(e.toString())
            true
        }
//
//        dateFormat = if (event.allDay == "false") {
//            SimpleDateFormat("dd/MM/yyyy\nhh:mm a", Locale.getDefault())
//        } else {
//            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
//        }
        dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvStartDateTime.text = startDate?.let { dateFormat.format(it) }

        val dbMembers = Room.databaseBuilder(this.context, MemberDatabase::class.java, MEMBERS_DATABASE_NAME)
            .allowMainThreadQueries()
            .createFromAsset("db/members.db")
            .build()
        val memberDAO = dbMembers.memberDAO
        val members = memberDAO!!.allMembers
        dbMembers.close()
        val membersList = mutableListOf<String>()
        if (members != null) {
            for (i in members.indices) {
                members[i]?.name?.let { membersList.add(it) }
            }
        }
        var admins = event.admins
        if (admins != null) {
            if (admins.endsWith(",")) {
                admins = admins.substring(0, admins.length - 1) //strip trailing comma
            }
        }

        val adminList = admins?.split(",") as MutableList
        adminList.removeAll(listOf(null, " ", ","))

        Timber.tag("Admin list").d(adminList.toString())
        Timber.tag("Admin size").d(adminList.size.toString())

        if (adminList.isNotEmpty()) {
            adminList.forEach {
                if (membersList.contains(it.trim())) {
                    Timber.tag("Admin in  list").d(adminList.toString())
                    holder.ivHasAdmin.visibility = View.VISIBLE
                } else {
                    Timber.tag("Admin not in list").d(adminList.toString())
                    holder.ivHasAdmin.visibility = View.GONE
                }
            }
        }

        if (favouritesList?.contains(event.eid) == true) {
            Timber.tag("Has eventlist:").d(favouritesList.size.toString())
            holder.ivFavourite.drawable.alpha = 250
        } else {
            holder.ivFavourite.drawable.alpha = 25
        }

        holder.rlLayout.setOnClickListener {
            val intent = Intent(context, EventDetailsActivity::class.java)
            intent.putExtra("eid", event.eid)
            context.startActivity(intent)
        }
    }


override fun getFilter(): Filter {
    return object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val charSearch = constraint.toString()
            eventsFilteredList = if (charSearch.isEmpty()) {
                eventsList as ArrayList<Event>
            } else {
                val resultList = ArrayList<Event>()
                for (row in eventsList!!) {
                    if (row != null) {
                        if (row.name.lowercase(Locale.ROOT)
                                .startsWith(constraint.toString().lowercase(Locale.ROOT))) {
                            resultList.add(row)
                            Timber.tag("Row").d(row.eid.toString())
                        }
                    }
                }
                resultList
            }
            val filterResults = FilterResults()
            filterResults.values = eventsFilteredList
            return filterResults
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            eventsFilteredList = results?.values as ArrayList<Event>
            notifyDataSetChanged()
        }
    }
}

override fun getItemCount(): Int {
    return eventsFilteredList.size
}

inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    // ui
    var sivAvatar: ShapeableImageView
    var tvName: TextView
    var tvStartDateTime: TextView
    var tvDetails: TextView
    var ivHasAdmin: ImageView
    var ivFavourite: ImageView
    var rlLayout: RelativeLayout

    init {
        rlLayout = itemView.findViewById(R.id.events_list_row)
        sivAvatar = itemView.findViewById(R.id.iv_event_avatar)
        tvName = itemView.findViewById(R.id.tv_event_name)
        tvStartDateTime = itemView.findViewById(R.id.tv_event_start_date_time)
        tvDetails = itemView.findViewById(R.id.tv_event_details)
        ivHasAdmin = itemView.findViewById(R.id.iv_event_has_admin)
        ivFavourite = itemView.findViewById(R.id.iv_event_favourite)
    }
}
}
