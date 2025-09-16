package com.example.barcodescanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemStockBarcodeBinding
import java.text.SimpleDateFormat
import java.util.*

class StockBarcodeAdapter(
    private val barcodeList: List<BarcodeItem>,
    private val onItemClick: (BarcodeItem) -> Unit
) : RecyclerView.Adapter<StockBarcodeAdapter.StockBarcodeViewHolder>() {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    class StockBarcodeViewHolder(private val binding: ItemStockBarcodeBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(item: BarcodeItem, onItemClick: (BarcodeItem) -> Unit, timeFormat: SimpleDateFormat) {
            binding.textViewBarcodeValue.text = item.value
            binding.textViewTimestamp.text = timeFormat.format(Date(item.timestamp))
            
            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockBarcodeViewHolder {
        val binding = ItemStockBarcodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockBarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockBarcodeViewHolder, position: Int) {
        holder.bind(barcodeList[position], onItemClick, timeFormat)
    }

    override fun getItemCount(): Int = barcodeList.size
}