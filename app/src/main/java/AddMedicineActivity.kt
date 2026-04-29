package com.example.medicinehw

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * AddMedicineActivity.java
 * 新增藥物頁面：
 * - 輸入藥物名稱、劑量、單位、服用頻率、備註
 * - 按「儲存」將資料寫入 SQLite 資料庫
 * - 按「返回」回到主頁面
 */
class AddMedicineActivity : AppCompatActivity() {
    // ── UI 元件 ──────────────────────────────────────────────────────────────────
    private var etMedName: EditText? = null // 藥物名稱輸入框
    private var etDosage: EditText? = null // 劑量輸入框（數字）
    private var spUnit: Spinner? = null // 單位選擇器（顆/mg/ml/包）
    private var spFrequency: Spinner? = null // 服用頻率選擇器（每日一次/二次/三次）
    private var etNote: EditText? = null // 備註輸入框
    private var btnSave: Button? = null // 儲存按鈕
    private var btnCancel: Button? = null // 取消按鈕

    // ── 資料庫輔助物件 ──────────────────────────────────────────────────────────
    private var dbHelper: DatabaseHelper? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine) // 載入新增藥物版面

        // 初始化資料庫
        dbHelper = DatabaseHelper(this)

        // 綁定 UI 元件
        etMedName = findViewById(R.id.etMedName)
        etDosage = findViewById(R.id.etDosage)
        spUnit = findViewById(R.id.spUnit)
        spFrequency = findViewById(R.id.spFrequency)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // 設定「儲存」按鈕事件
        btnSave!!.setOnClickListener(View.OnClickListener { v: View? -> saveMedicine() })

        // 設定「取消」按鈕事件：直接關閉此 Activity
        btnCancel!!.setOnClickListener(View.OnClickListener { v: View? -> finish() })
    }

    /**
     * 驗證輸入並將藥物資料儲存至資料庫。
     */
    private fun saveMedicine() {
        // ── 讀取使用者輸入 ──
        val name = etMedName!!.getText().toString().trim { it <= ' ' }
        val dosageStr = etDosage!!.getText().toString().trim { it <= ' ' }
        val unit: String? = spUnit!!.getSelectedItem().toString() // Spinner 選取值
        val frequency: String? = spFrequency!!.getSelectedItem().toString() // Spinner 選取值
        val note = etNote!!.getText().toString().trim { it <= ' ' }

        // ── 輸入驗證 ────────────────────────────────────────────────────────────
        if (TextUtils.isEmpty(name)) {
            // 藥物名稱不能為空
            etMedName!!.setError("請輸入藥物名稱")
            etMedName!!.requestFocus()
            return
        }
        if (TextUtils.isEmpty(dosageStr)) {
            // 劑量不能為空
            etDosage!!.setError("請輸入劑量")
            etDosage!!.requestFocus()
            return
        }

        // 將劑量字串轉為 double
        val dosage: Double
        try {
            dosage = dosageStr.toDouble()
            if (dosage <= 0) throw NumberFormatException() // 劑量必須大於 0
        } catch (e: NumberFormatException) {
            etDosage!!.setError("請輸入有效的劑量（大於0）")
            etDosage!!.requestFocus()
            return
        }

        // ── 取得當前時間作為建立時間 ────────────────────────────────────────────
        val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        // ── 寫入資料庫 ──────────────────────────────────────────────────────────
        val result: Long = dbHelper!!.insertMedicine(name, dosage, unit, frequency, note, createdAt)

        if (result > 0) {
            // 新增成功
            Toast.makeText(this, "藥物「" + name + "」新增成功！", Toast.LENGTH_SHORT).show()
            finish() // 關閉此 Activity，回到 MainActivity
        } else {
            // 新增失敗
            Toast.makeText(this, "新增失敗，請再試一次", Toast.LENGTH_SHORT).show()
        }
    }

    protected override fun onDestroy() {
        super.onDestroy()
        dbHelper?.close() // 關閉資料庫連線
    }
}
