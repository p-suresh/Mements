package app.sudroid.mements.ui.events

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class to store in Room Database
 */
@Entity(tableName = "Events")
class Event(
    /**
     * id that auto generate
     */
    @JvmField @field:PrimaryKey(autoGenerate = true) var eid: Int,
    @JvmField @field:ColumnInfo("name",2) var name: String,
    @JvmField @field:ColumnInfo("details",2) var details: String,
    @JvmField @field:ColumnInfo("place",2) var place: String,
    @JvmField @field:ColumnInfo("venue",2) var venue: String,
    @JvmField @field:ColumnInfo("admins",2) var admins: String?,
    @JvmField @field:ColumnInfo("participants",2) var participnts: String?,
    @JvmField @field:ColumnInfo("all_day",2) var allDay: String,
    @JvmField @field:ColumnInfo("start_date",2) var startDate: String,
//    @JvmField @field:ColumnInfo("start_time",2) var startTime: String,
    @JvmField @field:ColumnInfo("end_date",2) var endDate: String,
//    @JvmField @field:ColumnInfo("end_time",2) var endTime: String,

)