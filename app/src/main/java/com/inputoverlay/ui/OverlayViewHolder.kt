package com.inputoverlay.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.inputoverlay.R
import com.inputoverlay.model.SymbolCategory
import com.inputoverlay.utils.OverlayPreferences

class OverlayViewHolder(
    private val context: Context,
    private val prefs: OverlayPreferences,
    private val windowManager: WindowManager?,
    private val layoutParams: WindowManager.LayoutParams?
) {

    val rootView: View = LayoutInflater.from(context).inflate(R.layout.layout_overlay_panel, null)
    private val containerSymbols: LinearLayout = rootView.findViewById(R.id.container_symbols)
    private val btnShift: ImageButton = rootView.findViewById(R.id.btn_shift)
    private val btnDrag: ImageView = rootView.findViewById(R.id.btn_drag)
    private val btnCategory: TextView = rootView.findViewById(R.id.btn_category_chip)
    private val btnClose: ImageButton = rootView.findViewById(R.id.btn_close)

    private var currentItems: List<String> = emptyList()

    init {
        setupDragListener()
    }

    fun bind(
        category: SymbolCategory,
        isUppercase: Boolean,
        onCharacterSelected: (String) -> Unit,
        onShiftToggled: (Boolean) -> Unit,
        onCategorySwitchRequested: () -> Unit,
        onCloseRequested: () -> Unit
    ) {
        // Shift icon styling
        btnShift.setColorFilter(
            if (isUppercase) context.getColor(R.color.overlay_accent) else context.getColor(R.color.overlay_text_secondary)
        )
        btnShift.setOnClickListener {
            onShiftToggled(!isUppercase)
        }

        // Category chip
        btnCategory.text = category.shortCode
        btnCategory.setOnClickListener {
            onCategorySwitchRequested()
        }

        // Close button
        btnClose.setOnClickListener {
            onCloseRequested()
        }

        // Populate character buttons
        containerSymbols.removeAllViews()
        val inflater = LayoutInflater.from(context)
        val showShortcuts = prefs.isKeyboardShortcutsEnabled
        val characters = category.items.map { it.getDisplay(isUppercase) }
        currentItems = characters

        characters.forEachIndexed { index, char ->
            val itemView = inflater.inflate(R.layout.item_symbol_button, containerSymbols, false)
            val tvSymbol = itemView.findViewById<TextView>(R.id.tv_symbol)
            val tvShortcut = itemView.findViewById<TextView>(R.id.tv_shortcut)

            tvSymbol.text = char
            tvSymbol.setOnClickListener {
                onCharacterSelected(char)
            }

            if (showShortcuts && index < 9) {
                tvShortcut.visibility = View.VISIBLE
                tvShortcut.text = (index + 1).toString()
            } else {
                tvShortcut.visibility = View.GONE
            }

            containerSymbols.addView(itemView)
        }

        // Hardware keyboard listener
        rootView.isFocusable = true
        rootView.isFocusableInTouchMode = true
        rootView.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_BACK -> {
                        onCloseRequested()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        onShiftToggled(!isUppercase)
                        return@setOnKeyListener true
                    }
                    in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> {
                        val selectedIndex = keyCode - KeyEvent.KEYCODE_1
                        if (selectedIndex < characters.size) {
                            onCharacterSelected(characters[selectedIndex])
                            return@setOnKeyListener true
                        }
                    }
                }
            }
            false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragListener() {
        if (windowManager == null || layoutParams == null) return

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        btnDrag.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()
                    layoutParams.x = initialX + deltaX
                    layoutParams.y = initialY + deltaY
                    windowManager.updateViewLayout(rootView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // Save dragged position
                    prefs.overlayPosX = layoutParams.x
                    prefs.overlayPosY = layoutParams.y
                    true
                }
                else -> false
            }
        }
    }
}
