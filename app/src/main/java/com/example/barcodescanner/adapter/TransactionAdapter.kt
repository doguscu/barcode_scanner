package com.example.barcodescanner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.R
import com.example.barcodescanner.databinding.ItemTransactionBinding
import com.example.barcodescanner.model.Transaction
import java.text.NumberFormat
import java.util.*

class TransactionAdapter(
    private val transactions: List<Transaction>
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

    inner class TransactionViewHolder(private val binding: ItemTransactionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.textViewBrand.text = transaction.brand
            binding.textViewProductType.text = transaction.productType
            
            // İşlem tipini Türkçe olarak göster
            val transactionTypeText = when (transaction.transactionType) {
                Transaction.TYPE_SALE -> "Satış"
                Transaction.TYPE_STOCK_ENTRY -> "Stok Girdi"
                else -> transaction.transactionType
            }
            binding.textViewTransactionTypeText.text = transactionTypeText
            
            // Tutar formatı ve renklendirme
            when (transaction.transactionType) {
                Transaction.TYPE_SALE -> {
                    // Satış: Yeşil renkte pozitif tutar
                    binding.textViewAmount.text = currencyFormat.format(transaction.amount)
                    binding.textViewAmount.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    )
                }
                Transaction.TYPE_STOCK_ENTRY -> {
                    // Stok girdi: Kırmızı renkte negatif tutar
                    binding.textViewAmount.text = "-${currencyFormat.format(transaction.amount)}"
                    binding.textViewAmount.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                }
                else -> {
                    binding.textViewAmount.text = currencyFormat.format(transaction.amount)
                    binding.textViewAmount.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.black)
                    )
                }
            }
            
            // İşlem tipine göre ikon ayarla
            val iconRes = when (transaction.transactionType) {
                Transaction.TYPE_SALE -> R.drawable.ic_qr_code_scanner_24
                Transaction.TYPE_STOCK_ENTRY -> R.drawable.ic_inventory_24
                else -> R.drawable.ic_qr_code_scanner_24
            }
            binding.imageViewTransactionType.setImageResource(iconRes)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(transactions[position])
    }

    override fun getItemCount(): Int = transactions.size
}