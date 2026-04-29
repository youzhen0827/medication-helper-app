package com.example.medicinehw

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleCursorAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * MainActivity.java
 * 主畫面（首頁）：
 * - 顯示所有已登錄的藥物清單（ListView）
 * - 提供「新增藥物」按鈕，跳轉至 AddMedicineActivity
 * - 提供「新增服用記錄」按鈕，跳轉至 AddRecordActivity
 * - 提供「查詢記錄」按鈕，跳轉至 QueryActivity
 * - 長按藥物列表項目可刪除藥物
 */
class MainActivity : AppCompatActivity() {
    // ── UI 元件 ──────────────────────────────────────────────────────────────────
    private var listViewMedicines: ListView? = null // 顯示藥物清單的 ListView
    private var tvEmpty: TextView? = null // 清單為空時顯示的提示文字
    private var btnAddMedicine: Button? = null // 新增藥物按鈕
    private var btnAddRecord: Button? = null // 新增服用記錄按鈕
    private var btnQuery: Button? = null // 查詢記錄按鈕

    // ── 資料庫輔助物件 ──────────────────────────────────────────────────────────
    private var dbHelper: DatabaseHelper? = null

    // ── Cursor Adapter（將資料庫 Cursor 資料顯示於 ListView） ─────────────────
    private var adapter: SimpleCursorAdapter? = null
    private var medicinesCursor: Cursor? = null

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // 載入主畫面版面

        // 初始化資料庫輔助物件
        dbHelper = DatabaseHelper(this)

        // 綁定 UI 元件
        listViewMedicines = findViewById(R.id.listViewMedicines)
        tvEmpty = findViewById(R.id.tvEmpty)
        btnAddMedicine = findViewById(R.id.btnAddMedicine)
        btnAddRecord = findViewById(R.id.btnAddRecord)
        btnQuery = findViewById(R.id.btnQuery)

        // 設定按鈕點擊事件
        btnAddMedicine!!.setOnClickListener(View.OnClickListener { v: View? ->
            // 跳轉至新增藥物頁面
            val intent: Intent = Intent(this@MainActivity, AddMedicineActivity::class.java)
            startActivity(intent)
        })

        btnAddRecord!!.setOnClickListener(View.OnClickListener { v: View? ->
            // 跳轉至新增服用記錄頁面
            val intent: Intent = Intent(this@MainActivity, AddRecordActivity::class.java)
            startActivity(intent)
        })

        btnQuery!!.setOnClickListener(View.OnClickListener { v: View? ->
            // 跳轉至查詢記錄頁面
            val intent: Intent = Intent(this@MainActivity, QueryActivity::class.java)
            startActivity(intent)
        })

        // 長按列表項目 → 確認刪除藥物
        listViewMedicines!!.setOnItemLongClickListener(
            OnItemLongClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
                // id 即為該列的 _id（SQLite 主鍵）
                showDeleteDialog(id.toInt())
                true // 消費此事件，不再向上傳遞
            })

        // 載入藥物清單
        loadMedicineList()
    }

    /**
     * 每次 Activity 回到前景時重新載入清單，
     * 確保新增/刪除後資料同步更新。
     */
    protected override fun onResume() {
        super.onResume()
        loadMedicineList()
    }

    /**
     * 從資料庫讀取所有藥物並更新 ListView。
     * 若清單為空則顯示提示文字。
     */
    private fun loadMedicineList() {
        // 先關閉舊 Cursor 以避免資源洩漏
        if (medicinesCursor != null && !medicinesCursor!!.isClosed()) {
            medicinesCursor!!.close()
        }

        // 查詢資料庫取得所有藥物
        medicinesCursor = dbHelper?.getAllMedicines()

        if (medicinesCursor == null || medicinesCursor!!.getCount() == 0) {
            // 清單為空：顯示提示、隱藏 ListView
            tvEmpty!!.setVisibility(View.VISIBLE)
            listViewMedicines!!.setVisibility(View.GONE)
        } else {
            // 有資料：顯示 ListView、隱藏提示
            tvEmpty!!.setVisibility(View.GONE)
            listViewMedicines!!.setVisibility(View.VISIBLE)

            // 設定欄位對應：資料庫欄位 → 畫面 TextView id
            val fromColumns = arrayOf<String?>(
                DatabaseHelper.COL_MED_NAME,
                DatabaseHelper.COL_MED_DOSAGE,
                DatabaseHelper.COL_MED_UNIT,
                DatabaseHelper.COL_MED_FREQ
            )
            val toViews = intArrayOf(
                R.id.tvMedName,
                R.id.tvMedDosage,
                R.id.tvMedUnit,
                R.id.tvMedFreq
            )

            if (adapter == null) {
                // 首次建立 adapter
                adapter = SimpleCursorAdapter(
                    this,
                    R.layout.item_medicine,  // 每個列表項目的版面
                    medicinesCursor,
                    fromColumns,
                    toViews,
                    0
                )
                listViewMedicines!!.setAdapter(adapter)
            } else {
                // 已存在 adapter：更新資料來源
                adapter!!.changeCursor(medicinesCursor)
            }
        }
    }

    /**
     * 顯示刪除確認對話方塊
     * @param medicineId 要刪除的藥物 id
     */
    private fun showDeleteDialog(medicineId: Int) {
        AlertDialog.Builder(this)
            .setTitle("刪除藥物")
            .setMessage("確定要刪除此藥物嗎？\n相關服用記錄也會一併刪除。")
            .setPositiveButton("確定刪除", { dialog, which ->
                val rows: Int = dbHelper?.deleteMedicine(medicineId) ?: 0
                if (rows > 0) {
                    Toast.makeText(this, "藥物已刪除", Toast.LENGTH_SHORT).show()
                    loadMedicineList() // 重新載入清單
                } else {
                    Toast.makeText(this, "刪除失敗", Toast.LENGTH_SHORT).show()
                }
            })
            .setNegativeButton("取消", null) // 點取消不做任何事
            .show()
    }

    protected override fun onDestroy() {
        super.onDestroy()
        // Activity 銷毀時關閉 Cursor 與資料庫，釋放資源
        if (medicinesCursor != null && !medicinesCursor!!.isClosed()) {
            medicinesCursor!!.close()
        }
        dbHelper?.close()
    }
}
