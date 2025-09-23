package com.example.barcodescanner

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.adapter.SalesAdapter
import com.example.barcodescanner.databinding.ActivitySalesBinding
import com.example.barcodescanner.model.Transaction
import com.example.barcodescanner.repository.TransactionRepository

class SalesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySalesBinding
    private lateinit var salesAdapter: SalesAdapter
    private val salesList = mutableListOf<Transaction>()
    private lateinit var transactionRepository: TransactionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySalesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        
        setupToolbar()
        setupRecyclerView()
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

    private fun loadSalesData() {
        salesList.clear()
        // Sadece satış tipindeki işlemleri getir
        val allTransactions = transactionRepository.getRecentTransactions()
        val salesTransactions = allTransactions.filter { it.transactionType == Transaction.TYPE_SALE }
        salesList.addAll(salesTransactions)
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