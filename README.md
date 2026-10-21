# Input Overlay for Android

A fast, lightweight, and configurable Android overlay app designed to display accented characters (such as Italian vowels `à`, `è`, `é`, `ì`, `ò`, `ù`) and customizable symbols directly over your screen on demand. It can be triggered instantly via **KeyMapper**, hardware keys, keyboard shortcuts, or ADB intents, and injects characters directly into your active input field using Android's Accessibility framework or clipboard.

---

## Features

- **Instant Floating Overlay:** Pops up on top of any active app without stealing application focus or closing your current context.
- **Accents for Italian & Other Languages:**
  - **Italian Accents (Default):** `à`, `è`, `é`, `ì`, `ò`, `ù` (lowercase) and `À`, `È`, `É`, `Ì`, `Ò`, `Ù` (uppercase).
  - **Vowel Palettes:** Dedicated subsets for `e` (`è, é, ê, ë`), `a` (`à, á, â, ä, ã`), etc.
  - **Spanish, French, German, Typography, Math, and Currency:** Switchable on the fly.
  - **Custom Symbols:** Specify your own custom characters on the fly via Intent extra (`characters="à,è,é,..."`).
- **Direct Text Injection:** Injects the selected character straight into the active cursor position in any app via an integrated Accessibility Service (`ACTION_PASTE` and `ACTION_SET_TEXT`). Fallback to clipboard copy if accessibility is disabled.
- **Physical Keyboard Support:** When the overlay is open, press number keys `1`–`9` on an external hardware keyboard to instantly pick the corresponding character!
- **KeyMapper Integration:** Full support for triggering via Broadcast, Service, or Activity intents.
- **Draggable & Auto-Dismiss:** Freely reposition the floating bar, or tap outside / select a character to dismiss automatically.

---

## KeyMapper Setup Guide

[KeyMapper](https://github.com/keymapperorg/KeyMapper) allows you to map hardware buttons (like volume keys, physical keyboard keys, mouse buttons, or Bluetooth controllers) to Android actions.

### Step 1: Create a Trigger in KeyMapper
1. Open KeyMapper.
2. Tap the `+` button to create a new key map.
3. Record your desired trigger:
   - *Example 1:* Long-press a vowel key on a physical keyboard (e.g. long-press `E`).
   - *Example 2:* A key combination like `Alt + A`, `Ctrl + Grave`, or `Caps Lock`.
   - *Example 3:* Double-tap volume down or a headset button.

### Step 2: Add Action
Tap **Add Action** ➜ choose **Send Intent** (or **Broadcast Intent**):

#### Recommended Broadcast Action:
- **Action:**
  ```text
  com.inputoverlay.action.SHOW_OVERLAY
  ```
  *(or `com.inputoverlay.action.TOGGLE_OVERLAY` if you want pressing the key again to close the overlay)*

#### Optional Parameters (Extras):
You can pass custom Intent Extras to show specific characters or categories:

| Extra Key | Type | Example Values | Description |
| :--- | :--- | :--- | :--- |
| `category` | String | `italian`, `e`, `a`, `spanish`, `french`, `german`, `math` | Loads preset palette (e.g. `e` shows `è, é, ê, ë`) |
| `characters` | String | `à,è,é,ì,ò,ù` or `«»“”` | Loads arbitrary custom characters in the overlay |
| `case` | String | `upper`, `lower`, `toggle` | Forces uppercase or lowercase display |

#### Alternative: Start Activity / Service
- **Target Activity:** `com.inputoverlay/.ui.OverlayActivity`
- **Target Service:** `com.inputoverlay/.service.OverlayService`

---

## Testing via ADB

You can test the overlay directly from your terminal using ADB:

```bash
# Show Italian accents
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY

# Toggle overlay
adb shell am broadcast -a com.inputoverlay.action.TOGGLE_OVERLAY

# Show only 'e' accents (è, é, ê, ë)
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY --es category "e"

# Show custom symbols
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY --es characters "«»“”°±"

# Hide overlay
adb shell am broadcast -a com.inputoverlay.action.HIDE_OVERLAY
```

---

## Building from Source

To build the APK:
```bash
./gradlew assembleDebug
```
The output APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

To run tests:
```bash
./gradlew testDebugUnitTest
```
