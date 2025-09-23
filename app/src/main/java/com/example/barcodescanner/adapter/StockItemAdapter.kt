package com.example.barcodescanner.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemStockItemBinding
import com.example.barcodescanner.model.StockItem
import com.example.barcodescanner.repository.TransactionRepository
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

































































































































































































































































































class StockItemAdapter(
    private val context: Context,
    private val stockItems: List<StockItem>,
    private val onItemClick: (StockItem) -> Unit,
    private val onItemLongClick: (StockItem) -> Unit
) : RecyclerView.Adapter<StockItemAdapter.StockItemViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    private val transactionRepository = TransactionRepository(context)

    class StockItemViewHolder(private val binding: ItemStockItemBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: StockItem, onItemClick: (StockItem) -> Unit, onItemLongClick: (StockItem) -> Unit, dateFormat: SimpleDateFormat, currencyFormat: NumberFormat, transactionRepository: TransactionRepository) {
            binding.textViewBrand.text = item.brand
            
            // Ürün tipini transaction tablosundan çek
            val productType = transactionRepository.getProductTypeByBarcode(item.barcode) ?: "Genel"
            binding.textViewProductType.text = productType
            
            // Adet bilgisini göster
            binding.textViewQuantity.text = item.quantity.toString()
            
            binding.textViewDate.text = dateFormat.format(Date(item.stockDate))
            
            // Tutar: Kırmızı renkte ve - ile başlayarak
            binding.textViewAmount.text = "-${currencyFormat.format(item.purchasePrice)}"
            binding.textViewAmount.setTextColor(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
            )
            
            binding.root.setOnClickListener {
                onItemClick(item)
            }
            
            binding.root.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockItemViewHolder {
        val binding = ItemStockItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockItemViewHolder, position: Int) {
        holder.bind(stockItems[position], onItemClick, onItemLongClick, dateFormat, currencyFormat, transactionRepository)
    }

    override fun getItemCount(): Int = stockItems.size
}