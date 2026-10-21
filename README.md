# Input Overlay for Android

A fast, lightweight, and configurable Android overlay app designed to display accented characters (such as Italian vowels `à`, `è`, `é`, `ì`, `ò`, `ù`) and customizable symbols directly over your screen on demand. 

Optimized specifically for devices with physical keyboards (such as the **BlackBerry KEY2**, **Unihertz Titan**, and external Bluetooth/USB keyboards), it integrates seamlessly with **[KeyMapper](https://github.com/keymapperorg/KeyMapper)** to pop up instantly and inject characters directly into your active cursor—including within **Termux** and terminal emulators—without ever overwriting your clipboard.

---

## Key Features

- **Instant Floating Overlay:** Pops up on top of any active app without stealing application focus or closing your current context.
- **Direct Input (Zero Clipboard Overwrite):**
  - **Termux & Terminal Support:** Uses an integrated lightweight `InputMethodService` (`InputOverlayImeService`) communicating through `InputConnection.commitText()`. Characters are sent directly into Termux, shell sessions, and web views.
  - **Native Text Fields:** Directly splices characters at the cursor via `ACTION_SET_TEXT`.
  - **No Clipboard Abuse:** The system clipboard is never touched, modified, or polluted.
- **Physical Keyboard Row Shortcuts (`QWERTYUIOP`):**
  - Select characters instantly using the top letter row on your physical keyboard:
    | Shortcut | Character (Default) | <kbd>Shift</kbd> + Shortcut | Uppercase Result |
    | :---: | :---: | :---: | :---: |
    | <kbd>Q</kbd> | `à` | <kbd>Shift</kbd> + <kbd>Q</kbd> | `À` |
    | <kbd>W</kbd> | `è` | <kbd>Shift</kbd> + <kbd>W</kbd> | `È` |
    | <kbd>E</kbd> | `é` | <kbd>Shift</kbd> + <kbd>E</kbd> | `É` |
    | <kbd>R</kbd> | `ì` | <kbd>Shift</kbd> + <kbd>R</kbd> | `Ì` |
    | <kbd>T</kbd> | `ò` | <kbd>Shift</kbd> + <kbd>T</kbd> | `Ò` |
    | <kbd>Y</kbd> | `ù` | <kbd>Shift</kbd> + <kbd>Y</kbd> | `Ù` |
- **Instant Dismissal with <kbd>Esc</kbd>:** Pressing <kbd>Esc</kbd> on a physical keyboard or tapping outside immediately hides the overlay.
- **Natural 1:1 Drag & Positioning:** Reposition the floating bar anywhere on screen with natural tracking and boundary clamping (so the overlay never slips off-screen).
- **Accents for Italian & Other Languages:**
  - **Italian Accents (Default):** `à`, `è`, `é`, `ì`, `ò`, `ù` / `À`, `È`, `É`, `Ì`, `Ò`, `Ù`.
  - **Vowel Palettes:** Dedicated subsets for `e` (`è, é, ê, ë`), `a` (`à, á, â, ä, ã`), `i`, `o`, `u`.
  - **Spanish, French, German, Typography, Math, and Currency:** Switchable on the fly.
  - **Custom Symbols:** Specify your own custom characters via Intent extra (`characters="à,è,é,..."`).
- **KeyMapper Integration:** Full support for triggering via Broadcast, Service, or Activity intents.

---

## KeyMapper Setup Guide

[KeyMapper](https://github.com/keymapperorg/KeyMapper) allows you to map hardware buttons (like the BlackBerry KEY2 Speed Key, physical keyboard keys, volume keys, or combos) to Android actions.

### Step 1: Create a Trigger in KeyMapper
1. Open KeyMapper.
2. Tap the `+` button to create a new key map.
3. Record your desired trigger:
   - *Example 1:* Short-press or long-press on a key (e.g. `Speed Key + E` or long-press `E`).
   - *Example 2:* A key combination like `Alt + A`, `Ctrl + Grave`, or `Sym`.
   - *Example 3:* Double-tap volume down or convenience key.

### Step 2: Add Action
Tap **Add Action** ➜ choose **Send Intent** (or **Broadcast Intent**):

#### Recommended Broadcast Action:
- **Action:**
  ```text
  com.inputoverlay.action.SHOW_OVERLAY
  ```
  *(or `com.inputoverlay.action.TOGGLE_OVERLAY` to toggle visibility on and off)*

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
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY -p com.inputoverlay

# Toggle overlay
adb shell am broadcast -a com.inputoverlay.action.TOGGLE_OVERLAY -p com.inputoverlay

# Show only 'e' accents (è, é, ê, ë)
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY -p com.inputoverlay --es category "e"

# Show custom symbols
adb shell am broadcast -a com.inputoverlay.action.SHOW_OVERLAY -p com.inputoverlay --es characters "«»“”°±"

# Hide overlay
adb shell am broadcast -a com.inputoverlay.action.HIDE_OVERLAY -p com.inputoverlay
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
