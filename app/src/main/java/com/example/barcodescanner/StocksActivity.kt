package com.example.barcodescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.databinding.ActivityStocksBinding
import com.google.android.material.snackbar.Snackbar

class StocksActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStocksBinding
    private lateinit var stockBarcodeAdapter: StockBarcodeAdapter
    private val stockBarcodeList = mutableListOf<BarcodeItem>()

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
                addBarcodeToStockList(barcode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStocksBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupScanButton()
        updateEmptyState()
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
        stockBarcodeAdapter = StockBarcodeAdapter(stockBarcodeList) { barcodeItem ->
            showBarcodeDetails(barcodeItem)
        }
        
        binding.recyclerViewStockBarcodes.apply {
            layoutManager = LinearLayoutManager(this@StocksActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = stockBarcodeAdapter
        }
    }

    private fun setupScanButton() {
        binding.btnScanBarcode.setOnClickListener {
            checkCameraPermissionAndScan()
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

    private fun addBarcodeToStockList(barcode: String) {
        val timestamp = System.currentTimeMillis()
        val barcodeItem = BarcodeItem(barcode, timestamp)
        
        stockBarcodeList.add(0, barcodeItem) // Add to beginning of list
        stockBarcodeAdapter.notifyItemInserted(0)
        binding.recyclerViewStockBarcodes.smoothScrollToPosition(0)

        // Show success message
        Snackbar.make(
            binding.root,
            "Stok barkodu eklendi: $barcode",
            Snackbar.LENGTH_SHORT
        ).show()

        // Update empty state
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (stockBarcodeList.isEmpty()) {
            binding.textViewEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewStockBarcodes.visibility = android.view.View.GONE
        } else {
            binding.textViewEmptyState.visibility = android.view.View.GONE
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

    private fun showBarcodeDetails(barcodeItem: BarcodeItem) {
        val message = "Stok Barkodu: ${barcodeItem.value}\n" +
                "Eklenme Zamanı: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(barcodeItem.timestamp))}"
        
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        updateEmptyState()
    }
}