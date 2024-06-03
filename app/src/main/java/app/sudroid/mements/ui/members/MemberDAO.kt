package app.sudroid.mements.ui.members

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
interface MemberDAO {
    @get:Query("SELECT * FROM Members ORDER BY name ASC")
    val allMembers: List<Member?>?

    /**
     * Get Member in database ordered by id
     *
     * @return a Member
     */
    @Query("SELECT * FROM Members WHERE uid = :uid")
    fun getItemById(uid: Int): Member?

    @Query("SELECT * FROM Members WHERE name = :name")
    fun getItemByName(name: String): Member?

    /**
     * Function to insert a members in room database
     *
     * @param members to be inserted in database
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertMember(members: Member)

    /**
     * Function to Update an members in room database
     *
     * @param members the object to be Update
     */
    @Update
    fun updateMember(members: Member)

    /**
     * Function to delete an members in room database
     *
     * @param members the object to be deleted
     */
    @Delete
    fun deleteMember(members: Member)

    @RawQuery
    fun insertDataRawFormat(query: SupportSQLiteQuery): Boolean?
}
