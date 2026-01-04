package com.dasakami.keyboard

import android.content.ClipData
import android.content.Context

class ClipboardManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("clipboard_history", Context.MODE_PRIVATE)
    
    fun saveToHistory(text: String) {
        if (text.isBlank()) return
        
        val history = getHistory().toMutableList()
        history.remove(text) 
        history.add(0, text)
        
        val toSave = history.take(5)
        prefs.edit().apply {
            clear()
            toSave.forEachIndexed { index, item ->
                putString("clip_$index", item)
            }
            apply()
        }
    }
    
    fun getHistory(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until 5) {
            prefs.getString("clip_$i", null)?.let {
                list.add(it)
            }
        }
        return list
    }
    
    fun clearHistory() {
        prefs.edit().clear().apply()
    }
}
