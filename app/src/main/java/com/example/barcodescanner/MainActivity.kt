package com.example.barcodescanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.barcodescanner.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var barcodeAdapter: BarcodeAdapter
    private val barcodeList = mutableListOf<BarcodeItem>()
    private lateinit var drawerToggle: ActionBarDrawerToggle

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
                addBarcodeToList(barcode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupScanButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Ana Sayfa"
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
        barcodeAdapter = BarcodeAdapter(barcodeList) { barcodeItem ->
            // Handle barcode item click if needed
            showBarcodeDetails(barcodeItem)
        }
        
        binding.recyclerViewBarcodes.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = barcodeAdapter
        }
    }

    private fun setupScanButton() {
        binding.fabScanBarcode.setOnClickListener {
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

    private fun addBarcodeToList(barcode: String) {
        val timestamp = System.currentTimeMillis()
        val barcodeItem = BarcodeItem(barcode, timestamp)
        
        barcodeList.add(0, barcodeItem) // Add to beginning of list
        barcodeAdapter.notifyItemInserted(0)
        binding.recyclerViewBarcodes.smoothScrollToPosition(0)

        // Show success message
        Snackbar.make(
            binding.root,
            "Barkod başarıyla eklendi: $barcode",
            Snackbar.LENGTH_SHORT
        ).show()

        // Update empty state
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (barcodeList.isEmpty()) {
            binding.textViewEmptyState.visibility = android.view.View.VISIBLE
            binding.recyclerViewBarcodes.visibility = android.view.View.GONE
        } else {
            binding.textViewEmptyState.visibility = android.view.View.GONE
            binding.recyclerViewBarcodes.visibility = android.view.View.VISIBLE
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
        val message = "Barkod: ${barcodeItem.value}\n" +
                "Tarih: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(barcodeItem.timestamp))}"
        
        Snackbar.make(
            binding.root,
            message,
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
            R.id.nav_calendar -> {
                startActivity(Intent(this, CalendarActivity::class.java))
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        updateEmptyState()
        // Set home as selected when returning to main activity
        binding.navigationView.setCheckedItem(R.id.nav_home)
    }
}
