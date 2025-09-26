package com.example.barcodescanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ArrayAdapter
import com.example.barcodescanner.adapter.TransactionAdapter
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.example.barcodescanner.databinding.DialogAddScanResultBinding
import com.example.barcodescanner.model.ScanResult
import com.example.barcodescanner.model.StockItem
import com.example.barcodescanner.model.Transaction
import com.example.barcodescanner.repository.ScanResultRepository
import com.example.barcodescanner.repository.StockItemRepository
import com.example.barcodescanner.repository.TransactionRepository
import com.example.barcodescanner.repository.NotificationRepository
import com.example.barcodescanner.repository.DataImportExportRepository
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionAdapter: TransactionAdapter
    private val transactionList = mutableListOf<Transaction>()
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var scanResultRepository: ScanResultRepository
    private lateinit var stockItemRepository: StockItemRepository
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var dataImportExportRepository: DataImportExportRepository
    
    // File picker launchers
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { handleExportResult(it) }
    }
    
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { handleImportResult(it) }
    }
    
    // Sorting states for recent transactions
    private var currentSortField: String? = null
    private var isAscending = true
    
    companion object {
        const val SORT_BRAND = "brand"
        const val SORT_DATE = "date"
        const val SORT_AMOUNT = "amount"
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openBarcodeScanner()
        } else {
            showPermissionDeniedMessage()
        }
    }

    private val scannerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val scannedBarcode = result.data?.getStringExtra("scanned_barcode")
            scannedBarcode?.let { barcode ->
                showAddScanResultDialog(barcode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply saved theme before calling super.onCreate
        applySelectedTheme()
        
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanResultRepository = ScanResultRepository(this)
        stockItemRepository = StockItemRepository(this)
        transactionRepository = TransactionRepository(this)
        dataImportExportRepository = DataImportExportRepository(this)
        notificationRepository = NotificationRepository(this)
        
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupScanButton()
        setupNotificationButton()
        addDummyDataIfNeeded()
        loadDashboardData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Ana Sayfa"
        supportActionBar?.title = getString(R.string.nav_home)
    }

    private fun setupDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(drawerToggle)
        drawerToggle.syncState()

        binding.navigationView.setNavigationItemSelectedListener(this)
        // Set home as checked by default
        binding.navigationView.setCheckedItem(R.id.nav_home)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactionList)
        
        binding.recyclerViewRecentTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }

    private fun setupScanButton() {
        binding.fabScanBarcode.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }

    private fun setupNotificationButton() {
        updateNotificationBadge()
        binding.buttonNotifications.setOnClickListener {
            val intent = Intent(this, NotificationsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateNotificationBadge() {
        val unreadCount = notificationRepository.getUnreadNotificationCount()
        if (unreadCount > 0) {
            binding.textViewNotificationBadge.visibility = android.view.View.VISIBLE
            binding.textViewNotificationBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
        } else {
            binding.textViewNotificationBadge.visibility = android.view.View.GONE
        }
    }

    private fun checkCameraPermissionAndScan() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openBarcodeScanner()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openBarcodeScanner() {
        val intent = Intent(this, BarcodeScannerActivity::class.java)
        scannerLauncher.launch(intent)
    }

    private fun loadDashboardData() {
        // Dashboard kartlarını güncelle - Sadece bugünkü veriler
        val totalSalesRevenue = transactionRepository.getTodaySales() // Bugünkü satış geliri
        val netIncome = transactionRepository.getTodayNetIncome() // Bugünkü net gelir (satış - alım)
        val salesCount = transactionRepository.getTodaySalesCount() // Bugünkü satış sayısı

        binding.textViewNetIncome.text = "₺${String.format(java.util.Locale.getDefault(), "%.2f", netIncome)}"
        if (netIncome > 0) {
            binding.textViewNetIncome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else if (netIncome == 0.0) {
            // Sıfır değeri için varsayılan tema rengini kullan
            val typedValue = android.util.TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnPrimaryContainer, typedValue, true)
            binding.textViewNetIncome.setTextColor(typedValue.data)
        } else {
            binding.textViewNetIncome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
        
        binding.textViewIncome.text = "₺${String.format(java.util.Locale.getDefault(), "%.2f", totalSalesRevenue)}"
        if (totalSalesRevenue > 0) {
            binding.textViewIncome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else if (totalSalesRevenue == 0.0) {
            // Sıfır değeri için varsayılan tema rengini kullan
            val typedValue = android.util.TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSecondaryContainer, typedValue, true)
            binding.textViewIncome.setTextColor(typedValue.data)
        } else {
            binding.textViewIncome.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        }
        
        binding.textViewSalesCount.text = salesCount.toString()

        // Son işlemleri yükle
        transactionList.clear()
        val recentTransactions = transactionRepository.getRecentTransactions()
        transactionList.addAll(recentTransactions)
        transactionAdapter.notifyDataSetChanged()
        updateEmptyState()
        
        // Debug için
        android.util.Log.d("MainActivity", "Transaction count: ${transactionList.size}")
        for (transaction in transactionList) {
            android.util.Log.d("MainActivity", "Transaction: ${transaction.brand} - ${transaction.productType} - ${transaction.transactionType} - ${transaction.amount}")
        }
    }

    private fun showAddScanResultDialog(barcode: String) {
        // Önce stokta bu barkodun olup olmadığını kontrol et
        val stockItem = stockItemRepository.getStockItemByBarcode(barcode)
        
        if (stockItem == null) {
            // Ürün stokta yok, uyarı ver
            AlertDialog.Builder(this)
                .setTitle(R.string.product_not_found_title)
                .setMessage(getString(R.string.product_not_in_stock_message))
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }

        val dialogBinding = DialogAddScanResultBinding.inflate(LayoutInflater.from(this))
        dialogBinding.textViewScannedBarcode.text = "${getString(R.string.barcode_label)}: $barcode"

        // Stok bilgilerini göster
        val productType = transactionRepository.getProductTypeByBarcode(barcode) ?: "Genel"
        dialogBinding.textViewStockInfo.text = "Marka: ${stockItem.brand} | Tip: $productType"

        AlertDialog.Builder(this)
            .setTitle(R.string.sales_info_title)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val salePriceText = dialogBinding.editTextSalePrice.text.toString().trim()

                if (salePriceText.isEmpty()) {
                    Snackbar.make(binding.root, R.string.enter_sale_price, Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val salePrice = salePriceText.toDouble()
                    val productTypeFromDB = transactionRepository.getProductTypeByBarcode(barcode) ?: "Genel"
                    
                    // ScanResult kaydet
                    val scanResult = ScanResult(
                        barcode = barcode,
                        brand = stockItem.brand,
                        salePrice = salePrice,
                        scanDate = System.currentTimeMillis()
                    )
                    scanResultRepository.insertScanResult(scanResult)

                    // Transaction kaydet
                    val transaction = Transaction(
                        brand = stockItem.brand,
                        productType = productTypeFromDB,
                        transactionType = Transaction.TYPE_SALE,
                        amount = salePrice,
                        barcode = barcode,
                        transactionDate = System.currentTimeMillis()
                    )
                    val id = transactionRepository.insertTransaction(transaction)
                    
                    if (id > 0) {
                        loadDashboardData()
                        Snackbar.make(binding.root, "Satış başarıyla kaydedildi", Snackbar.LENGTH_SHORT).show()
                        Snackbar.make(binding.root, R.string.sale_saved_successfully, Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, R.string.save_error, Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Snackbar.make(binding.root, R.string.enter_valid_price, Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun updateEmptyState() {
        if (transactionList.isEmpty()) {
            binding.textViewEmptyTransactions.visibility = android.view.View.VISIBLE
            binding.recyclerViewRecentTransactions.visibility = android.view.View.GONE
        } else {
            binding.textViewEmptyTransactions.visibility = android.view.View.GONE
            binding.recyclerViewRecentTransactions.visibility = android.view.View.VISIBLE
        }
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            R.string.camera_permission_required_for_scanning,
            Snackbar.LENGTH_LONG
        ).show()
    }



    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> {
                // Already on home, just close drawer
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                return true
            }
            R.id.nav_stocks -> {
                startActivity(Intent(this, StocksActivity::class.java))
            }
            R.id.nav_sales -> {
                startActivity(Intent(this, SalesActivity::class.java))
            }
            R.id.nav_calendar -> {
                startActivity(Intent(this, CalendarActivity::class.java))
            }
            R.id.nav_theme_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_import_data -> {
                startImportData()
            }
            R.id.nav_export_data -> {
                startExportData()
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun addDummyDataIfNeeded() {
            addDummyTransactions()
            addDummyStockItems()
    }

    private fun addDummyTransactions() {
        // 13-24 Eylül 2025 tarihleri için Calendar kullan
        val calendar = java.util.Calendar.getInstance()
        
        val dummyTransactions = listOf(
            Transaction(
                brand = "Samsung",
                productType = "Çerçeve",
                transactionType = Transaction.TYPE_SALE,
                amount = 850.0,
                barcode = "1234567890123",
                transactionDate = calendar.apply { set(2025, 8, 24, 14, 30) }.timeInMillis // 24 Eylül 2024
            ),
            Transaction(
                brand = "Apple",
                productType = "Cam",
                transactionType = Transaction.TYPE_SALE,
                amount = 1200.0,
                barcode = "1234567890124",
                transactionDate = calendar.apply { set(2025, 8, 23, 10, 15) }.timeInMillis // 23 Eylül 2024
            ),
            Transaction(
                brand = "Huawei",
                productType = "Çerçeve",
                transactionType = Transaction.TYPE_STOCK_ENTRY,
                amount = 450.0,
                barcode = "1234567890125",
                transactionDate = calendar.apply { set(2025, 8, 20, 16, 45) }.timeInMillis // 20 Eylül 2024
            ),
            Transaction(
                brand = "Xiaomi",
                productType = "Cam",
                transactionType = Transaction.TYPE_SALE,
                amount = 675.0,
                barcode = "1234567890126",
                transactionDate = calendar.apply { set(2025, 8, 18, 11, 20) }.timeInMillis // 18 Eylül 2024
            ),
            Transaction(
                brand = "LG",
                productType = "Lens",
                transactionType = Transaction.TYPE_STOCK_ENTRY,
                amount = 320.0,
                barcode = "1234567890127",
                transactionDate = calendar.apply { set(2025, 8, 15, 9, 10) }.timeInMillis // 15 Eylül 2024
            )
        )

        for (transaction in dummyTransactions) {
            transactionRepository.insertTransaction(transaction)
        }
    }

    private fun addDummyStockItems() {
        // 13-24 Eylül 2025 tarihleri için Calendar kullan
        val calendar = java.util.Calendar.getInstance()
        
        val dummyStockItems = listOf(
            StockItem(
                barcode = "1234567890125",
                brand = "Huawei",
                purchasePrice = 450.0,
                stockDate = calendar.apply { set(2025, 8, 20, 16, 45) }.timeInMillis, // 20 Eylül 2024
                quantity = 3
            ),
            StockItem(
                barcode = "1234567890127",
                brand = "LG",
                purchasePrice = 320.0,
                stockDate = calendar.apply { set(2025, 8, 15, 9, 10) }.timeInMillis, // 15 Eylül 2024
                quantity = 5
            ),
            StockItem(
                barcode = "1234567890128",
                brand = "Sony",
                purchasePrice = 780.0,
                stockDate = calendar.apply { set(2025, 8, 22, 13, 25) }.timeInMillis, // 22 Eylül 2024
                quantity = 2
            ),
            StockItem(
                barcode = "1234567890129",
                brand = "Nokia",
                purchasePrice = 290.0,
                stockDate = calendar.apply { set(2025, 8, 17, 8, 45) }.timeInMillis, // 17 Eylül 2024
                quantity = 4
            ),
            StockItem(
                barcode = "1234567890130",
                brand = "Oppo",
                purchasePrice = 520.0,
                stockDate = calendar.apply { set(2025, 8, 14, 15, 30) }.timeInMillis, // 14 Eylül 2024
                quantity = 1
            )
        )

        for (stockItem in dummyStockItems) {
            stockItemRepository.insertStockItem(stockItem)
            
            // Stok items için transaction da ekle
            val transaction = Transaction(
                brand = stockItem.brand,
                productType = "Çerçeve", // Default olarak çerçeve
                transactionType = Transaction.TYPE_STOCK_ENTRY,
                amount = stockItem.purchasePrice,
                barcode = stockItem.barcode,
                transactionDate = stockItem.stockDate
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    private fun applySelectedTheme() {
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val savedTheme = sharedPreferences.getString(SettingsActivity.PREF_THEME, SettingsActivity.THEME_LIGHT)
        
        when (savedTheme) {
            SettingsActivity.THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            SettingsActivity.THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            SettingsActivity.THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
        updateNotificationBadge()
        // Set home as selected when returning to main activity
        binding.navigationView.setCheckedItem(R.id.nav_home)
    }
    
    // Data Import/Export Methods
    private fun startExportData() {
        val fileName = dataImportExportRepository.generateExportFileName()
        exportLauncher.launch(fileName)
    }
    
    private fun startImportData() {
        importLauncher.launch(arrayOf("application/json", "application/octet-stream", "*/*"))
    }
    
    private fun handleExportResult(uri: Uri) {
        try {
            val result = dataImportExportRepository.exportAllData(uri)
            if (result.isSuccess) {
                Snackbar.make(binding.root, result.getOrThrow(), Snackbar.LENGTH_LONG).show()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "Dışa aktarma hatası: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    
    private fun handleImportResult(uri: Uri) {
        // Önce dosyayı doğrula
        val validationResult = dataImportExportRepository.validateImportFile(uri)
        if (validationResult.isFailure) {
            val error = validationResult.exceptionOrNull()?.message ?: "Geçersiz dosya"
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            return
        }
        
        val importData = validationResult.getOrNull()!!
        
        // Import seçenekleri dialogu göster
        showImportOptionsDialog(uri, importData)
    }
    
    private fun showImportOptionsDialog(uri: Uri, importData: com.example.barcodescanner.model.ExportData) {
        val options = arrayOf(
            "Mevcut verilerle birleştir",
            "Mevcut verileri değiştir"
        )
        
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(this)
        dialogBuilder.setTitle("İçe Aktarma Seçenekleri")
        dialogBuilder.setMessage(
            "Dosyadaki veriler:\n" +
            "• Stok: ${importData.stockItems.size} kayıt\n" +
            "• İşlem: ${importData.transactions.size} kayıt\n" +
            "• Tarama: ${importData.scanResults.size} kayıt\n\n" +
            "Nasıl içe aktarmak istiyorsunuz?"
        )
        
        dialogBuilder.setItems(options) { _, which ->
            val replaceExisting = (which == 1)
            performImport(uri, replaceExisting)
        }
        
        dialogBuilder.setNegativeButton("İptal", null)
        dialogBuilder.show()
    }
    
    private fun performImport(uri: Uri, replaceExisting: Boolean) {
        try {
            val result = dataImportExportRepository.importData(uri, replaceExisting)
            if (result.isSuccess) {
                Snackbar.make(binding.root, result.getOrThrow(), Snackbar.LENGTH_LONG).show()
                // Verileri yenile
                loadDashboardData()
                updateNotificationBadge()
            } else {
                val error = result.exceptionOrNull()?.message ?: "Bilinmeyen hata"
                Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Snackbar.make(binding.root, "İçe aktarma hatası: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
}
