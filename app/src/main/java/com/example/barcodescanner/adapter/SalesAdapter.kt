package com.example.barcodescanner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemSaleBinding
import com.example.barcodescanner.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class SalesAdapter(
    private val sales: List<Transaction>
) : RecyclerView.Adapter<SalesAdapter.SalesViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

    class SalesViewHolder(private val binding: ItemSaleBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(sale: Transaction, dateFormat: SimpleDateFormat, currencyFormat: NumberFormat) {
            binding.textViewBrand.text = sale.brand
            binding.textViewProductType.text = sale.productType
            binding.textViewDate.text = dateFormat.format(Date(sale.transactionDate))
            
            // Satış tutarını yeşil renkte ve + ile göster
            binding.textViewAmount.text = "+${currencyFormat.format(sale.amount)}"
            binding.textViewAmount.setTextColor(
                ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalesViewHolder {
        val binding = ItemSaleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SalesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SalesViewHolder, position: Int) {
        holder.bind(sales[position], dateFormat, currencyFormat)
    }

    override fun getItemCount(): Int = sales.size
}