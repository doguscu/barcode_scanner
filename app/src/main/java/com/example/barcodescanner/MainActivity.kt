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
import kotlin.random.Random

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
        loadDashboardData()
        
        // Geçici: Dummy data yükle (sadece ilk çalıştırmada)
        val sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val dummyDataLoaded = sharedPreferences.getBoolean("dummy_data_loaded", false)
        if (!dummyDataLoaded) {
            loadDummyData()
            sharedPreferences.edit().putBoolean("dummy_data_loaded", true).apply()
        }
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

        // Negatif değerler için - sembolünü ₺ den önce getir
        val formattedNetIncome = if (netIncome < 0) {
            "-₺${String.format(java.util.Locale.getDefault(), "%.2f", -netIncome)}"
        } else {
            "₺${String.format(java.util.Locale.getDefault(), "%.2f", netIncome)}"
        }
        binding.textViewNetIncome.text = formattedNetIncome
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
            R.id.nav_clear_data -> {
                showClearDataConfirmationDialog()
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

    private fun showClearDataConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Verileri Temizle")
            .setMessage("Bu işlem tüm verilerinizi kalıcı olarak silecektir:\n\n• Tüm stok kayıtları\n• Tüm satış işlemleri\n• Tüm tarama geçmişi\n• Tüm bildirimler\n\nBu işlem geri alınamaz. Devam etmek istediğinizden emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                showFinalClearDataConfirmation()
            }
            .setNegativeButton("İptal", null)
            .setIcon(R.drawable.ic_delete_24)
            .show()
    }

    private fun showFinalClearDataConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Son Uyarı")
            .setMessage("TÜM VERİLERİNİZ SİLİNECEK!\n\nBu işlem geri alınamaz. Emin misiniz?")
            .setPositiveButton("EVET, SİL") { _, _ ->
                clearAllData()
            }
            .setNegativeButton("İptal", null)
            .setCancelable(false)
            .show()
    }

    private fun clearAllData() {
        try {
            // Tüm repository'leri temizle
            scanResultRepository.clearAllScanResults()
            stockItemRepository.clearAllStockItems()
            transactionRepository.clearAllTransactions()
            notificationRepository.clearAllNotifications()

            // Dashboard'ı yenile
            loadDashboardData()
            updateNotificationBadge()

            // Başarı mesajı
            Snackbar.make(binding.root, "Tüm veriler başarıyla temizlendi", Snackbar.LENGTH_LONG).show()

        } catch (e: Exception) {
            Snackbar.make(binding.root, "Veri temizleme hatası: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun loadDummyData() {
        loadDummyStockData()
        loadDummySalesData()
        
        // Dashboard'ı yenile
        loadDashboardData()
        updateNotificationBadge()
        
        Snackbar.make(binding.root, "Dummy veriler yüklendi", Snackbar.LENGTH_LONG).show()
    }

    private fun loadDummyStockData() {
        val brands = arrayOf("Ray-Ban", "Oakley", "Prada", "Gucci", "Versace", "Dior", "Tom Ford", "Persol", "Maui Jim", "Police")
        val productTypes = arrayOf(
            Transaction.PRODUCT_TYPE_GLASS,
            Transaction.PRODUCT_TYPE_FRAME, 
            Transaction.PRODUCT_TYPE_LENS
        )
        
        // 13 Eylül 2025 - 27 Eylül 2025 arası rastgele tarihler
        val startDate = java.util.Calendar.getInstance().apply {
            set(2025, 8, 13, 0, 0, 0) // Ay 0-indexed (8 = Eylül)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endDate = java.util.Calendar.getInstance().apply {
            set(2025, 8, 27, 23, 59, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

        // 20 adet dummy stok verisi
        for (i in 1..20) {
            val brand = brands.random()
            val productType = productTypes.random()
            val randomDate = (startDate..endDate).random()
            val quantity = (1..15).random()
            val price = when (productType) {
                Transaction.PRODUCT_TYPE_GLASS -> (50.0..300.0).random()
                Transaction.PRODUCT_TYPE_FRAME -> (100.0..800.0).random()
                Transaction.PRODUCT_TYPE_LENS -> (75.0..400.0).random()
                else -> (50.0..300.0).random()
            }
            
            val barcode = "DUMMY${System.nanoTime()}${i}"
            
            // StockItem ekle
            val stockItem = StockItem(
                barcode = barcode,
                brand = brand,
                purchasePrice = price,
                stockDate = randomDate,
                quantity = quantity,
                productType = productType
            )
            stockItemRepository.insertStockItem(stockItem)
            
            // Transaction ekle
            val transaction = Transaction(
                brand = brand,
                productType = productType,
                transactionType = Transaction.TYPE_STOCK_ENTRY,
                amount = price,
                barcode = barcode,
                transactionDate = randomDate,
                quantity = quantity,
                purchasePrice = price
            )
            transactionRepository.insertTransaction(transaction)
        }
    }

    private fun loadDummySalesData() {
        val brands = arrayOf("Ray-Ban", "Oakley", "Prada", "Gucci", "Versace", "Dior", "Tom Ford", "Persol", "Maui Jim", "Police")
        val productTypes = arrayOf(
            Transaction.PRODUCT_TYPE_GLASS,
            Transaction.PRODUCT_TYPE_FRAME,
            Transaction.PRODUCT_TYPE_LENS
        )
        
        // 13 Eylül 2025 - 27 Eylül 2025 arası rastgele tarihler
        val startDate = java.util.Calendar.getInstance().apply {
            set(2025, 8, 13, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val endDate = java.util.Calendar.getInstance().apply {
            set(2025, 8, 27, 23, 59, 59)
            set(java.util.Calendar.MILLISECOND, 999)
        }.timeInMillis

        // 15 adet dummy satış verisi
        for (i in 1..15) {
            val brand = brands.random()
            val productType = productTypes.random()
            val randomDate = (startDate..endDate).random()
            val quantity = (1..3).random()
            val salePrice = when (productType) {
                Transaction.PRODUCT_TYPE_GLASS -> (100.0..500.0).random()
                Transaction.PRODUCT_TYPE_FRAME -> (200.0..1200.0).random()
                Transaction.PRODUCT_TYPE_LENS -> (150.0..600.0).random()
                else -> (100.0..500.0).random()
            }
            
            val barcode = "SALE${System.nanoTime()}${i}"
            
            // Satış transaction'ı ekle
            val transaction = Transaction(
                brand = brand,
                productType = productType,
                transactionType = Transaction.TYPE_SALE,
                amount = salePrice,
                barcode = barcode,
                transactionDate = randomDate,
                quantity = quantity,
                purchasePrice = salePrice * 0.6 // %40 kar marjı
            )
            transactionRepository.insertTransaction(transaction)
            
            // Ana sayfa tarama sonucu ekle
            val scanResult = ScanResult(
                barcode = barcode,
                brand = brand,
                salePrice = salePrice,
                scanDate = randomDate
            )
            scanResultRepository.insertScanResult(scanResult)
        }
    }
}

// Extension function for ClosedRange<Double>.random()
fun ClosedRange<Double>.random(): Double {
    return Random.nextDouble(start, endInclusive)
}
