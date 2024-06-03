package app.sudroid.mements.ui.events

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery


@Dao
interface EventDAO {
    @get:Query("SELECT * FROM events ORDER BY start_date ASC")
    val allEvents: List<Event?>?

    /**
     * Get Event in database ordered by id
     *
     * @return a Event
     */
    @Query("SELECT * FROM events WHERE eid = :eid")
    fun getItemById(eid: Int): Event?


    @Query("SELECT * FROM events WHERE name = :name")
    fun getItemByName(name: String): Event?

    /**
     * Function to insert a events in room database
     *
     * @param events to be inserted in database
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertEvent(events: Event)

    /**
     * Function to Update an events in room database
     *
     * @param events the object to be Update
     */
    @Update
    fun updateEvent(events: Event)

    /**
     * Function to delete an events in room database
     *
     * @param events the object to be deleted
     */
    @Delete
    fun deleteEvent(events: Event)

    @RawQuery
    fun insertDataRawFormat(query: SupportSQLiteQuery): Boolean?
}
