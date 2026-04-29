package com.example.medicinehw

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

/**
 * AddRecordActivity.java
 * 新增服用記錄頁面：
 * - 從 Spinner 選擇藥物（從資料庫讀取）
 * - 點擊日期欄位開啟 DatePickerDialog 選取服用日期
 * - 從 Spinner 選擇時段（早/午/晚/睡前）
 * - 從 Spinner 選擇是否已服用（已服用/未服用）
 * - 輸入備註
 * - 按「儲存」寫入記錄
 */
class AddRecordActivity : AppCompatActivity() {
    // ── UI 元件 ──────────────────────────────────────────────────────────────────
    private var spMedicine: Spinner? = null // 藥物選擇器（從資料庫動態載入）
    private var tvDate: TextView? = null // 顯示所選日期
    private var spTimeSlot: Spinner? = null // 時段選擇器（早/午/晚/睡前）
    private var spIsTaken: Spinner? = null // 是否服用選擇器
    private var etNote: EditText? = null // 備註輸入框
    private var btnPickDate: Button? = null // 選擇日期按鈕
    private var btnSave: Button? = null // 儲存按鈕
    private var btnCancel: Button? = null // 取消按鈕

    // ── 資料庫輔助物件 ──────────────────────────────────────────────────────────
    private var dbHelper: DatabaseHelper? = null

    // ── 藥物清單（用於 Spinner 顯示） ─────────────────────────────────────────
    private val medicineNames: MutableList<String> = ArrayList<String>() // 藥物名稱清單
    private val medicineIds: MutableList<Int?> = ArrayList<Int?>() // 對應藥物 id 清單

    // ── 已選取的日期字串 ────────────────────────────────────────────────────────
    private var selectedDate = ""

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_record) // 載入新增記錄版面

        // 初始化資料庫
        dbHelper = DatabaseHelper(this)

        // 綁定 UI 元件
        spMedicine = findViewById(R.id.spMedicine)
        tvDate = findViewById(R.id.tvDate)
        spTimeSlot = findViewById(R.id.spTimeSlot)
        spIsTaken = findViewById(R.id.spIsTaken)
        etNote = findViewById(R.id.etNote)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // 載入藥物清單至 Spinner
        loadMedicinesIntoSpinner()

        // 預設日期為今天
        val cal = Calendar.getInstance()
        selectedDate = String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,  // 月份從 0 起算，需 +1
            cal.get(Calendar.DAY_OF_MONTH)
        )
        tvDate!!.setText(selectedDate)

        // ── 設定選擇日期按鈕事件（開啟 DatePickerDialog） ──────────────────────
        btnPickDate!!.setOnClickListener(View.OnClickListener { v: View? ->
            val c = Calendar.getInstance()
            // 建立日期選擇對話方塊，預設顯示今天
            val dialog = DatePickerDialog(
                this,
                OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                    // 使用者選取日期後更新 selectedDate 與 tvDate
                    selectedDate = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d", year, month + 1, dayOfMonth
                    )
                    tvDate!!.setText(selectedDate)
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()
        })

        // 設定「儲存」按鈕事件
        btnSave!!.setOnClickListener(View.OnClickListener { v: View? -> saveRecord() })

        // 設定「取消」按鈕事件
        btnCancel!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }

    /**
     * 從資料庫讀取所有藥物名稱，填入 Spinner。
     * 若無藥物則提示使用者先新增。
     */
    private fun loadMedicinesIntoSpinner() {
        medicineNames.clear()
        medicineIds.clear()

        val cursor: Cursor? = dbHelper?.getAllMedicines()
        if (cursor == null || cursor.getCount() == 0) {
            // 尚未新增任何藥物
            Toast.makeText(this, "尚未有藥物資料，請先新增藥物", Toast.LENGTH_LONG).show()
            cursor?.close()
            finish() // 關閉頁面，返回主畫面
            return
        }

        // 逐筆讀取藥物名稱與 id
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_NAME))
            val unit = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_UNIT))
            val dosage =
                cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_MED_DOSAGE))
            medicineIds.add(id)
            // Spinner 顯示格式：「藥名 劑量 單位」
            medicineNames.add(name + "  " + dosage + " " + unit)
        }
        cursor.close()

        // 建立 ArrayAdapter 並套用至 Spinner
        val adapter: ArrayAdapter<String?> = ArrayAdapter<String?>(
            this, android.R.layout.simple_spinner_item, medicineNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spMedicine!!.setAdapter(adapter)
    }

    /**
     * 驗證輸入並將服用記錄寫入資料庫。
     */
    private fun saveRecord() {
        // ── 讀取使用者選擇/輸入 ──
        val selectedIndex = spMedicine!!.getSelectedItemPosition()
        val medicineId: Int = medicineIds.get(selectedIndex)!!
        // 取得純藥物名稱（去除劑量顯示部分）
        val fullName = medicineNames.get(selectedIndex)
        val medicineName: String? = fullName.split("  ".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[0] // 藥名在第一個「  」之前
        val timeSlot = spTimeSlot!!.getSelectedItem().toString()
        val isTakenStr = spIsTaken!!.getSelectedItem().toString()
        val note = etNote!!.getText().toString().trim { it <= ' ' }
        val isTaken = if (isTakenStr == "已服用") 1 else 0

        // ── 驗證日期 ────────────────────────────────────────────────────────────
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "請選擇服用日期", Toast.LENGTH_SHORT).show()
            return
        }

        // ── 寫入資料庫 ──────────────────────────────────────────────────────────
        val result: Long = dbHelper?.insertRecord(
            medicineId, medicineName, selectedDate, timeSlot, isTaken, note
        ) ?: -1L

        if (result > 0) {
            val status = if (isTaken == 1) "已服用" else "未服用"
            Toast.makeText(
                this,
                "記錄新增成功！\n" + medicineName + " " + selectedDate + " " + timeSlot + " " + status,
                Toast.LENGTH_LONG
            ).show()
            finish() // 返回主頁面
        } else {
            Toast.makeText(this, "記錄新增失敗，請再試一次", Toast.LENGTH_SHORT).show()
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        dbHelper?.close()
    }
}
