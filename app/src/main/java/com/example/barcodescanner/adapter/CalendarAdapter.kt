package com.example.barcodescanner.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.barcodescanner.R
import com.example.barcodescanner.databinding.ItemCalendarDayBinding
import java.text.NumberFormat
import java.util.*

data class CalendarDay(val day: String, val profit: Double)

class CalendarAdapter(
    private var days: List<CalendarDay>
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    class CalendarViewHolder(private val binding: ItemCalendarDayBinding) : 
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(calendarDay: CalendarDay) {
            if (calendarDay.day.isEmpty()) {
                binding.textViewDay.text = ""
                binding.textViewProfit.text = ""
                binding.root.alpha = 0.3f
            } else {
                binding.textViewDay.text = calendarDay.day
                binding.textViewProfit.text = formatProfitText(calendarDay.profit)
                binding.root.alpha = 1.0f
                
                // Kar durumuna göre renk ayarla
                if (calendarDay.profit > 0) {
                    binding.textViewProfit.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_green_dark)
                    )
                } else if (calendarDay.profit < 0) {
                    binding.textViewProfit.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.holo_red_dark)
                    )
                } else {
                    binding.textViewProfit.setTextColor(
                        ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                    )
                }
                
                // Bugünün tarihini vurgula
                val today = Calendar.getInstance()
                val dayInt = calendarDay.day.toIntOrNull() ?: 0
                
                if (dayInt == today.get(Calendar.DAY_OF_MONTH)) {
                    binding.root.strokeColor = ContextCompat.getColor(binding.root.context, R.color.purple_500)
                    binding.root.strokeWidth = 4
                } else {
                    binding.root.strokeColor = ContextCompat.getColor(binding.root.context, android.R.color.transparent)
                    binding.root.strokeWidth = 1
                }
            }
        }

        private fun formatProfitText(profit: Double): String {
            val roundedProfit = profit.toInt()
            return if (roundedProfit < 0) {
                "-₺${kotlin.math.abs(roundedProfit)}"
            } else {
                "₺$roundedProfit"
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        holder.bind(days[position])
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}