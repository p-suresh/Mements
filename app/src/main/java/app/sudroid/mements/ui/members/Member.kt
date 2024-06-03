package app.sudroid.mements.ui.members

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity class to store in Room Database
 */
@Entity(tableName = "Members")
class Member(
    /**
     * id that auto generate
     */
    @JvmField @field:PrimaryKey(autoGenerate = true) var uid: Int,
    @JvmField @field:ColumnInfo("name",2) var name: String,
    @JvmField @field:ColumnInfo("full_name",2) var fullName: String,
    @JvmField @field:ColumnInfo("phone",1) var phone: String,
    @JvmField @field:ColumnInfo("address",2) var address: String,
    @JvmField @field:ColumnInfo("email",2) var email: String?,
    @JvmField @field:ColumnInfo("nick_name",2) var nickName: String?,
    @JvmField @field:ColumnInfo("description",2) var description: String?,
    @JvmField @field:ColumnInfo("dob",1) var dob: String?,
    @JvmField @field:ColumnInfo("bio",2) var bio: String?,
    @JvmField @field:ColumnInfo("family",2) var family: String?,
    @JvmField @field:ColumnInfo("contact",1) var contact: String?,
    @JvmField @field:ColumnInfo("facebook",2) var facebook: String?,
    @JvmField @field:ColumnInfo("web",2) var web: String?,
)