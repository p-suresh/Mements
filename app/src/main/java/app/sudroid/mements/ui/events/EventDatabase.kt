package app.sudroid.mements.ui.events

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Event::class], version = 1, exportSchema = true)
abstract class EventDatabase : RoomDatabase() {
    abstract val eventDAO: EventDAO?
}