# CPhone Studio v2.0 — Commercial Android Code Editor & Mobile IDE

A commercial-grade, high-density Android code editor and mobile IDE engineered to rival **Acode** and **VS Code Mobile** with real-time bug inspection, **Code Trails**, multi-language execution, and calibrated mobile ergonomics.

---

## 🚀 What's New in CPhone v2.0

### 1. Code Trails & Live Bug Inspector
- **Live Syntax & Bug Diagnostics (`CodeAnalyzer`)**:
  - Real-time detection of unclosed brackets `()`, `{}`, `[]`, unclosed string literals, Python indentation (`TabError`) & missing `:` block headers, Python 2 `print` statements, accidental `=` in conditions, JSON trailing commas/parse errors, and mismatched HTML/XML tags.
  - **Gutter Error Dots & Right-Edge Bug Radar Minimap**: Visual red/amber markers in the line-number gutter and a right-edge minimap showing exact bug and cursor locations across the file.
  - **One-Tap Bug Pill (`● N` / `✓`)**: Tap the status pill in the top bar to open the **Code Trails & Bug Inspector** sheet and jump straight to the offending line and column.
- **Code Trail Micro-Bar**:
  - Displays live breadcrumbs (`fileName › ƒ activeFunction()`), **Cursor Trail Back/Forward (`←` / `→`)** navigation history, `Ln:Col` jump trigger, and instant `A-` / `A+` font calibration controls.
- **Symbol Outline Drawer**:
  - Automatically indexes classes, functions, methods, interfaces, HTML structural tags, CSS rules, Markdown headings, and Batch labels for instant jump-to-definition.

### 2. Renovated In-App Compiler & Multi-Language Runner
- **Main-Thread Safe WebView Bridge**: Marshals all `@JavascriptInterface` callbacks (`onStdout`, `onError`, `onFinished`, `onConsoleMessage`) onto Android's main UI thread and uses UTF-8 safe `TextDecoder` decoding.
- **Python 3 (Skulpt) Fixes & Upgrades**:
  - Interactive **`input()` support** via a pre-supplied Stdin queue or live interactive popup prompt dialog.
  - **Python Turtle Graphics Canvas (`import turtle`)** support rendered directly inside the runner.
  - Execution timeout guard (`25s` limit) to prevent infinite loops from freezing the device.
  - **Jump to Bug Line Button**: Automatically extracts traceback line numbers and lets you jump straight to the buggy line in one tap.
- **JavaScript & TypeScript V8 Async Runner**:
  - Built-in TypeScript type-stripping transpiler so `.ts` and `.tsx` files execute directly alongside `.js` and `async/await` scripts.
- **Multi-Tab HTML + CSS + JS Bundler & DevTools Console**:
  - Automatically links `<link rel="stylesheet" href="style.css">` and `<script src="app.js"></script>` from your open editor tabs so multi-file web projects run together seamlessly.
  - Interactive **DevTools JS Console** with live expression evaluation (`eval`).
- **CSS Live Sandbox & JSON Validator**:
  - Preview `.css` files against a live component sandbox and validate/pretty-print `.json` files.

### 3. Android Screen Calibration & Maximum Code View Space
- **Ultra-Compact Calibrated Bottom Dock (`36dp`)**:
  - Saves vertical screen space when the Android soft keyboard is open, with an `[▲/▼]` toggle to expand the precision cursor D-pad (`←`, `→`, `↑`, `↓`, `Home`, `End`) and line tools (`// Cmt`, `Dup Ln`, `Del Ln`, `Beautify`, `Snippets`).
- **Zen Fullscreen Mode (`⛶`)**:
  - One-tap fullscreen toggle in the top bar that collapses tab/trail chrome to dedicate 90%+ of the viewport to code.
- **Two-Finger Pinch-to-Zoom & Line Density Calibration**:
  - Pinch directly on the editor canvas or use `A-` / `A+` to calibrate font size (`9sp`–`26sp`) and line spacing (`1.25x`–`1.85x`).

### 4. Acode-Class Power Features
- **Command Palette (`⌘`)**: Searchable instant access to 20+ IDE commands (*Format Code*, *Go to Line*, *Toggle Comment*, *Duplicate/Delete/Move Line*, *Sort Lines*, *Trim Whitespace*, *Snippets*, *Switch Theme*, *Switch Language*).
- **6 Commercial IDE Color Themes**:
  - **One Dark Pro**, **Dracula Pro**, **Monokai Pro**, **Tokyo Night**, **Solarized Dark**, and **GitHub Light**.
- **20 Supported Languages & Session Auto-Persistence**:
  - Automatically saves open tabs and unsaved edits across app restarts.
