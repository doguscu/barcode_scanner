package com.example.barcodescanner

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.example.barcodescanner.adapter.CalendarAdapter
import com.example.barcodescanner.databinding.ActivityCalendarBinding
import com.example.barcodescanner.adapter.CalendarDay
import com.example.barcodescanner.repository.TransactionRepository
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var transactionRepository: TransactionRepository
    private val calendar = Calendar.getInstance()
    private var currentYear = 2025
    private var currentMonth = Calendar.JANUARY

    private val yearList = (2025..2030).toList()
    private val monthNames = arrayOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        
        setupToolbar()
        setupSpinners()
        setupCalendar()
        loadProfitData()
        updateCalendar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Takvim"
        }
    }

    private fun setupSpinners() {
        // Yıl spinner
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, yearList)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerYear.adapter = yearAdapter
        binding.spinnerYear.setSelection(0) // 2025 başlangıç

        // Ay spinner
        val monthAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, monthNames)
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMonth.adapter = monthAdapter
        binding.spinnerMonth.setSelection(Calendar.getInstance().get(Calendar.MONTH)) // Mevcut ay

        // Spinner listeners
        binding.spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentYear = yearList[position]
                updateCalendar()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerMonth.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentMonth = position
                updateCalendar()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarAdapter(emptyList())
        binding.recyclerViewCalendar.apply {
            layoutManager = GridLayoutManager(this@CalendarActivity, 7)
            adapter = calendarAdapter
        }
    }

    private fun updateCalendar() {
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.MONTH, currentMonth)
        calendar.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        var firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2 // Pazartesi = 0
        if (firstDayOfWeek < 0) firstDayOfWeek = 6 // Pazar için 6

        val calendarDays = mutableListOf<CalendarDay>()

        // Önceki ayın boş günleri
        repeat(firstDayOfWeek) {
            calendarDays.add(CalendarDay("", 0.0))
        }

        // Bu ayın günleri
        for (day in 1..daysInMonth) {
            val profit = getDayProfit(currentYear, currentMonth, day)
            calendarDays.add(CalendarDay(day.toString(), profit))
        }

        // Kalan boş günleri tamamla (42 gün toplam - 6 satır x 7 gün)
        while (calendarDays.size < 42) {
            calendarDays.add(CalendarDay("", 0.0))
        }

        calendarAdapter.updateDays(calendarDays)
    }

    private fun getDayProfit(year: Int, month: Int, day: Int): Double {
        // Belirli bir günün net karını hesapla
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis
        
        return transactionRepository.getProfitForDateRange(startOfDay, endOfDay)
    }

    private fun loadProfitData() {
        // Bugünkü kar
        val dailyProfit = transactionRepository.getTodayNetIncome()
        binding.textViewDailyProfit.text = String.format("₺%.2f", dailyProfit)
        
        // Kar durumuna göre renk ayarla
        if (dailyProfit > 0) {
            binding.textViewDailyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else if (dailyProfit < 0) {
            binding.textViewDailyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
        }

        // Son 7 günün karı
        val weeklyProfit = getWeeklyProfit()
        binding.textViewWeeklyProfit.text = String.format("₺%.2f", weeklyProfit)
        
        if (weeklyProfit > 0) {
            binding.textViewWeeklyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else if (weeklyProfit < 0) {
            binding.textViewWeeklyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
        }

        // Son 30 günün karı
        val monthlyProfit = getMonthlyProfit()
        binding.textViewMonthlyProfit.text = String.format("₺%.2f", monthlyProfit)
        
        if (monthlyProfit > 0) {
            binding.textViewMonthlyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_green_dark)
            )
        } else if (monthlyProfit < 0) {
            binding.textViewMonthlyProfit.setTextColor(
                ContextCompat.getColor(this, android.R.color.holo_red_dark)
            )
        }
    }

    private fun getWeeklyProfit(): Double {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfWeek = calendar.timeInMillis
        
        val endOfWeek = System.currentTimeMillis()
        
        return transactionRepository.getProfitForDateRange(startOfWeek, endOfWeek)
    }

    private fun getMonthlyProfit(): Double {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis
        
        val endOfMonth = System.currentTimeMillis()
        
        return transactionRepository.getProfitForDateRange(startOfMonth, endOfMonth)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        loadProfitData()
    }
}