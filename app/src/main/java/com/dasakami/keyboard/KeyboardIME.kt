package com.dasakami.keyboard

import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import java.io.File

class KeyboardIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {
    
    private var keyboardView: CustomKeyboardView? = null
    private var keyboard: Keyboard? = null
    private var caps = false
    private var capsLock = false
    
    private lateinit var prefs: KeyboardPreferences
    private lateinit var clipboardMgr: ClipboardManager
    
    private lateinit var enKeyboard: Keyboard
    private lateinit var ruKeyboard: Keyboard
    private lateinit var progKeyboard: Keyboard
    private lateinit var symbolsKeyboard: Keyboard
    
    private var controlPanel: LinearLayout? = null
    
    private val deleteHandler = Handler(Looper.getMainLooper())
    private var isDeleting = false
    private val deleteRunnable = object : Runnable {
        override fun run() {
            currentInputConnection?.deleteSurroundingText(1, 0)
            deleteHandler.postDelayed(this, 50) 
        }
    }
    
    private var ctrlPressed = false
    private var altPressed = false
    
    override fun onCreateInputView(): View {
        prefs = KeyboardPreferences(this)
        clipboardMgr = ClipboardManager(this)
        
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        controlPanel = createControlPanel()
        mainLayout.addView(controlPanel)
        
        keyboardView = layoutInflater.inflate(R.layout.keyboard_view, null) as CustomKeyboardView
        
        enKeyboard = createKeyboard(R.xml.keyboard_en)
        ruKeyboard = createKeyboard(R.xml.keyboard_ru)
        progKeyboard = createKeyboard(R.xml.keyboard_prog)
        symbolsKeyboard = createKeyboard(R.xml.keyboard_symbols)
        
        keyboard = if (prefs.currentLanguage == "ru") ruKeyboard else enKeyboard
        keyboardView?.keyboard = keyboard
        keyboardView?.setOnKeyboardActionListener(this)
        
        applySettings()
        mainLayout.addView(keyboardView)
        
        return mainLayout
    }
    
    private fun createKeyboard(xmlRes: Int): Keyboard {
        return Keyboard(this, xmlRes)
    }
    
