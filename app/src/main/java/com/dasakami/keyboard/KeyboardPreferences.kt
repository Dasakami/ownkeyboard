package com.dasakami.keyboard

import android.content.Context
import android.graphics.Color

class KeyboardPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE)
    
    var keyboardScale: Float
        get() = prefs.getFloat("scale", 1.0f)
        set(value) = prefs.edit().putFloat("scale", value).apply()
    
    var keyHeight: Int
        get() = prefs.getInt("key_height", 55)
        set(value) = prefs.edit().putInt("key_height", value).apply()
    
    var horizontalGap: Int
        get() = prefs.getInt("h_gap", 2)
        set(value) = prefs.edit().putInt("h_gap", value).apply()
    
    var verticalGap: Int
        get() = prefs.getInt("v_gap", 4)
        set(value) = prefs.edit().putInt("v_gap", value).apply()
    
    var backgroundImage: String?
        get() = prefs.getString("bg_image", null)
        set(value) = prefs.edit().putString("bg_image", value).apply()
    
    var keyColor: Int
        get() = prefs.getInt("key_color", Color.WHITE)
        set(value) = prefs.edit().putInt("key_color", value).apply()
    
    var keyPressedColor: Int
        get() = prefs.getInt("key_pressed_color", Color.parseColor("#E0E0E0"))
        set(value) = prefs.edit().putInt("key_pressed_color", value).apply()
    
    var textColor: Int
        get() = prefs.getInt("text_color", Color.parseColor("#212121"))
        set(value) = prefs.edit().putInt("text_color", value).apply()
    
    var backgroundColor: Int
        get() = prefs.getInt("bg_color", Color.parseColor("#ECEFF1"))
        set(value) = prefs.edit().putInt("bg_color", value).apply()
    
    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()
    
    var vibrationEnabled: Boolean
        get() = prefs.getBoolean("vibration", true)
        set(value) = prefs.edit().putBoolean("vibration", value).apply()
    
    var vibrationDuration: Long
        get() = prefs.getLong("vib_duration", 25L)
        set(value) = prefs.edit().putLong("vib_duration", value).apply()
    
    var currentLanguage: String
        get() = prefs.getString("language", "en") ?: "en"
        set(value) = prefs.edit().putString("language", value).apply()
    
    fun reset() {
        prefs.edit().clear().apply()
    }
    
    fun applyTheme(theme: String) {
        when (theme) {
            "light" -> {
                keyColor = Color.WHITE
                keyPressedColor = Color.parseColor("#E0E0E0")
                textColor = Color.parseColor("#212121")
                backgroundColor = Color.parseColor("#ECEFF1")
            }
            "dark" -> {
                keyColor = Color.parseColor("#424242")
                keyPressedColor = Color.parseColor("#212121")
                textColor = Color.WHITE
                backgroundColor = Color.parseColor("#121212")
            }
            "blue" -> {
                keyColor = Color.parseColor("#E3F2FD")
                keyPressedColor = Color.parseColor("#90CAF9")
                textColor = Color.parseColor("#0D47A1")
                backgroundColor = Color.parseColor("#BBDEFB")
            }
            "green" -> {
                keyColor = Color.parseColor("#E8F5E9")
                keyPressedColor = Color.parseColor("#81C784")
                textColor = Color.parseColor("#1B5E20")
                backgroundColor = Color.parseColor("#C8E6C9")
            }
        }
    }
}