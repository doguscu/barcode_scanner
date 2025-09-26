package com.example.barcodescanner

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.adapter.SalesAdapter
import com.example.barcodescanner.databinding.ActivitySalesBinding
import com.example.barcodescanner.databinding.DialogAdvancedFilterBinding
import com.example.barcodescanner.model.Transaction
import com.example.barcodescanner.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*

class SalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesBinding
    private lateinit var salesAdapter: SalesAdapter
    private val salesList = mutableListOf<Transaction>()
    private val allSalesList = mutableListOf<Transaction>()
    private lateinit var transactionRepository: TransactionRepository
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("tr", "TR"))
    
    // Sorting states
    private var currentSortField: String? = null
    private var isAscending = true
    
    companion object {
        const val SORT_BRAND = "brand"
        const val SORT_PRODUCT_TYPE = "product_type"
        const val SORT_DATE = "date"
        const val SORT_AMOUNT = "amount"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        
        setupToolbar()
        setupRecyclerView()
        setupFilterButton()
        setupSortHeaders()
        loadSalesData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Satışlarım"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        salesAdapter = SalesAdapter(salesList)
        
        binding.recyclerViewSales.apply {
            layoutManager = LinearLayoutManager(this@SalesActivity)
            adapter = salesAdapter
        }
    }

    private fun setupFilterButton() {
        binding.buttonFilterSales.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun setupSortHeaders() {
        binding.headerBrand.setOnClickListener {
            sortBy(SORT_BRAND)
        }
        
        binding.headerProductType.setOnClickListener {
            sortBy(SORT_PRODUCT_TYPE)
        }
        
        binding.headerDate.setOnClickListener {
            sortBy(SORT_DATE)
        }
        
        binding.headerAmount.setOnClickListener {
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
                    salesList.sortedBy { it.brand.lowercase() }
                } else {
                    salesList.sortedByDescending { it.brand.lowercase() }
                }
            }
            SORT_PRODUCT_TYPE -> {
                if (isAscending) {
                    salesList.sortedBy { it.productType.lowercase() }
                } else {
                    salesList.sortedByDescending { it.productType.lowercase() }
                }
            }
            SORT_DATE -> {
                if (isAscending) {
                    salesList.sortedBy { it.transactionDate }
                } else {
                    salesList.sortedByDescending { it.transactionDate }
                }
            }
            SORT_AMOUNT -> {
                if (isAscending) {
                    salesList.sortedBy { it.amount }
                } else {
                    salesList.sortedByDescending { it.amount }
                }
            }
            else -> salesList
        }
        
        salesList.clear()
        salesList.addAll(sortedList)
        salesAdapter.notifyDataSetChanged()
    }

    private fun loadSalesData() {
        salesList.clear()
        allSalesList.clear()
        // Sadece satış tipindeki işlemleri getir
        val allTransactions = transactionRepository.getRecentTransactions()
        val salesTransactions = allTransactions.filter { it.transactionType == Transaction.TYPE_SALE }
        allSalesList.addAll(salesTransactions)
        salesList.addAll(salesTransactions)
        salesAdapter.notifyDataSetChanged()
        updateEmptyState()
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
            .setTitle("@string/filter_sales")
            .setView(dialogBinding.root)
            .setPositiveButton("Filtrele") { _, _ ->
                applyFilter(dialogBinding)
            }
            .setNegativeButton("İptal", null)
            .create()

        // Temizle butonu
        dialogBinding.btnClearFilter.setOnClickListener {
            clearFilters()
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

    private fun applyFilter(dialogBinding: DialogAdvancedFilterBinding) {
        val brandFilter = dialogBinding.etBrand.text.toString().trim()
        val productTypeFilter = dialogBinding.etProductType.text.toString().trim()
        val startDateText = dialogBinding.etStartDate.text.toString().trim()
        val endDateText = dialogBinding.etEndDate.text.toString().trim()
        val minAmountText = dialogBinding.etMinAmount.text.toString().trim()
        val maxAmountText = dialogBinding.etMaxAmount.text.toString().trim()

        var filteredList = allSalesList.toList()

        // Marka filtresi
        if (brandFilter.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.brand.contains(brandFilter, ignoreCase = true) 
            }
        }

        // Ürün tipi filtresi
        if (productTypeFilter.isNotEmpty()) {
            filteredList = filteredList.filter { 
                it.productType.contains(productTypeFilter, ignoreCase = true) 
            }
        }

        // Tarih filtresi
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
                    
                    filteredList = filteredList.filter { transaction ->
                        val transactionDate = Date(transaction.transactionDate)
                        transactionDate.after(startDate) && transactionDate.before(endDateMidnight)
                    }
                }
            } catch (e: Exception) {
                // Tarih parse hatası
            }
        }

        // Tutar filtresi
        val minAmount = minAmountText.toDoubleOrNull()
        val maxAmount = maxAmountText.toDoubleOrNull()
        
        if (minAmount != null) {
            filteredList = filteredList.filter { it.amount >= minAmount }
        }
        
        if (maxAmount != null) {
            filteredList = filteredList.filter { it.amount <= maxAmount }
        }

        // Listeyi güncelle
        salesList.clear()
        salesList.addAll(filteredList)
        salesAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun clearFilters() {
        salesList.clear()
        salesList.addAll(allSalesList)
        salesAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (salesList.isEmpty()) {
            binding.textViewEmptySales.visibility = android.view.View.VISIBLE
            binding.recyclerViewSales.visibility = android.view.View.GONE
        } else {
            binding.textViewEmptySales.visibility = android.view.View.GONE
            binding.recyclerViewSales.visibility = android.view.View.VISIBLE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadSalesData()
    }
}