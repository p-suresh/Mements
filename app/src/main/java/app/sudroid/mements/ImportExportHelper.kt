package app.sudroid.mements

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.ContactsContract
import android.widget.Toast
import androidx.room.RoomDatabase
import app.sudroid.mements.ui.events.Event
import app.sudroid.mements.ui.events.EventDAO
import app.sudroid.mements.ui.members.Member
import app.sudroid.mements.ui.members.MemberDAO
import au.com.bytecode.opencsv.CSVWriter
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.io.IOException
import kotlin.system.exitProcess

class ImportExportHelper(var context: Context, private var database: RoomDatabase) {

    fun exportCSV(table:String, queryAdd: String?, filePath:String){
        val exportDir = File(Environment.getExternalStorageDirectory(), DATABASE_STORAGE_DIR) // your path where you want save your file
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, filePath) //$TABLE_NAME.csv is like user.csv or any name you want to save

        try {
            file.createNewFile()
            val csvWrite = CSVWriter(FileWriter(file))
            val curCSV = database.query("SELECT * FROM $table $queryAdd", null) // query for get all data of your database table
            csvWrite.writeNext(curCSV.columnNames)
            Timber.tag("Column name:").d(curCSV.columnNames.toString())
            while (curCSV.moveToNext()) {
                //Which column you want to export
                val arrStr = arrayOfNulls<String>(curCSV.columnCount)
                for (i in 0 until curCSV.columnCount) {
                    when (i) {
                        20, 22 -> {
                        }
                        else -> arrStr[i] = curCSV.getString(i)
                    }
                }
                csvWrite.writeNext(arrStr)
            }
            csvWrite.close()
            curCSV.close()
            database.close()
            Toast.makeText(this.context,"Exported to $DATABASE_STORAGE_DIR SuccessFully!!", Toast.LENGTH_SHORT).show()
        } catch (sqlEx: Exception) {
            Timber.e(sqlEx)
        }
    }

    fun importMemberCSV(uri: Uri, memberDAO: MemberDAO){
//        // CSV can have more events
//        var itemCount = 0
//        // Get Last uid value from sqlite seq table
//        val cursor: Cursor =
//            database.query("SELECT seq FROM sqlite_sequence WHERE name=?", arrayOf(table))
//        var lastId = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        val filePath = uri.path.toString()
        val fileName = filePath.substring(filePath.lastIndexOf("/")+1)
        if (!fileName.endsWith(".csv")) {
            Toast.makeText(
                this.context,
                "${uri.path}: Not a .csv File!? Aborting",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (!fileName.startsWith("member")) {
            Toast.makeText(
                this.context,
                "${uri.path}: Not a member .csv File!? Aborting",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val inputStream = this.context.contentResolver?.openInputStream(uri)
        val bufferReader = inputStream?.bufferedReader()
        val csvFormat = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true)
        val csvParser = CSVParser(bufferReader, csvFormat.build())

        val members = if (csvParser.recordNumber > 1) {
            "Members"
        } else "Member"

        for (csvRecord in csvParser) {
            val member = Member(0, csvRecord.get(1), csvRecord.get(2), csvRecord.get(3), csvRecord.get(4), csvRecord.get(5), csvRecord.get(6), csvRecord.get(7), csvRecord.get(8), csvRecord.get(9), csvRecord.get(10), csvRecord.get(11), csvRecord.get(12), csvRecord.get(13))
            memberDAO.insertMember(member)
        }
        inputStream?.close()
        Toast.makeText(this.context, "Imported $members SuccessFully!!", Toast.LENGTH_SHORT).show()


//        }
//
//        val input = this.context.contentResolver.openInputStream(uri)
//        val csvReader = CSVReader(InputStreamReader(input))
//        var nextLine: Array<String> ?
//        var count = 0
//        val columns = StringBuilder()
//        GlobalScope.launch(Dispatchers.IO) {
//            do {
//                val value = StringBuilder()
//                nextLine = csvReader.readNext()
//                nextLine?.let {nextLine->
//                    for (i in nextLine.indices) {
//                        if (count == 0) {
//                            // the count==0 part only read
//                            Log.d("i:", "$i")
//                            Log.d("NextLine:", "$nextLine[$i]")
//                            Log.d("Count:", "$count")
//                            if (i == nextLine.size - 1) {
//                                Log.d("i:", "$i")
//                                Log.d("NextLine:", "$nextLine[$i]")
//                                Log.d("Count:", "$count")
//                                //your csv file column name
//                                columns.append(nextLine[i])
//                                count =1
//                            }
//                            else
//                                columns.append(nextLine[i]).append(",")
//                        } else {
//                            Log.d("i:", "$i")
//                            Log.d("NextLine:", "$nextLine[$i]")
//                            Log.d("Count:", "$count")                            // this part is for reading value of each row
//                            if (i == nextLine.size - 1) {
//                                value.append("'").append(nextLine[i]).append("'")
//                                Log.d("Value-$i:", nextLine[i])
//                                count = 2
//                            }
//                            else
//                                value.append("'").append(nextLine[i]).append("',")
//                        }
//                        Log.d("Column:", columns.toString())
//                        Log.d("Values:", value.toString())
//                    }
//                    if (count==2) {
//                        Log.d("push Column:", columns.toString())
//                        Log.d("push Values:", value.toString())
//                        Log.d("Last ID:", "$lastId")
//                        // Update importing event uid using lastId to avert duplicate uid conflict, if any
//                        lastId++
//                        itemCount++
////                        Log.d("Item Count:", "$itemCount")
//                        value.replace(0,value.indexOf(","), "'${lastId}'")
//                        Log.d("Values:", value.toString())
//
//                        val query = SimpleSQLiteQuery("INSERT OR IGNORE INTO $table ($columns) values($value)")
//                        try {
//                            memberDAO.insertDataRawFormat(query)
//                        }catch (sqlEx: Exception) {
//                            Timber.e(sqlEx)
//                            return@launch
//                        }
//                    }
//                }
//            }while ((nextLine)!=null)
//            Log.d("Item Count:", "$itemCount")
//            cursor.close()
//            csvReader.close()
//            database.close()
//        }
//        Toast.makeText(this.context, "Imported SuccessFully!!", Toast.LENGTH_SHORT).show()
//        // Restart If More events
//        Log.d("Item Count:", "$itemCount")
//        if (itemCount > 1) {
//            Toast.makeText(this.context, "Restarting...", Toast.LENGTH_SHORT).show()
//            val i = context.packageManager?.getLaunchIntentForPackage(this.context.packageName)
//            i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            context.startActivity(i)
//            exitProcess(0)
//        }
    }

    fun importEventCSV(uri: Uri, eventDAO: EventDAO) {
        // CSV can have more events
//        var itemCount = 0
        // Get Last uid value from sqlite seq table
//        val cursor: Cursor =
//            database.query("SELECT seq FROM sqlite_sequence WHERE name=?", arrayOf(table))
//        var lastId = if (cursor.moveToFirst()) cursor.getInt(0) else 0
        val filePath = uri.path.toString()
        val fileName = filePath.substring(filePath.lastIndexOf("/")+1)
        if (!fileName.endsWith(".csv")) {
            Toast.makeText(
                this.context,
                "${uri.path}: Not a .csv File!? Aborting",
                Toast.LENGTH_SHORT
            ).show()
            return
        } else if (!fileName.startsWith("event")) {
            Toast.makeText(
                this.context,
                "${uri.path}: Not a event .csv File!? Aborting",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val inputStream = this.context.contentResolver?.openInputStream(uri)
        val bufferReader = inputStream?.bufferedReader()
        val csvFormat = CSVFormat.Builder.create().setHeader().setSkipHeaderRecord(true)
        val csvParser = CSVParser(bufferReader, csvFormat.build())

        val events = if (csvParser.recordNumber > 1) {
            "Events"
        } else "Event"

        for (csvRecord in csvParser) {
            val event = Event(0, csvRecord.get(1), csvRecord.get(2), csvRecord.get(3), csvRecord.get(4), csvRecord.get(5), csvRecord.get(6), csvRecord.get(7), csvRecord.get(8), csvRecord.get(9))
            eventDAO.insertEvent(event)
        }
        inputStream?.close()
        Toast.makeText(this.context, "Imported $events SuccessFully!!", Toast.LENGTH_SHORT).show()


//
//        val input = this.context.contentResolver.openInputStream(uri)
//        val csvReader = CSVReader(InputStreamReader(input))
//        var nextLine: Array<String> ?
//        var count = 0
//        val columns = StringBuilder()
//        GlobalScope.launch(Dispatchers.IO) {
//            do {
//                val value = StringBuilder()
//                nextLine = csvReader.readNext()
//                nextLine?.let {nextLine->
//                    for (i in nextLine.indices) {
//                        if (count == 0) {
//                            // the count==0 part only read
//                            Log.d("i:", "$i")
//                            Log.d("NextLine:", "$nextLine[$i]")
//                            Log.d("Count:", "$count")
//                            if (i == nextLine.size - 1) {
//                                Log.d("i:", "$i")
//                                Log.d("NextLine:", "$nextLine[$i]")
//                                Log.d("Count:", "$count")
//                                //your csv file column name
//                                columns.append(nextLine[i])
//                                count =1
//                            }
//                            else
//                                columns.append(nextLine[i]).append(",")
//                        } else {
//                            Log.d("i:", "$i")
//                            Log.d("NextLine:", "$nextLine[$i]")
//                            Log.d("Count:", "$count")                            // this part is for reading value of each row
//                            if (i == nextLine.size - 1) {
//                                value.append("'").append(nextLine[i]).append("'")
//                                Log.d("Value-$i:", nextLine[i])
//                                count = 2
//                            }
//                            else
//                                value.append("'").append(nextLine[i]).append("',")
//                        }
//                        Log.d("Column:", columns.toString())
//                        Log.d("Values:", value.toString())
//                    }
//                    if (count==2) {
//                        Log.d("push Column:", columns.toString())
//                        Log.d("push Values:", value.toString())
//                        Log.d("Last ID:", "$lastId")
//                        // Update importing event uid using lastId to avert duplicate uid conflict, if any
//                        lastId++
//                        itemCount++
//                        Log.d("Item Count:", "$itemCount")
//                        value.replace(0,value.indexOf(","), "'${lastId}'")
//                        Log.d("Values:", value.toString())
//                        val query = SimpleSQLiteQuery("INSERT OR IGNORE INTO $table ($columns) values($value)")
//                        try {
//                            eventDAO.insertDataRawFormat(query)
//                        }catch (sqlEx: Exception) {
//                            Timber.e(sqlEx)
//                            return@launch
//                        }                    }
//                }
//            }while ((nextLine)!=null)
//            cursor.close()
//            csvReader.close()
//            database.close()
//        }
////    Toast.makeText(this.context, "Imported SuccessFully!!", Toast.LENGTH_SHORT).show()
////    if (restart) {
////        Toast.makeText(this.context, "Imported SuccessFully!!", Toast.LENGTH_SHORT).show()
//        // Restart If More events
//        if (itemCount > 1) {
//            Toast.makeText(this.context, "Restarting...", Toast.LENGTH_SHORT).show()
//            val i = context.packageManager?.getLaunchIntentForPackage(this.context.packageName)
//            i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//            context.startActivity(i)
//            exitProcess(0)
//        }
    }

    /**
     * Backup the database
     */
    fun backupDatabase(dbName:String): Int {
        var result = -99
//        if (instance==null) return result
        val dbFile = context.getDatabasePath(dbName)
        Timber.tag("Export DB File").d(dbFile.toString())
        val dbWalFile = File(dbFile?.path + SQLITE_WALFILE_SUFFIX)
        val dbShmFile = File(dbFile?.path + SQLITE_SHMFILE_SUFFIX)
        val bkpFile = File(/*dbFile.path*/ Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR + dbName + DATABASE_BACKUP_SUFFIX)
        val bkpWalFile = File(/*bkpFile.path*/Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR  + dbName + SQLITE_WALFILE_SUFFIX)
        val bkpShmFile = File(/*bkpFile.path*/Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR  + dbName + SQLITE_SHMFILE_SUFFIX)
        if (bkpFile.exists()) bkpFile.delete()
        if (bkpWalFile.exists()) bkpWalFile.delete()
        if (bkpShmFile.exists()) bkpShmFile.delete()
        try {
            dbFile.copyTo(bkpFile,true)
            Timber.tag("Export File").d(bkpFile.toString())
            if (dbWalFile.exists()) dbWalFile.copyTo(bkpWalFile,true)
            if (dbShmFile.exists()) dbShmFile.copyTo(bkpShmFile, true)
            result = 0
        } catch (e: IOException) {
            e.printStackTrace()
        }
        database.close()
        if (result == 0) {
            Toast.makeText(this.context, "Backed Up to $DATABASE_STORAGE_DIR SuccessFully!!", Toast.LENGTH_SHORT).show()
        }
        return result
    }
    /**
     *  Restore the database and then restart the App
     */
    fun restoreDatabase(dbName:String, restart: Boolean = true) {
        if(!File(Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR + dbName + DATABASE_BACKUP_SUFFIX).exists()) {
            Toast.makeText(this.context, "No Backup File Found for Restore?! Aborting...,", Toast.LENGTH_SHORT).show()
            return
        }
//        if (instance == null) return
        val dbpath = database.openHelper.readableDatabase.path
        val dbFile = dbpath?.let { File(it) }
        Timber.tag("Import DB File").d("%s%s", dbpath.toString(), dbFile)
        val dbWalFile = File(dbFile?.path + SQLITE_WALFILE_SUFFIX)
        val dbShmFile = File(dbFile?.path + SQLITE_SHMFILE_SUFFIX)
        val bkpFile = File(/*dbFile.path*/ Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR + dbName + DATABASE_BACKUP_SUFFIX)
        val bkpWalFile = File(Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR  + dbName + SQLITE_WALFILE_SUFFIX)
        val bkpShmFile = File(Environment.getExternalStorageDirectory().path + DATABASE_STORAGE_DIR  + dbName + SQLITE_SHMFILE_SUFFIX)
        dbShmFile.delete() /* ADDED for VACUUM BACKUP */
        dbWalFile.delete() /* ADDED for VACUUM BACKUP */
        try {
            if (dbFile != null) {
                bkpFile.copyTo(dbFile, true)
            }
            Timber.tag("Import File").d("%s%s", dbpath.toString(), bkpFile)
            if (bkpWalFile.exists()) bkpWalFile.copyTo(dbWalFile, true)
            if (bkpShmFile.exists()) bkpShmFile.copyTo(dbShmFile,true)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        database.close()
        if (restart) {
            Toast.makeText(this.context, "Restored SuccessFully!! Restarting App", Toast.LENGTH_LONG).show()
            val i = context.packageManager.getLaunchIntentForPackage(context.packageName)
            i!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(i)
            exitProcess(0)
        }
    }


    @SuppressLint("Range")
//    @RequiresApi(Build.VERSION_CODES.R)
    fun importVCF(uri: Uri): Member {
        val cursor1 = this.context.contentResolver?.query(uri, null, null, null, null)
        var fullName = "[full name]"
        var phone = "[number]"
        val address = "[address]"
//        var contactThumbnail: String? = null
//        val email: String? = null

        if (cursor1 != null) {
            if (cursor1.moveToFirst()) {
                //get contact details
                val contactId: String =
                    cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts._ID))
                fullName =
                    cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
//                contactThumbnail =
//                    cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI))
                val idResults: String =
                    cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))
                val idResultHold = idResults.toInt()

                if (idResultHold == 1) {
                    val cursor2 = this.context.contentResolver?.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId,
                        null,
                        null
                    )
                    //a contact may have multiple phone numbers
                    if (cursor2 != null) {
                        while (cursor2.moveToNext()) {
                            //get phone number
                            phone =
                                cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            //set details
//                            contactTv.append("\nPhone: $contactNumber")
//                            //before setting image, check if have or not
//                            if (contactThumnail != null) {
//                                thumbnailIv.setImageURI(Uri.parse(contactThumnail))
//                            } else {
//                                thumbnailIv.setImageResource(R.drawable.ic_person)
//                            }
                        }
                    }
                    cursor2?.close()
                }
                cursor1.close()
            }
        }
        fullName = fullName.replace("[^a-zA-Z0-9\\p{L}\\p{M} ]".toRegex(), "")
        val names = fullName.split("\\s+".toRegex())
        var name = if (names.size > 1)
            names.dropLast(1).joinToString()
        else fullName
        name = name.replace(",", "") // Remove comma char
        //        val query = SimpleSQLiteQuery("INSERT OR IGNORE INTO members values (0, $name, \"$fullName\", $phone ,$address)")
//        database.eventDAO?.insertEvent(event)
//        database.query(query)
//
//        if (contactThumbnail != null) {
        return Member(0, name, fullName, phone, address, "", "", "", "", "", "", "", "", "")
    }
}