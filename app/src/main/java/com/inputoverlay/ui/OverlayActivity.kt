package com.inputoverlay.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.inputoverlay.databinding.ActivityOverlayBinding
import com.inputoverlay.model.PresetData
import com.inputoverlay.model.SymbolCategory
import com.inputoverlay.service.OverlayService
import com.inputoverlay.utils.OverlayPreferences
import com.inputoverlay.utils.TextInjector

class OverlayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayBinding
    private lateinit var prefs: OverlayPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = OverlayPreferences(this)

        // If overlay permission is available, delegate immediately to OverlayService for true system overlay
        if (Settings.canDrawOverlays(this)) {
            val serviceIntent = Intent(this, OverlayService::class.java).apply {
                action = intent.action ?: OverlayService.ACTION_SHOW
                intent.extras?.let { putExtras(it) }
            }
            ContextCompat.startForegroundService(this, serviceIntent)
            finish()
            overridePendingTransition(0, 0)
            return
        }

        // Fallback: If overlay permission not yet granted, display in-activity translucent overlay
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val categoryId = intent.getStringExtra(OverlayService.EXTRA_CATEGORY)
        val customChars = intent.getStringExtra(OverlayService.EXTRA_CHARACTERS)

        var category = if (!customChars.isNullOrBlank()) {
            SymbolCategory("custom", "Custom", "★", PresetData.parseCustomCharacters(customChars))
        } else {
            PresetData.findCategoryById(categoryId ?: prefs.defaultPresetId)
        }

        var isUppercase = prefs.isDefaultUppercase

        val viewHolder = OverlayViewHolder(this, prefs, null, null)
        fun updateView() {
            viewHolder.bind(
                category = category,
                isUppercase = isUppercase,
                onCharacterSelected = { char ->
                    TextInjector.injectOrCopy(this, char, category.id, prefs)
                    if (prefs.isAutoDismissEnabled) {
                        finish()
                    }
                },
                onShiftToggled = { newShift ->
                    isUppercase = newShift
                    updateView()
                },
                onCategorySwitchRequested = {
                    val presets = PresetData.ALL_PRESETS
                    val idx = presets.indexOfFirst { it.id == category.id }
                    category = presets[(idx + 1) % presets.size]
                    updateView()
                },
                onCloseRequested = {
                    finish()
                }
            )
        }

        binding.overlayActivityRoot.removeAllViews()
        binding.overlayActivityRoot.addView(viewHolder.rootView)
        binding.overlayActivityRoot.setOnClickListener {
            finish()
        }

        updateView()
    }
}
