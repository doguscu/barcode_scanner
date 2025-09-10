package com.example.barcodescanner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.databinding.ItemBarcodeBinding
import java.text.DateFormat
import java.util.*

class BarcodeAdapter(
    private val barcodeList: List<BarcodeItem>,
    private val onItemClick: (BarcodeItem) -> Unit
) : RecyclerView.Adapter<BarcodeAdapter.BarcodeViewHolder>() {

    inner class BarcodeViewHolder(private val binding: ItemBarcodeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barcodeItem: BarcodeItem) {
            binding.textViewBarcodeValue.text = barcodeItem.value
            binding.textViewTimestamp.text = DateFormat.getDateTimeInstance(
                DateFormat.SHORT,
                DateFormat.SHORT
            ).format(Date(barcodeItem.timestamp))

            binding.root.setOnClickListener {
                onItemClick(barcodeItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarcodeViewHolder {
        val binding = ItemBarcodeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BarcodeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarcodeViewHolder, position: Int) {
        holder.bind(barcodeList[position])
    }

    override fun getItemCount(): Int = barcodeList.size
}
