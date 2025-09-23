package com.example.barcodescanner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemScanResultBinding
import com.example.barcodescanner.model.ScanResult
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

class ScanResultAdapter(
    private val scanResults: List<ScanResult>,
    private val onItemClick: (ScanResult) -> Unit
) : RecyclerView.Adapter<ScanResultAdapter.ScanResultViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    inner class ScanResultViewHolder(private val binding: ItemScanResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(scanResult: ScanResult) {
            binding.textViewBarcode.text = scanResult.barcode
            binding.textViewBrand.text = scanResult.brand
            binding.textViewSalePrice.text = currencyFormat.format(scanResult.salePrice)
            binding.textViewDate.text = dateFormat.format(Date(scanResult.scanDate))

            binding.root.setOnClickListener {
                onItemClick(scanResult)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanResultViewHolder {
        val binding = ItemScanResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScanResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScanResultViewHolder, position: Int) {
        holder.bind(scanResults[position])
    }

    override fun getItemCount(): Int = scanResults.size
}