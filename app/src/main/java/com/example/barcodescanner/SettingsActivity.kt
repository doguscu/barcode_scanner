package com.example.barcodescanner

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.barcodescanner.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        const val PREF_THEME = "theme_preference"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_SYSTEM = "system"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        
        setupToolbar()
        setupThemeOptions()
        updateThemeSelection()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Tema"
        }
    }

    private fun setupThemeOptions() {
        binding.lightThemeOption.setOnClickListener {
            setTheme(THEME_LIGHT)
        }
        
        binding.darkThemeOption.setOnClickListener {
            setTheme(THEME_DARK)
        }
        
        binding.systemThemeOption.setOnClickListener {
            setTheme(THEME_SYSTEM)
        }
    }

    private fun setTheme(theme: String) {
        sharedPreferences.edit().putString(PREF_THEME, theme).apply()
        
        when (theme) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            THEME_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        
        updateThemeSelection()
    }

    private fun updateThemeSelection() {
        val currentTheme = sharedPreferences.getString(PREF_THEME, THEME_LIGHT) ?: THEME_LIGHT
        
        // Reset all selections
        binding.lightThemeOption.isChecked = false
        binding.darkThemeOption.isChecked = false
        binding.systemThemeOption.isChecked = false
        
        // Set current selection
        when (currentTheme) {
            THEME_LIGHT -> binding.lightThemeOption.isChecked = true
            THEME_DARK -> binding.darkThemeOption.isChecked = true
            THEME_SYSTEM -> binding.systemThemeOption.isChecked = true
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}