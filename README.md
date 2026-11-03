# CPhone - Professional Android Code Editor

A high-performance, minimalist Android code editor engineered with pixel-level visual fidelity to replicate the clean, modern Android code editing experience.

---

## 📱 Features

- **Pixel-Accurate Top Navigation Bar:**
  - Left Home navigation button.
  - Center-left filename display with smooth ellipsis truncation (`netfetch-broadband...`).
  - Outline Information (ⓘ) button for file metadata (path, size, lines, characters, UTF-8 encoding).
  - Dark/Light theme toggle button featuring the split droplet icon.
  - Overflow (⋮) action menu.
  - Crisp light-grey divider underneath.

- **High-Performance Code Editing Area:**
  - Distinctive left vertical blue indicator bar.
  - Dynamic line numbers gutter with clean right alignment and subtle vertical separator.
  - Pure white background in Light theme, sleek dark background in Dark theme.
  - Monospaced programming font with comfortable line spacing.
  - Horizontal scrolling for long lines and synchronized vertical scrolling.
  - Auto-indentation preserving tabs and block scopes.

- **Multi-Language Syntax Highlighting:**
  - **Windows Batch / CMD (`.bat`, `.cmd`)**: Blue/cyan commands (`echo`, `reg`, `title`, `set`, `if`), green strings, red digits & IPs (`192.168.49.1`), grey comments.
  - **Python (`.py`)**
  - **Java (`.java`)**
  - **Kotlin (`.kt`, `.kts`)**
  - **JavaScript & TypeScript (`.js`, `.jsx`, `.ts`, `.tsx`)**
  - **HTML & XML (`.html`, `.xml`, `.svg`)**
  - **CSS (`.css`)**
  - **JSON (`.json`)**
  - **Shell Scripts (`.sh`, `.bash`)**
  - **C & C++ (`.c`, `.cpp`, `.h`)**

- **Real Editing Engine:**
  - Full cursor positioning, text selection, and native soft-keyboard support.
  - 200-level Undo / Redo history stack.
  - Find and Replace with live match counting and match navigation.
  - Storage Access Framework (SAF) integration for opening and saving any file on Android without requiring intrusive permissions.
  - Intent filter handling to open files directly from Android file managers.
