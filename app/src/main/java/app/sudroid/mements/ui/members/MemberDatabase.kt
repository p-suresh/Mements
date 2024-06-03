package app.sudroid.mements.ui.members

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Member::class], version = 1, exportSchema = true)
abstract class MemberDatabase : RoomDatabase() {
    abstract val memberDAO: MemberDAO?
}