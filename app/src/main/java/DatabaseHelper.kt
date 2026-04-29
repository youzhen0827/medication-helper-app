package com.example.medicinehw

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * DatabaseHelper.java
 * 資料庫輔助類別，負責建立與管理 SQLite 資料庫。
 */
class DatabaseHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_MEDICINES)
        db.execSQL(SQL_CREATE_RECORDS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS)
        onCreate(db)
    }

    fun insertMedicine(
        name: String?, dosage: Double, unit: String?,
        frequency: String?, note: String?, createdAt: String?
    ): Long {
        val db = this.getWritableDatabase()
        val cv = ContentValues()
        cv.put(COL_MED_NAME, name)
        cv.put(COL_MED_DOSAGE, dosage)
        cv.put(COL_MED_UNIT, unit)
        cv.put(COL_MED_FREQ, frequency)
        cv.put(COL_MED_NOTE, note)
        cv.put(COL_MED_CREATED, createdAt)
        return db.insert(TABLE_MEDICINES, null, cv)
    }

    fun getAllMedicines(): Cursor {
        val db = this.getReadableDatabase()
        return db.query(
            TABLE_MEDICINES, null, null, null, null, null,
            COL_MED_CREATED + " DESC"
        )
    }

    fun getMedicineById(id: Int): Cursor {
        val db = this.getReadableDatabase()
        return db.query(
            TABLE_MEDICINES, null,
            COL_MED_ID + "=?", arrayOf<String>(id.toString()),
            null, null, null
        )
    }

    fun deleteMedicine(id: Int): Int {
        val db = this.getWritableDatabase()
        db.delete(
            TABLE_RECORDS, COL_REC_MED_ID + "=?",
            arrayOf<String>(id.toString())
        )
        return db.delete(
            TABLE_MEDICINES, COL_MED_ID + "=?",
            arrayOf<String>(id.toString())
        )
    }

    fun insertRecord(
        medicineId: Int, medicineName: String?,
        takeDate: String?, timeSlot: String?,
        isTaken: Int, note: String?
    ): Long {
        val db = this.getWritableDatabase()
        val cv = ContentValues()
        cv.put(COL_REC_MED_ID, medicineId)
        cv.put(COL_REC_MED_NAME, medicineName)
        cv.put(COL_REC_DATE, takeDate)
        cv.put(COL_REC_SLOT, timeSlot)
        cv.put(COL_REC_TAKEN, isTaken)
        cv.put(COL_REC_NOTE, note)
        return db.insert(TABLE_RECORDS, null, cv)
    }

    fun getAllRecords(): Cursor {
        val db = this.getReadableDatabase()
        return db.query(
            TABLE_RECORDS, null, null, null, null, null,
            COL_REC_DATE + " DESC, " + COL_REC_SLOT + " ASC"
        )
    }

    fun getRecordsByDate(date: String?): Cursor {
        val db = this.getReadableDatabase()
        return db.query(
            TABLE_RECORDS, null,
            COL_REC_DATE + "=?", arrayOf<String?>(date),
            null, null, COL_REC_SLOT + " ASC"
        )
    }

    fun deleteRecord(id: Int): Int {
        val db = this.getWritableDatabase()
        return db.delete(
            TABLE_RECORDS, COL_REC_ID + "=?",
            arrayOf<String>(id.toString())
        )
    }

    fun countTakenByDate(date: String?): Int {
        val db = this.getReadableDatabase()
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM " + TABLE_RECORDS +
                    " WHERE " + COL_REC_DATE + "=? AND " + COL_REC_TAKEN + "=1",
            arrayOf<String?>(date)
        )
        var count = 0
        if (cursor.moveToFirst()) count = cursor.getInt(0)
        cursor.close()

        return count
    }

    companion object {
        private const val DB_NAME = "medicine.db" 
        private const val DB_VERSION = 2 

        const val TABLE_MEDICINES: String = "medicines"
        const val COL_MED_ID: String = "_id"
        const val COL_MED_NAME: String = "name"
        const val COL_MED_DOSAGE: String = "dosage"
        const val COL_MED_UNIT: String = "unit"
        const val COL_MED_FREQ: String = "frequency"
        const val COL_MED_NOTE: String = "note"
        const val COL_MED_CREATED: String = "created_at"

        const val TABLE_RECORDS: String = "take_records"
        const val COL_REC_ID: String = "_id"
        const val COL_REC_MED_ID: String = "medicine_id"
        const val COL_REC_MED_NAME: String = "medicine_name"
        const val COL_REC_DATE: String = "take_date"
        const val COL_REC_SLOT: String = "time_slot"
        const val COL_REC_TAKEN: String = "is_taken"
        const val COL_REC_NOTE: String = "note"

        private val SQL_CREATE_MEDICINES = "CREATE TABLE " + TABLE_MEDICINES + " (" +
                COL_MED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_MED_NAME + " TEXT NOT NULL, " +
                COL_MED_DOSAGE + " REAL NOT NULL, " +
                COL_MED_UNIT + " TEXT NOT NULL, " +
                COL_MED_FREQ + " TEXT NOT NULL, " +
                COL_MED_NOTE + " TEXT, " +
                COL_MED_CREATED + " TEXT NOT NULL" +
                ");"

        private val SQL_CREATE_RECORDS = "CREATE TABLE " + TABLE_RECORDS + " (" +
                COL_REC_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_REC_MED_ID + " INTEGER NOT NULL, " +
                COL_REC_MED_NAME + " TEXT NOT NULL, " +
                COL_REC_DATE + " TEXT NOT NULL, " +
                COL_REC_SLOT + " TEXT NOT NULL, " +
                COL_REC_TAKEN + " INTEGER NOT NULL DEFAULT 0, " +
                COL_REC_NOTE + " TEXT" +
                ");"
    }
}
