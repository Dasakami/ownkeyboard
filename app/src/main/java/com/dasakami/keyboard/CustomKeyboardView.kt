package com.dasakami.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.inputmethodservice.KeyboardView
import android.util.AttributeSet

class CustomKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : KeyboardView(context, attrs, defStyleAttr) {
    
    private val prefs = KeyboardPreferences(context)
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Можно добавить свою отрисовку если нужно
    }
}