    private fun createControlPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(44)
            )
            setBackgroundColor(prefs.backgroundColor)
            setPadding(dpToPx(8), dpToPx(6), dpToPx(8), dpToPx(6))
            elevation = 4f
            
            addView(createPanelButton("🌐") { switchLanguage() })
            addView(createPanelButton("</>") { switchToMode("code") })
            addView(createPanelButton("123") { switchToMode("symbols") })
            
            addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, 1, 1f)
            })
            
            addView(createPanelButton("📋") { showClipboardMenu() })
            addView(createPanelButton("⚙") { showQuickSettings() })
            addView(createPanelButton("▼") { requestHideSelf(0) })
        }
    }
    
    private fun createPanelButton(text: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(42), dpToPx(38)).apply {
                setMargins(dpToPx(2), 0, dpToPx(2), 0)
            }
            gravity = android.view.Gravity.CENTER
            
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(8).toFloat()
                setColor(prefs.keyColor)
                setStroke(1, Color.parseColor("#CCCCCC"))
            }
            background = bg
            
            val tv = android.widget.TextView(context).apply {
                this.text = text
                textSize = 18f
                gravity = android.view.Gravity.CENTER
                setTextColor(prefs.textColor)
            }
            addView(tv)
            
            setOnClickListener {
                vibrate()
                onClick()
            }
        }
    }
    
    private fun switchLanguage() {
        prefs.currentLanguage = if (prefs.currentLanguage == "en") "ru" else "en"
        keyboard = if (prefs.currentLanguage == "ru") ruKeyboard else enKeyboard
        keyboardView?.keyboard = keyboard
        Toast.makeText(this, 
            if (prefs.currentLanguage == "ru") "Русский 🇷🇺" else "English 🇬🇧", 
            Toast.LENGTH_SHORT).show()
    }
    
    private fun switchToMode(mode: String) {
        keyboard = when (mode) {
            "code" -> if (keyboard == progKeyboard) {
                if (prefs.currentLanguage == "ru") ruKeyboard else enKeyboard
            } else progKeyboard
            "symbols" -> if (keyboard == symbolsKeyboard) {
                if (prefs.currentLanguage == "ru") ruKeyboard else enKeyboard
            } else symbolsKeyboard
            else -> enKeyboard
        }
        keyboardView?.keyboard = keyboard
    }
    
    private fun showClipboardMenu() {
        val history = clipboardMgr.getHistory()
        
        if (history.isEmpty()) {
            Toast.makeText(this, "История буфера пуста", Toast.LENGTH_SHORT).show()
            return
        }
        
        val items = history.map { 
            if (it.length > 30) it.take(30) + "..." else it
        }.toTypedArray()
        
        AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
            .setTitle("📋 Буфер обмена")
            .setItems(items) { _, which ->
                currentInputConnection?.commitText(history[which], 1)
            }
            .setNegativeButton("Очистить") { _, _ ->
                clipboardMgr.clearHistory()
                Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Отмена", null)
            .show()
    }
    
    private fun showQuickSettings() {
        val options = arrayOf(
            "📏 Размеры",
            "🖼️ Фон и темы",
            "🔊 Звук и вибрация",
            "⚙️ Полные настройки"
        )
        
        try {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Быстрые настройки")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> openSettings("size")
                        1 -> openSettings("theme")
                        2 -> openSettings("feedback")
                        3 -> openSettings("full")
                    }
                }
                .setNegativeButton("Отмена", null)
                .create()
                .apply {
                    window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                }
                .show()
        } catch (e: Exception) {
            openSettings("full")
        }
    }
    
    private fun openSettings(section: String = "full") {
        try {
            val intent = android.content.Intent(this, MainActivity::class.java)
            intent.putExtra("section", section)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка открытия настроек: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun applySettings() {
        keyboardView?.scaleX = prefs.keyboardScale
        keyboardView?.scaleY = prefs.keyboardScale
        keyboardView?.setBackgroundColor(prefs.backgroundColor)
        
        val bgPath = prefs.backgroundImage
        if (!bgPath.isNullOrEmpty() && File(bgPath).exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(bgPath)
                keyboardView?.background = BitmapDrawable(resources, bitmap)
            } catch (e: Exception) {
                keyboardView?.setBackgroundColor(prefs.backgroundColor)
            }
        }
    }
    
    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        playFeedback(primaryCode)
        
        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                if (ctrlPressed) {
                    deleteWord()
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            Keyboard.KEYCODE_SHIFT -> {
                handleShift()
            }
            Keyboard.KEYCODE_DONE -> sendKeyEvent(KeyEvent.KEYCODE_ENTER)
            Keyboard.KEYCODE_MODE_CHANGE -> switchToMode("symbols")
            Keyboard.KEYCODE_CANCEL -> requestHideSelf(0)
            
            -101 -> { ctrlPressed = !ctrlPressed; showModifierState() }
            -102 -> { altPressed = !altPressed; showModifierState() }
            -103 -> sendKeyEvent(KeyEvent.KEYCODE_TAB)
            -104 -> sendKeyEvent(KeyEvent.KEYCODE_ESCAPE)
            -105 -> ic.commitText("->", 1)
            -106 -> ic.commitText("{}", 1)
            -107 -> ic.commitText("[]", 1)
            -108 -> ic.commitText("()", 1)
            -109 -> sendKeyEvent(KeyEvent.KEYCODE_MOVE_HOME) 
            -110 -> sendKeyEvent(KeyEvent.KEYCODE_MOVE_END) 
            -111 -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_LEFT) 
            -112 -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_RIGHT) 
            -113 -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_UP) 
            -114 -> sendKeyEvent(KeyEvent.KEYCODE_DPAD_DOWN) 
            -115 -> sendKeyEvent(KeyEvent.KEYCODE_PAGE_UP) 
            -116 -> sendKeyEvent(KeyEvent.KEYCODE_PAGE_DOWN) 
            
            in -131..-120 -> {
                val fKey = KeyEvent.KEYCODE_F1 + (-120 - primaryCode)
                sendKeyEvent(fKey)
            }
            
            in -201..-200 -> {
                when (primaryCode) {
                    -200 -> sendCtrlKey(KeyEvent.KEYCODE_C) 
                    -201 -> sendCtrlKey(KeyEvent.KEYCODE_V) 
                }
            }
            
            else -> {
                var code = primaryCode.toChar()
                
                if (ctrlPressed) {
                    sendCtrlKey(primaryCode)
                    ctrlPressed = false
                    showModifierState()
                } else if (altPressed) {
                    sendAltKey(primaryCode)
                    altPressed = false
                    showModifierState()
                } else {
                    if (Character.isLetter(code) && (caps || capsLock)) {
                        code = Character.toUpperCase(code)
                    }
                    ic.commitText(code.toString(), 1)
                    
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.addPrimaryClipChangedListener {
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text?.toString()
                            if (!text.isNullOrBlank()) {
                                clipboardMgr.saveToHistory(text)
                            }
                        }
                    }
                    
                    if (caps && !capsLock) {
                        caps = false
                        keyboard?.isShifted = false
                        keyboardView?.invalidateAllKeys()
                    }
                }
            }
        }
    }
    
    private fun handleShift() {
        when {
            capsLock -> {
                capsLock = false
                caps = false
                Toast.makeText(this, "CAPS LOCK ВЫКЛ", Toast.LENGTH_SHORT).show()
            }
            caps -> {
                capsLock = true
                caps = true
                Toast.makeText(this, "CAPS LOCK ВКЛ", Toast.LENGTH_SHORT).show()
            }
            else -> {
                caps = true
                capsLock = false
            }
        }
        keyboard?.isShifted = caps
        keyboardView?.invalidateAllKeys()
    }
    
    private fun deleteWord() {
        val ic = currentInputConnection ?: return
        val textBefore = ic.getTextBeforeCursor(100, 0)
        if (textBefore.isNullOrEmpty()) return
        
        var deleteCount = 0
        for (i in textBefore.length - 1 downTo 0) {
            if (textBefore[i].isWhitespace()) {
                break
            }
            deleteCount++
        }
        ic.deleteSurroundingText(deleteCount, 0)
    }
    
    private fun sendKeyEvent(keyCode: Int) {
        val ic = currentInputConnection ?: return
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
    
    private fun sendCtrlKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        val event = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_CTRL_ON
        )
        ic.sendKeyEvent(event)
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
    
    private fun sendAltKey(keyCode: Int) {
        val ic = currentInputConnection ?: return
        val event = KeyEvent(
            0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, KeyEvent.META_ALT_ON
        )
        ic.sendKeyEvent(event)
        ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
    
    private fun showModifierState() {
        val state = mutableListOf<String>()
        if (ctrlPressed) state.add("Ctrl")
        if (altPressed) state.add("Alt")
        
        if (state.isNotEmpty()) {
            Toast.makeText(this, state.joinToString("+") + " активен", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun playFeedback(keyCode: Int) {
        if (prefs.vibrationEnabled) {
            vibrate()
        }
        
        if (prefs.soundEnabled) {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            when (keyCode) {
                Keyboard.KEYCODE_DELETE -> am.playSoundEffect(AudioManager.FX_KEYPRESS_DELETE)
                Keyboard.KEYCODE_DONE -> am.playSoundEffect(AudioManager.FX_KEYPRESS_RETURN)
                32 -> am.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR)
                else -> am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD)
            }
        }
    }
    
    private fun vibrate() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(prefs.vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(prefs.vibrationDuration)
        }
    }
    
    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
    
    override fun onPress(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE) {
            isDeleting = true
            deleteHandler.postDelayed(deleteRunnable, 500) 
        }
    }
    
    override fun onRelease(primaryCode: Int) {
        if (primaryCode == Keyboard.KEYCODE_DELETE && isDeleting) {
            isDeleting = false
            deleteHandler.removeCallbacks(deleteRunnable)
        }
    }
    
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() { switchLanguage() }
    override fun swipeDown() { requestHideSelf(0) }
    override fun swipeUp() {}
}
