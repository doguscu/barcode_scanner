package com.example.barcodescanner

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ArrayAdapter
import com.example.barcodescanner.adapter.StockItemAdapter
import com.example.barcodescanner.databinding.ActivityStocksBinding
import com.example.barcodescanner.databinding.DialogAddStockItemBinding
import com.example.barcodescanner.databinding.DialogAdvancedFilterBinding
import com.example.barcodescanner.model.StockItem
import com.example.barcodescanner.model.Transaction
import com.example.barcodescanner.repository.StockItemRepository
import com.example.barcodescanner.repository.TransactionRepository
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

class StocksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStocksBinding
    private lateinit var stockItemAdapter: StockItemAdapter
    private val stockItemList = mutableListOf<StockItem>()
    private val allStocksList = mutableListOf<StockItem>()
    private lateinit var stockItemRepository: StockItemRepository
    private lateinit var transactionRepository: TransactionRepository
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR"))
    
    // Sorting states
    private var currentSortField: String? = null
    private var isAscending = true
    
    companion object {
        const val SORT_BRAND = "brand"
        const val SORT_PRODUCT_TYPE = "product_type"
        const val SORT_QUANTITY = "quantity"
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
                showAddStockItemDialog(barcode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStocksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stockItemRepository = StockItemRepository(this)
        transactionRepository = TransactionRepository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupScanButton()
        setupFilterButton()
        setupSortHeaders()
        loadStockItems()
        loadProductTypeCounts()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Stoklar"
        }
    }

    private fun setupRecyclerView() {
        stockItemAdapter = StockItemAdapter(
            context = this,
            stockItems = stockItemList,
            onItemClick = { stockItem ->
                showStockItemDetails(stockItem)
            },
            onItemLongClick = { stockItem ->
                showStockItemContextMenu(stockItem)
            }
        )
        
        binding.recyclerViewStockBarcodes.apply {
            layoutManager = LinearLayoutManager(this@StocksActivity)
            adapter = stockItemAdapter
        }
    }

    private fun setupScanButton() {
        binding.fabAddStock.setOnClickListener {
            checkCameraPermissionAndScan()
        }
    }

    private fun setupFilterButton() {
        binding.buttonFilterStocks.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupSortHeaders() {
        binding.headerStockBrand.setOnClickListener {
            sortBy(SORT_BRAND)
        }
        
        binding.headerStockProductType.setOnClickListener {
            sortBy(SORT_PRODUCT_TYPE)
        }
        
        binding.headerStockQuantity.setOnClickListener {
            sortBy(SORT_QUANTITY)
        }
        
        binding.headerStockDate.setOnClickListener {
            sortBy(SORT_DATE)
        }
        
        binding.headerStockAmount.setOnClickListener {
            sortBy(SORT_AMOUNT)
        }
    }

    private fun sortBy(field: String) {
        if (currentSortField == field) {
            // Same field clicked, toggle order
            isAscending = !isAscending
        } else {
            // New field clicked, default to ascending
            currentSortField = field
            isAscending = true
        }

        applySorting()
    }

    private fun applySorting() {
        val sortedList = when (currentSortField) {
            SORT_BRAND -> {
                if (isAscending) {
                    stockItemList.sortedBy { it.brand.lowercase() }
                } else {
                    stockItemList.sortedByDescending { it.brand.lowercase() }
                }
            }
            SORT_PRODUCT_TYPE -> {
                if (isAscending) {
                    stockItemList.sortedBy { it.productType.lowercase() }
                } else {
                    stockItemList.sortedByDescending { it.productType.lowercase() }
                }
            }
            SORT_QUANTITY -> {
                if (isAscending) {
                    stockItemList.sortedBy { it.quantity }
                } else {
                    stockItemList.sortedByDescending { it.quantity }
                }
            }
            SORT_DATE -> {
                if (isAscending) {
                    stockItemList.sortedBy { it.stockDate }
                } else {
                    stockItemList.sortedByDescending { it.stockDate }
                }
            }
            SORT_AMOUNT -> {
                if (isAscending) {
                    stockItemList.sortedBy { it.purchasePrice }
                } else {
                    stockItemList.sortedByDescending { it.purchasePrice }
                }
            }
            else -> stockItemList
        }
        
        stockItemList.clear()
        stockItemList.addAll(sortedList)
        stockItemAdapter.notifyDataSetChanged()
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

    private fun loadStockItems() {
        stockItemList.clear()
        allStocksList.clear()
        val allStocks = stockItemRepository.getAllStockItems()
        allStocksList.addAll(allStocks)
        stockItemList.addAll(allStocks)
        stockItemAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun loadProductTypeCounts() {
        // Tüm stok öğelerini al
        val allStocks = stockItemRepository.getAllStockItems()
        
        // Ürün tiplerine göre toplam miktarları hesapla
        var glassCount = 0
        var frameCount = 0
        var lensCount = 0
        var totalCount = 0
        
        for (stockItem in allStocks) {
            val quantity = stockItem.quantity
            totalCount += quantity
            
            when (stockItem.productType) {
                Transaction.PRODUCT_TYPE_GLASS -> glassCount += quantity
                Transaction.PRODUCT_TYPE_FRAME -> frameCount += quantity
                Transaction.PRODUCT_TYPE_LENS -> lensCount += quantity
            }
        }

        // UI'ı güncelle
        binding.textViewGlassCount.text = glassCount.toString()
        binding.textViewFrameCount.text = frameCount.toString()
        binding.textViewLensCount.text = lensCount.toString()
        binding.textViewTotalCount.text = totalCount.toString()
    }

    private fun showAddStockItemDialog(barcode: String) {
        val dialogBinding = DialogAddStockItemBinding.inflate(LayoutInflater.from(this))
        dialogBinding.textViewScannedBarcode.text = "Barkod: $barcode"

        // Ürün tipi dropdown'ını hazırla
        val productTypes = arrayOf(
            Transaction.PRODUCT_TYPE_GLASS,
            Transaction.PRODUCT_TYPE_FRAME,
            Transaction.PRODUCT_TYPE_LENS
        )
        val productTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productTypes)
        dialogBinding.spinnerProductType.setAdapter(productTypeAdapter)

        AlertDialog.Builder(this)
            .setTitle("Stok Bilgileri")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val brand = dialogBinding.editTextBrand.text.toString().trim()
                val purchasePriceText = dialogBinding.editTextPurchasePrice.text.toString().trim()
                val quantityText = dialogBinding.editTextQuantity.text.toString().trim()
                val productType = dialogBinding.spinnerProductType.text.toString().trim()

                if (brand.isEmpty() || purchasePriceText.isEmpty() || quantityText.isEmpty() || productType.isEmpty()) {
                    Snackbar.make(binding.root, "Tüm alanları doldurun", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val purchasePrice = purchasePriceText.toDouble()
                    val quantity = quantityText.toInt()
                    
                    if (quantity <= 0) {
                        Snackbar.make(binding.root, "Adet 0'dan büyük olmalı", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    // StockItem kaydet
                    val stockItem = StockItem(
                        barcode = barcode,
                        brand = brand,
                        purchasePrice = purchasePrice,
                        stockDate = System.currentTimeMillis(),
                        quantity = quantity,
                        productType = productType
                    )
                    stockItemRepository.insertStockItem(stockItem)

                    // Transaction kaydet
                    val transaction = Transaction(
                        brand = brand,
                        productType = productType,
                        transactionType = Transaction.TYPE_STOCK_ENTRY,
                        amount = purchasePrice,
                        barcode = barcode,
                        transactionDate = System.currentTimeMillis()
                    )
                    val id = transactionRepository.insertTransaction(transaction)
                    
                    if (id > 0) {
                        loadStockItems()
                        loadProductTypeCounts()
                        Snackbar.make(binding.root, "Stok başarıyla kaydedildi", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "Kaydetme hatası", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Snackbar.make(binding.root, "Geçerli bir fiyat girin", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun updateEmptyState() {
        if (stockItemList.isEmpty()) {
            binding.textViewEmptyState.visibility = android.view.View.VISIBLE
            binding.layoutTableHeaders.visibility = android.view.View.GONE
            binding.recyclerViewStockBarcodes.visibility = android.view.View.GONE
        } else {
            binding.textViewEmptyState.visibility = android.view.View.GONE
            binding.layoutTableHeaders.visibility = android.view.View.VISIBLE
            binding.recyclerViewStockBarcodes.visibility = android.view.View.VISIBLE
        }
    }

    private fun showPermissionDeniedMessage() {
        Snackbar.make(
            binding.root,
            "Kamera izni barkod tarama için gereklidir",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showStockItemDetails(stockItem: StockItem) {
        val message = "Marka: ${stockItem.brand}\n" +
                "Alış Fiyatı: ₺${String.format(java.util.Locale.getDefault(), "%.2f", stockItem.purchasePrice)}\n" +
                "Barkod: ${stockItem.barcode}\n" +
                "Tarih: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(stockItem.stockDate))}"
        
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun showStockItemContextMenu(stockItem: StockItem) {
        val options = arrayOf("Ayrıntıları Gör", "Sil")
        
        AlertDialog.Builder(this)
            .setTitle(stockItem.brand)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showStockItemDetailDialog(stockItem)
                    1 -> showDeleteStockItemDialog(stockItem)
                }
            }
            .show()
    }

    private fun showStockItemDetailDialog(stockItem: StockItem) {
        val productType = transactionRepository.getProductTypeByBarcode(stockItem.barcode) ?: "Genel"
        val message = "Marka: ${stockItem.brand}\n" +
                "Ürün Tipi: $productType\n" +
                "Adet: ${stockItem.quantity}\n" +
                "Alış Fiyatı: ₺${String.format(java.util.Locale.getDefault(), "%.2f", stockItem.purchasePrice)}\n" +
                "Barkod: ${stockItem.barcode}\n" +
                "Tarih: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(stockItem.stockDate))}"
        
        AlertDialog.Builder(this)
            .setTitle("Stok Ayrıntıları")
            .setMessage(message)
            .setPositiveButton("Tamam", null)
            .show()
    }

    private fun showDeleteStockItemDialog(stockItem: StockItem) {
        AlertDialog.Builder(this)
            .setTitle("Stok Ürününü Sil")
            .setMessage("${stockItem.brand} ürününü stoktan silmek istediğinize emin misiniz?")
            .setPositiveButton("Sil") { _, _ ->
                deleteStockItem(stockItem)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteStockItem(stockItem: StockItem) {
        val success = stockItemRepository.deleteStockItem(stockItem.id)
        if (success) {
            loadStockItems()
            loadProductTypeCounts()
            Snackbar.make(binding.root, "Ürün stoktan silindi", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Silme işlemi başarısız", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun showEditStockItemDialog(stockItem: StockItem) {
        val dialogBinding = DialogAddStockItemBinding.inflate(LayoutInflater.from(this))
        dialogBinding.textViewScannedBarcode.text = "Barkod: ${stockItem.barcode}"

        // Mevcut verileri dialog'a yükle
        dialogBinding.editTextBrand.setText(stockItem.brand)
        dialogBinding.editTextQuantity.setText(stockItem.quantity.toString())
        dialogBinding.editTextPurchasePrice.setText(stockItem.purchasePrice.toString())

        // Ürün tipi dropdown'ını hazırla
        val productTypes = arrayOf(
            Transaction.PRODUCT_TYPE_GLASS,
            Transaction.PRODUCT_TYPE_FRAME,
            Transaction.PRODUCT_TYPE_LENS
        )
        val productTypeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productTypes)
        dialogBinding.spinnerProductType.setAdapter(productTypeAdapter)

        // Mevcut ürün tipini seç
        val currentProductType = transactionRepository.getProductTypeByBarcode(stockItem.barcode) ?: Transaction.PRODUCT_TYPE_FRAME
        dialogBinding.spinnerProductType.setText(currentProductType, false)

        AlertDialog.Builder(this)
            .setTitle("Stok Bilgilerini Düzenle")
            .setView(dialogBinding.root)
            .setPositiveButton("Kaydet") { _, _ ->
                val brand = dialogBinding.editTextBrand.text.toString().trim()
                val purchasePriceText = dialogBinding.editTextPurchasePrice.text.toString().trim()
                val quantityText = dialogBinding.editTextQuantity.text.toString().trim()
                val productType = dialogBinding.spinnerProductType.text.toString().trim()

                if (brand.isEmpty() || purchasePriceText.isEmpty() || quantityText.isEmpty() || productType.isEmpty()) {
                    Snackbar.make(binding.root, "Tüm alanları doldurun", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val purchasePrice = purchasePriceText.toDouble()
                    val quantity = quantityText.toInt()
                    
                    if (quantity <= 0) {
                        Snackbar.make(binding.root, "Adet 0'dan büyük olmalı", Snackbar.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    
                    // StockItem güncelle
                    val updatedStockItem = stockItem.copy(
                        brand = brand,
                        purchasePrice = purchasePrice,
                        quantity = quantity,
                        productType = productType
                    )
                    val success = stockItemRepository.updateStockItem(updatedStockItem)
                    
                    if (success) {
                        // Transaction'ı da güncelle
                        val existingTransaction = transactionRepository.getTransactionByBarcode(stockItem.barcode)
                        if (existingTransaction != null) {
                            val updatedTransaction = existingTransaction.copy(
                                brand = brand,
                                productType = productType,
                                amount = purchasePrice
                            )
                            transactionRepository.updateTransaction(updatedTransaction)
                        }
                        
                        loadStockItems()
                        loadProductTypeCounts()
                        Snackbar.make(binding.root, "Stok başarıyla güncellendi", Snackbar.LENGTH_SHORT).show()
                    } else {
                        Snackbar.make(binding.root, "Güncelleme hatası", Snackbar.LENGTH_SHORT).show()
                    }
                } catch (e: NumberFormatException) {
                    Snackbar.make(binding.root, "Geçerli bir değer girin", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun showFilterDialog() {
        val dialogBinding = DialogAdvancedFilterBinding.inflate(LayoutInflater.from(this))
        
        // Tarih seçici olayları
        dialogBinding.etStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                dialogBinding.etStartDate.setText(dateFormat.format(selectedDate))
            }
        }
        
        dialogBinding.etEndDate.setOnClickListener {
            showDatePicker { selectedDate ->
                dialogBinding.etEndDate.setText(dateFormat.format(selectedDate))
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("@string/filter_stocks")
            .setView(dialogBinding.root)
            .setPositiveButton("Filtrele") { _, _ ->
                applyStockFilter(dialogBinding)
            }
            .setNegativeButton("İptal", null)
            .create()

        // Temizle butonu
        dialogBinding.btnClearFilter.setOnClickListener {
            clearStockFilters()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDatePicker(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                onDateSelected(selectedCalendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun applyStockFilter(dialogBinding: DialogAdvancedFilterBinding) {
        val brandFilter = dialogBinding.etBrand.text.toString().trim()
        val productTypeFilter = dialogBinding.etProductType.text.toString().trim()
        val startDateText = dialogBinding.etStartDate.text.toString().trim()
        val endDateText = dialogBinding.etEndDate.text.toString().trim()
        val minAmountText = dialogBinding.etMinAmount.text.toString().trim()
        val maxAmountText = dialogBinding.etMaxAmount.text.toString().trim()

        var filteredList = allStocksList.toList()

        // Marka filtresi
        if (brandFilter.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.brand.contains(brandFilter, ignoreCase = true) 
            }
        }

        // Tarih filtresi (stok tarihi)
        if (startDateText.isNotEmpty() && endDateText.isNotEmpty()) {
            try {
                val startDate = dateFormat.parse(startDateText)
                val endDate = dateFormat.parse(endDateText)
                if (startDate != null && endDate != null) {
                    val endDateMidnight = Calendar.getInstance().apply {
                        time = endDate
                        add(Calendar.DAY_OF_MONTH, 1)
                        add(Calendar.MILLISECOND, -1)
                    }.time
                    
                    filteredList = filteredList.filter { stockItem ->
                        val stockDate = Date(stockItem.stockDate)
                        stockDate.after(startDate) && stockDate.before(endDateMidnight)
                    }
                }
            } catch (e: Exception) {
                // Tarih parse hatası
            }
        }

        // Tutar filtresi (alış fiyatı)
        val minAmount = minAmountText.toDoubleOrNull()
        val maxAmount = maxAmountText.toDoubleOrNull()
        
        if (minAmount != null) {
            filteredList = filteredList.filter { it.purchasePrice >= minAmount }
        }
        
        if (maxAmount != null) {
            filteredList = filteredList.filter { it.purchasePrice <= maxAmount }
        }

        // Listeyi güncelle
        stockItemList.clear()
        stockItemList.addAll(filteredList)
        stockItemAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun clearStockFilters() {
        stockItemList.clear()
        stockItemList.addAll(allStocksList)
        stockItemAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    override fun onResume() {
        super.onResume()
        loadStockItems()
        loadProductTypeCounts()
    }
}