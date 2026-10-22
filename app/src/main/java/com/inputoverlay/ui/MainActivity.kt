package com.inputoverlay.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.inputoverlay.R
import com.inputoverlay.databinding.ActivityMainBinding
import com.inputoverlay.model.PresetData
import com.inputoverlay.service.InputOverlayAccessibilityService
import com.inputoverlay.service.OverlayService
import com.inputoverlay.utils.KeyboardShortcutHelper
import com.inputoverlay.utils.OverlayPreferences

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: OverlayPreferences

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        updatePermissionStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = OverlayPreferences(this)

        setupButtons()
        setupPreferences()
        setupKeyMapperGuide()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupButtons() {
        // Overlay permission button
        binding.btnGrantOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
        }

        // Accessibility service button
        binding.btnGrantAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(
                this,
                "Find 'Input Overlay Text Insertion' and enable it",
                Toast.LENGTH_LONG
            ).show()
        }

        // IME button
        binding.btnGrantIme.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Enable 'Input Overlay Keyboard' in Virtual Keyboards", Toast.LENGTH_LONG).show()
        }

        // Battery optimization button
        binding.btnGrantBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    @SuppressLint("BatteryLife")
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        }

        // Test overlay button
        binding.btnTestOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Please grant Overlay permission first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_SHOW
            }
            ContextCompat.startForegroundService(this, intent)
        }

        // Hide overlay button
        binding.btnHideOverlay.setOnClickListener {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_HIDE
            }
            ContextCompat.startForegroundService(this, intent)
        }
    }

    private fun setupPreferences() {
        // Switches
        binding.switchAutoDismiss.isChecked = prefs.isAutoDismissEnabled
        binding.switchAutoDismiss.setOnCheckedChangeListener { _, isChecked ->
            prefs.isAutoDismissEnabled = isChecked
        }

        binding.switchHaptic.isChecked = prefs.isHapticEnabled
        binding.switchHaptic.setOnCheckedChangeListener { _, isChecked ->
            prefs.isHapticEnabled = isChecked
        }

        binding.switchKeyboardShortcuts.isChecked = prefs.isKeyboardShortcutsEnabled
        binding.layoutShortcutConfig.visibility = if (prefs.isKeyboardShortcutsEnabled) View.VISIBLE else View.GONE
        binding.switchKeyboardShortcuts.setOnCheckedChangeListener { _, isChecked ->
            prefs.isKeyboardShortcutsEnabled = isChecked
            binding.layoutShortcutConfig.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.switchDefaultUpper.isChecked = prefs.isDefaultUppercase

        // Presets Chips
        when (prefs.defaultPresetId) {
            "spanish" -> binding.chipSpanish.isChecked = true
            "french" -> binding.chipFrench.isChecked = true
            "german" -> binding.chipGerman.isChecked = true
            "typography" -> binding.chipSymbols.isChecked = true
            else -> binding.chipItalian.isChecked = true
        }

        // Live preview update helper
        fun updateShortcutPreview() {
            val currentPalette = PresetData.findCategoryById(prefs.defaultPresetId)
            val currentShortcuts = KeyboardShortcutHelper.parseShortcutKeys(binding.etShortcutKeys.text?.toString())
            val chars = currentPalette.items.map { it.getDisplay(prefs.isDefaultUppercase) }
            binding.tvShortcutMappingPreview.text = KeyboardShortcutHelper.formatMappingPreview(currentShortcuts, chars)
        }

        // Setup shortcut keys input and layout selection
        binding.etShortcutKeys.setText(prefs.shortcutKeys)

        when (prefs.shortcutLayout) {
            KeyboardShortcutHelper.LAYOUT_QWERTZ -> binding.chipLayoutQwertz.isChecked = true
            KeyboardShortcutHelper.LAYOUT_AZERTY -> binding.chipLayoutAzerty.isChecked = true
            KeyboardShortcutHelper.LAYOUT_CUSTOM -> binding.chipLayoutCustom.isChecked = true
            else -> binding.chipLayoutQwerty.isChecked = true
        }

        updateShortcutPreview()

        binding.switchDefaultUpper.setOnCheckedChangeListener { _, isChecked ->
            prefs.isDefaultUppercase = isChecked
            updateShortcutPreview()
        }

        var isUpdatingTextInternally = false
        binding.chipGroupShortcuts.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty() || isUpdatingTextInternally) return@setOnCheckedStateChangeListener
            val selectedLayout = when (checkedIds.first()) {
                R.id.chip_layout_qwertz -> KeyboardShortcutHelper.LAYOUT_QWERTZ
                R.id.chip_layout_azerty -> KeyboardShortcutHelper.LAYOUT_AZERTY
                R.id.chip_layout_custom -> KeyboardShortcutHelper.LAYOUT_CUSTOM
                else -> KeyboardShortcutHelper.LAYOUT_QWERTY
            }
            prefs.shortcutLayout = selectedLayout
            if (selectedLayout != KeyboardShortcutHelper.LAYOUT_CUSTOM) {
                val presetKeys = KeyboardShortcutHelper.getPresetShortcuts(selectedLayout).joinToString(", ")
                isUpdatingTextInternally = true
                binding.etShortcutKeys.setText(presetKeys)
                prefs.shortcutKeys = presetKeys
                isUpdatingTextInternally = false
                updateShortcutPreview()
            }
        }

        binding.etShortcutKeys.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdatingTextInternally) return
                val text = s?.toString() ?: ""
                prefs.shortcutKeys = text
                val parsed = KeyboardShortcutHelper.parseShortcutKeys(text)

                val matchedLayout = when (parsed) {
                    KeyboardShortcutHelper.PRESET_QWERTY -> KeyboardShortcutHelper.LAYOUT_QWERTY
                    KeyboardShortcutHelper.PRESET_QWERTZ -> KeyboardShortcutHelper.LAYOUT_QWERTZ
                    KeyboardShortcutHelper.PRESET_AZERTY -> KeyboardShortcutHelper.LAYOUT_AZERTY
                    else -> KeyboardShortcutHelper.LAYOUT_CUSTOM
                }
                prefs.shortcutLayout = matchedLayout
                isUpdatingTextInternally = true
                when (matchedLayout) {
                    KeyboardShortcutHelper.LAYOUT_QWERTZ -> binding.chipLayoutQwertz.isChecked = true
                    KeyboardShortcutHelper.LAYOUT_AZERTY -> binding.chipLayoutAzerty.isChecked = true
                    KeyboardShortcutHelper.LAYOUT_CUSTOM -> binding.chipLayoutCustom.isChecked = true
                    else -> binding.chipLayoutQwerty.isChecked = true
                }
                isUpdatingTextInternally = false
                updateShortcutPreview()
            }
        })

        binding.chipGroupPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val selectedPreset = when (checkedIds.first()) {
                R.id.chip_spanish -> "spanish"
                R.id.chip_french -> "french"
                R.id.chip_german -> "german"
                R.id.chip_symbols -> "typography"
                else -> "italian"
            }
            prefs.defaultPresetId = selectedPreset
            updateShortcutPreview()
            Toast.makeText(this, "Default palette set to: $selectedPreset", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupKeyMapperGuide() {
        binding.btnCopyShowAction.setOnClickListener {
            copyTextToClipboard(OverlayService.ACTION_SHOW, "Show Action")
        }

        binding.btnCopyToggleAction.setOnClickListener {
            copyTextToClipboard(OverlayService.ACTION_TOGGLE, "Toggle Action")
        }
    }

    private fun copyTextToClipboard(text: String, label: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, getString(R.string.copied_to_clipboard, text), Toast.LENGTH_SHORT).show()
    }

    private fun updatePermissionStatus() {
        // Overlay permission
        val hasOverlay = Settings.canDrawOverlays(this)
        if (hasOverlay) {
            binding.ivOverlayStatus.setColorFilter(getColor(R.color.status_green))
            binding.btnGrantOverlay.isEnabled = false
            binding.btnGrantOverlay.text = getString(R.string.btn_granted)
        } else {
            binding.ivOverlayStatus.setColorFilter(getColor(R.color.status_amber))
            binding.btnGrantOverlay.isEnabled = true
            binding.btnGrantOverlay.text = getString(R.string.btn_grant)
        }

        // Accessibility service
        val hasAccessibility = InputOverlayAccessibilityService.isRunning || isAccessibilityServiceEnabled()
        if (hasAccessibility) {
            binding.ivAccessibilityStatus.setColorFilter(getColor(R.color.status_green))
            binding.btnGrantAccessibility.isEnabled = false
            binding.btnGrantAccessibility.text = getString(R.string.btn_granted)
        } else {
            binding.ivAccessibilityStatus.setColorFilter(getColor(R.color.status_amber))
            binding.btnGrantAccessibility.isEnabled = true
            binding.btnGrantAccessibility.text = getString(R.string.btn_grant)
        }

        // Input Method (IME) status for Termux
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        val isImeEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        if (isImeEnabled) {
            binding.ivImeStatus.setColorFilter(getColor(R.color.status_green))
            binding.btnGrantIme.isEnabled = false
            binding.btnGrantIme.text = getString(R.string.btn_granted)
        } else {
            binding.ivImeStatus.setColorFilter(getColor(R.color.status_amber))
            binding.btnGrantIme.isEnabled = true
            binding.btnGrantIme.text = getString(R.string.btn_grant)
        }

        // Battery optimization
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBattery = powerManager.isIgnoringBatteryOptimizations(packageName)
        if (isIgnoringBattery) {
            binding.ivBatteryStatus.setColorFilter(getColor(R.color.status_green))
            binding.btnGrantBattery.isEnabled = false
            binding.btnGrantBattery.text = getString(R.string.btn_granted)
        } else {
            binding.ivBatteryStatus.setColorFilter(getColor(R.color.status_amber))
            binding.btnGrantBattery.isEnabled = true
            binding.btnGrantBattery.text = getString(R.string.btn_grant)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.packageName == packageName &&
                    it.resolveInfo.serviceInfo.name == InputOverlayAccessibilityService::class.java.name
        }
    }
}
