package com.example.barcodescanner.repository

import android.content.Context
import android.net.Uri
import com.example.barcodescanner.model.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class DataImportExportRepository(private val context: Context) {
    
    private val stockItemRepository = StockItemRepository(context)
    private val transactionRepository = TransactionRepository(context)
    private val scanResultRepository = ScanResultRepository(context)
    private val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Tüm verileri JSON formatında dışa aktar
     */
    fun exportAllData(uri: Uri): Result<String> {
        return try {
            val stockItems = stockItemRepository.getAllStockItems()
            val transactions = transactionRepository.getAllTransactions()
            val scanResults = scanResultRepository.getAllScanResults()
            
            val exportData = ExportData(
                stockItems = stockItems,
                transactions = transactions,
                scanResults = scanResults
            )
            
            val jsonString = gson.toJson(exportData)
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(jsonString)
                }
            }
            
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())
            
            Result.success("Veriler başarıyla dışa aktarıldı\n$formattedDate\n" +
                    "Stok: ${stockItems.size} kayıt\n" +
                    "İşlem: ${transactions.size} kayıt\n" +
                    "Tarama: ${scanResults.size} kayıt")
            
        } catch (e: Exception) {
            Result.failure(Exception("Dışa aktarma hatası: ${e.message}"))
        }
    }
    
    /**
     * JSON formatından verileri içe aktar
     */
    fun importData(uri: Uri, replaceExisting: Boolean = false): Result<String> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, "UTF-8").use { reader ->
                    reader.readText()
                }
            } ?: throw Exception("Dosya okunamadı")
            
            val importData = gson.fromJson(jsonString, ExportData::class.java)
                ?: throw Exception("Geçersiz dosya formatı")
            
            // Verileri temizle (isteğe bağlı)
            if (replaceExisting) {
                clearAllData()
            }
            
            var importedStocks = 0
            var importedTransactions = 0
            var importedScans = 0
            var skippedItems = 0
            
            // Stok verilerini içe aktar
            importData.stockItems.forEach { stockItem ->
                try {
                    if (!replaceExisting) {
                        // Mevcut barkodu kontrol et
                        val existing = stockItemRepository.getStockItemByBarcode(stockItem.barcode)
                        if (existing != null) {
                            skippedItems++
                            return@forEach
                        }
                    }
                    stockItemRepository.insertStockItem(stockItem.copy(id = 0))
                    importedStocks++
                } catch (e: Exception) {
                    skippedItems++
                }
            }
            
            // İşlem verilerini içe aktar
            importData.transactions.forEach { transaction ->
                try {
                    transactionRepository.insertTransaction(transaction.copy(id = 0))
                    importedTransactions++
                } catch (e: Exception) {
                    skippedItems++
                }
            }
            
            // Tarama verilerini içe aktar
            importData.scanResults.forEach { scanResult ->
                try {
                    scanResultRepository.insertScanResult(scanResult.copy(id = 0))
                    importedScans++
                } catch (e: Exception) {
                    skippedItems++
                }
            }
            
            val resultMessage = buildString {
                appendLine("Veriler başarıyla içe aktarıldı")
                appendLine("Stok: $importedStocks kayıt")
                appendLine("İşlem: $importedTransactions kayıt")
                appendLine("Tarama: $importedScans kayıt")
                if (skippedItems > 0) {
                    appendLine("Atlanan: $skippedItems kayıt")
                }
            }
            
            Result.success(resultMessage)
            
        } catch (e: Exception) {
            Result.failure(Exception("İçe aktarma hatası: ${e.message}"))
        }
    }
    
    /**
     * Tüm verileri temizle
     */
    private fun clearAllData() {
        stockItemRepository.clearAllStockItems()
        transactionRepository.clearAllTransactions()
        scanResultRepository.clearAllScanResults()
    }
    
    /**
     * Varsayılan dosya adı oluştur
     */
    fun generateExportFileName(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        return "barcode_data_$timestamp.json"
    }
    
    /**
     * Dosya formatını kontrol et
     */
    fun validateImportFile(uri: Uri): Result<ExportData> {
        return try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, "UTF-8").use { reader ->
                    reader.readText()
                }
            } ?: throw Exception("Dosya okunamadı")
            
            val importData = gson.fromJson(jsonString, ExportData::class.java)
                ?: throw Exception("Geçersiz JSON formatı")
            
            Result.success(importData)
            
        } catch (e: Exception) {
            Result.failure(Exception("Dosya doğrulama hatası: ${e.message}"))
        }
    }
}