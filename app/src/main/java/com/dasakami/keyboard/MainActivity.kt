package com.dasakami.keyboard

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    
    private val PICK_IMAGE = 100
    private lateinit var prefs: KeyboardPreferences
    
    private lateinit var sectionSize: LinearLayout
    private lateinit var sectionTheme: LinearLayout
    private lateinit var sectionFeedback: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        prefs = KeyboardPreferences(this)
        
        sectionSize = findViewById(R.id.sectionSize)
        sectionTheme = findViewById(R.id.sectionTheme)
        sectionFeedback = findViewById(R.id.sectionFeedback)
        
        // Показать нужный раздел
        val section = intent.getStringExtra("section") ?: "full"
        showSection(section)
        
        initViews()
        loadSettings()
        setupListeners()
    }
    
    private fun showSection(section: String) {
        when (section) {
            "size" -> {
                sectionSize.visibility = View.VISIBLE
                sectionTheme.visibility = View.GONE
                sectionFeedback.visibility = View.GONE
            }
            "theme" -> {
                sectionSize.visibility = View.GONE
                sectionTheme.visibility = View.VISIBLE
                sectionFeedback.visibility = View.GONE
            }
            "feedback" -> {
                sectionSize.visibility = View.GONE
                sectionTheme.visibility = View.GONE
                sectionFeedback.visibility = View.VISIBLE
            }
            else -> {
                // Показать все
                sectionSize.visibility = View.VISIBLE
                sectionTheme.visibility = View.VISIBLE
                sectionFeedback.visibility = View.VISIBLE
            }
        }
    }
    
    private fun initViews() {
        // Кнопки фона
        findViewById<Button>(R.id.uploadBgButton).setOnClickListener { pickImage() }
        findViewById<Button>(R.id.removeBgButton).setOnClickListener { removeBackground() }
        
        // Кнопки тем
        findViewById<Button>(R.id.themeLightButton).setOnClickListener { applyTheme("light") }
        findViewById<Button>(R.id.themeDarkButton).setOnClickListener { applyTheme("dark") }
        findViewById<Button>(R.id.themeBlueButton).setOnClickListener { applyTheme("blue") }
        findViewById<Button>(R.id.themeGreenButton).setOnClickListener { applyTheme("green") }
        
        // Сброс
        findViewById<Button>(R.id.resetButton).setOnClickListener { resetSettings() }
    }
    
    private fun loadSettings() {
        // Размер
        val scale = prefs.keyboardScale
        val scaleSeekBar = findViewById<SeekBar>(R.id.scaleSeekBar)
        val scaleText = findViewById<TextView>(R.id.scaleText)
        scaleSeekBar.progress = ((scale - 0.6f) * 100).toInt()
        scaleText.text = "Размер: ${String.format("%.1f", scale)}x"
        
        // Высота клавиш
        val keyHeight = prefs.keyHeight
        val keyHeightSeekBar = findViewById<SeekBar>(R.id.keyHeightSeekBar)
        val keyHeightText = findViewById<TextView>(R.id.keyHeightText)
        keyHeightSeekBar.progress = keyHeight - 40
        keyHeightText.text = "Высота клавиш: ${keyHeight}dp"
        
        // Отступы
        val hGap = prefs.horizontalGap
        val hGapSeekBar = findViewById<SeekBar>(R.id.hGapSeekBar)
        val hGapText = findViewById<TextView>(R.id.hGapText)
        hGapSeekBar.progress = hGap
        hGapText.text = "Горизонтальный: ${hGap}dp"
        
        val vGap = prefs.verticalGap
        val vGapSeekBar = findViewById<SeekBar>(R.id.vGapSeekBar)
        val vGapText = findViewById<TextView>(R.id.vGapText)
        vGapSeekBar.progress = vGap
        vGapText.text = "Вертикальный: ${vGap}dp"
        
        // Вибрация
        val vibDuration = prefs.vibrationDuration
        val vibrationDurationSeekBar = findViewById<SeekBar>(R.id.vibrationDurationSeekBar)
        val vibDurationText = findViewById<TextView>(R.id.vibDurationText)
        vibrationDurationSeekBar.progress = (vibDuration - 10).toInt()
        vibDurationText.text = "Длительность: ${vibDuration}ms"
        
        // Переключатели
        findViewById<Switch>(R.id.soundSwitch).isChecked = prefs.soundEnabled
        findViewById<Switch>(R.id.vibrationSwitch).isChecked = prefs.vibrationEnabled
        
        // Фон
        val bgPath = prefs.backgroundImage
        if (!bgPath.isNullOrEmpty() && File(bgPath).exists()) {
            val bitmap = BitmapFactory.decodeFile(bgPath)
            findViewById<ImageView>(R.id.bgPreview).setImageBitmap(bitmap)
        }
    }
    
    private fun setupListeners() {
        findViewById<SeekBar>(R.id.scaleSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val scale = 0.6f + (progress / 100f)
                prefs.keyboardScale = scale
                findViewById<TextView>(R.id.scaleText).text = "Размер: ${String.format("%.1f", scale)}x"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.keyHeightSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val height = progress + 40
                prefs.keyHeight = height
                findViewById<TextView>(R.id.keyHeightText).text = "Высота клавиш: ${height}dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.hGapSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.horizontalGap = progress
                findViewById<TextView>(R.id.hGapText).text = "Горизонтальный: ${progress}dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.vGapSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.verticalGap = progress
                findViewById<TextView>(R.id.vGapText).text = "Вертикальный: ${progress}dp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<SeekBar>(R.id.vibrationDurationSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val duration = (progress + 10).toLong()
                prefs.vibrationDuration = duration
                findViewById<TextView>(R.id.vibDurationText).text = "Длительность: ${duration}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        findViewById<Switch>(R.id.soundSwitch).setOnCheckedChangeListener { _, isChecked ->
            prefs.soundEnabled = isChecked
        }
        
        findViewById<Switch>(R.id.vibrationSwitch).setOnCheckedChangeListener { _, isChecked ->
            prefs.vibrationEnabled = isChecked
        }
    }
    
    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE)
    }
    
    private fun removeBackground() {
        prefs.backgroundImage = null
        findViewById<ImageView>(R.id.bgPreview).setImageResource(android.R.color.transparent)
        Toast.makeText(this, "Фон удалён ✓", Toast.LENGTH_SHORT).show()
    }
    
    private fun resetSettings() {
        prefs.reset()
        loadSettings()
        Toast.makeText(this, "Настройки сброшены ✓", Toast.LENGTH_SHORT).show()
    }
    
    private fun applyTheme(theme: String) {
        prefs.applyTheme(theme)
        val themeName = when (theme) {
            "light" -> "Светлая"
            "dark" -> "Тёмная"
            "blue" -> "Синяя"
            "green" -> "Зелёная"
            else -> ""
        }
        Toast.makeText(this, "Тема '$themeName' применена ✓", Toast.LENGTH_SHORT).show()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                saveImageToInternalStorage(uri)
            }
        }
    }
    
    private fun saveImageToInternalStorage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, "keyboard_bg_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            
            prefs.backgroundImage = file.absolutePath
            
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            findViewById<ImageView>(R.id.bgPreview).setImageBitmap(bitmap)
            
            Toast.makeText(this, "Фон установлен ✓", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}