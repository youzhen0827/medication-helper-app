package com.example.medicinehw

import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Button
import android.widget.DatePicker
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

/**
 * QueryActivity.java
 * 查詢服用記錄頁面：
 * - 可選擇日期篩選（DatePickerDialog）
 * - 顯示該日期所有服用記錄（ListView）
 * - 顯示當日已服用筆數統計
 * - 長按列表項目可刪除記錄
 * - 按「查詢全部」顯示所有日期的記錄
 */
class QueryActivity : AppCompatActivity() {
    // ── UI 元件 ──────────────────────────────────────────────────────────────────
    private var tvQueryDate: TextView? = null // 顯示已選查詢日期
    private var btnPickDate: Button? = null // 選擇日期按鈕
    private var btnQueryAll: Button? = null // 查詢全部按鈕
    private var listViewRecords: ListView? = null // 記錄清單
    private var tvEmpty: TextView? = null // 無資料提示文字
    private var tvStats: TextView? = null // 統計資訊（當日已服用筆數）

    // ── 資料庫輔助物件 ──────────────────────────────────────────────────────────
    private var dbHelper: DatabaseHelper? = null

    // ── Cursor Adapter ──────────────────────────────────────────────────────────
    private var adapter: SimpleCursorAdapter? = null
    private var recordsCursor: Cursor? = null

    // ── 目前查詢的日期（空字串代表查詢全部） ─────────────────────────────────
    private var currentDate = ""

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_query) // 載入查詢版面

        // 初始化資料庫
        dbHelper = DatabaseHelper(this)

        // 綁定 UI 元件
        tvQueryDate = findViewById(R.id.tvQueryDate)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnQueryAll = findViewById(R.id.btnQueryAll)
        listViewRecords = findViewById(R.id.listViewRecords)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvStats = findViewById(R.id.tvStats)

        // 預設顯示今天的記錄
        val cal = Calendar.getInstance()
        currentDate = String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        tvQueryDate!!.setText("查詢日期：" + currentDate)
        loadRecords(currentDate) // 載入今天的記錄

        // ── 選擇日期按鈕：開啟 DatePickerDialog ────────────────────────────────
        btnPickDate!!.setOnClickListener(View.OnClickListener { v: View? ->
            val c = Calendar.getInstance()
            val dialog = DatePickerDialog(
                this,
                OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
                    currentDate = String.format(
                        Locale.getDefault(),
                        "%04d-%02d-%02d", year, month + 1, dayOfMonth
                    )
                    tvQueryDate!!.setText("查詢日期：" + currentDate)
                    loadRecords(currentDate) // 載入所選日期的記錄
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()
        })

        // ── 查詢全部按鈕 ────────────────────────────────────────────────────────
        btnQueryAll!!.setOnClickListener(View.OnClickListener { v: View? ->
            currentDate = "" // 空字串代表不篩選日期
            tvQueryDate!!.setText("查詢日期：全部")
            loadRecords(null) // 傳 null 表示查詢全部
        })

        // ── 長按列表項目 → 確認刪除 ────────────────────────────────────────────
        listViewRecords!!.setOnItemLongClickListener(OnItemLongClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            showDeleteDialog(id.toInt())
            true
        })
    }

    /**
     * 從資料庫讀取服用記錄並更新 ListView。
     * @param date 日期字串（yyyy-MM-dd），傳 null 則查詢全部
     */
    private fun loadRecords(date: String?) {
        // 關閉舊 Cursor
        if (recordsCursor != null && !recordsCursor!!.isClosed()) {
            recordsCursor!!.close()
        }

        // 依日期查詢或查詢全部
        if (date != null && !date.isEmpty()) {
            recordsCursor = dbHelper?.getRecordsByDate(date)
        } else {
            recordsCursor = dbHelper?.getAllRecords()
        }

        // 更新統計資訊
        updateStats(date)

        if (recordsCursor == null || recordsCursor!!.getCount() == 0) {
            // 無資料：顯示提示、隱藏清單
            tvEmpty!!.setVisibility(View.VISIBLE)
            listViewRecords!!.setVisibility(View.GONE)
        } else {
            // 有資料：顯示清單
            tvEmpty!!.setVisibility(View.GONE)
            listViewRecords!!.setVisibility(View.VISIBLE)

            // 設定 Cursor Adapter：資料庫欄位 → 版面 TextView
            val fromColumns = arrayOf<String?>(
                DatabaseHelper.COL_REC_MED_NAME,
                DatabaseHelper.COL_REC_DATE,
                DatabaseHelper.COL_REC_SLOT,
                DatabaseHelper.COL_REC_TAKEN,
                DatabaseHelper.COL_REC_NOTE
            )
            val toViews = intArrayOf(
                R.id.tvRecMedName,
                R.id.tvRecDate,
                R.id.tvRecSlot,
                R.id.tvRecTaken,
                R.id.tvRecNote
            )

            if (adapter == null) {
                adapter = SimpleCursorAdapter(
                    this,
                    R.layout.item_record,  // 每個記錄項目版面
                    recordsCursor,
                    fromColumns,
                    toViews,
                    0
                )
                // 自訂 ViewBinder：將資料庫 0/1 轉為中文「已服用」/「未服用」
                adapter!!.setViewBinder { view, cursor, columnIndex ->
                    if (view!!.id == R.id.tvRecTaken) {
                        val isTaken = cursor!!.getInt(columnIndex)
                        val tv = view as TextView
                        if (isTaken == 1) {
                            tv.text = "✅ 已服用"
                            tv.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                        } else {
                            tv.text = "❌ 未服用"
                            tv.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                        }
                        true // 已自行處理，不讓 adapter 再設定
                    } else {
                        false // 其他欄位由 adapter 預設處理
                    }
                }
                listViewRecords!!.setAdapter(adapter)
            } else {
                adapter!!.changeCursor(recordsCursor)
            }
        }
    }

    /**
     * 更新統計文字（顯示當日已服用/未服用筆數）
     */
    private fun updateStats(date: String?) {
        if (date != null && !date.isEmpty()) {
            val takenCount = dbHelper?.countTakenByDate(date) ?: 0
            tvStats!!.setText("當日已服用：" + takenCount + " 筆")
            tvStats!!.setVisibility(View.VISIBLE)
        } else {
            tvStats!!.setVisibility(View.GONE) // 查詢全部時不顯示統計
        }
    }

    /**
     * 顯示刪除服用記錄的確認對話方塊
     */
    private fun showDeleteDialog(recordId: Int) {
        AlertDialog.Builder(this)
            .setTitle("刪除記錄")
            .setMessage("確定要刪除此服用記錄嗎？")
            .setPositiveButton("確定刪除", { _, _ ->
                val rows = dbHelper?.deleteRecord(recordId) ?: 0
                if (rows > 0) {
                    Toast.makeText(this, "記錄已刪除", Toast.LENGTH_SHORT).show()
                    // 重新載入（維持目前的查詢條件）
                    if (currentDate.isEmpty()) {
                        loadRecords(null)
                    } else {
                        loadRecords(currentDate)
                    }
                } else {
                    Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show()
                }
            })
            .setNegativeButton("取消", null)
            .show()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        if (recordsCursor != null && !recordsCursor!!.isClosed()) {
            recordsCursor!!.close()
        }
        dbHelper?.close()
    }
}
