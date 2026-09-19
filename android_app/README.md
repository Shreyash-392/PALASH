# 🌺 PALASH VoiceBridge

**AI-Powered Vernacular Pedagogy and Real-Time Translation Tool for Mother Tongue-Based Primary Education**

> *“Teach in the language children understand.”*

PALASH VoiceBridge is a **100% offline-first Android application** built for primary-school teachers in tribal regions of India. It bridges the communication gap between Hindi-speaking teachers and Santali-speaking children using **AI4Bharat IndicTrans2**, **AI4Bharat IndicConformer**, and **Ol Chiki (ᱚᱞ ᱪᱤᱠᱤ) script** pedagogy.

---

## 🚀 Key Multimodal Features

- 🎙️ **Voice Bridge (Spoken Translation)**: Speak Hindi classroom instructions and hear Santali audio output with real-time Ol Chiki script rendering (< 1.0s latency).
- ✍️ **Text Translation Playground**: Type any custom Hindi sentence or tap quick classroom phrase chips to translate immediately with live latency metrics.
- 📚 **FLN Bilingual Lesson Library**: 10 structured Class 1–2 Mathematics & Language lessons with step-by-step teacher prompts, audio pronunciation, and on-demand activity translation.
- 🖨️ **Printable A4 PDF Worksheets**: Generate and share clean, bilingual printable PDF worksheets across 7 question types (*Counting, Addition, Matching, Fill in the Blank, MCQ, Circle Answer, Comparison*) without internet.
- 🃏 **Interactive Flashcards**: 5 foundational categories (*Numbers, Animals, Colors, Shapes, Objects*) with tap-to-flip active recall animation and acoustic pronunciation.
- 🎯 **SIH 2026 Live Demo Showcase**: Dedicated 1-tap evaluator presentation mode for hackathon judging.
- 🔒 **100% Offline & Private**: `android.permission.INTERNET` explicitly removed. Works in complete **Airplane Mode** on 2GB RAM Android tablets.

---

## 🏗️ Architecture & Tech Stack

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   PALASH VoiceBridge Architecture                      │
├──────────────────────────────────┬─────────────────────────────────────┤
│  UI Layer                        │  Jetpack Compose + Material 3       │
│  Architecture                    │  MVVM + Repository Pattern          │
│  Offline Speech Recognition      │  AI4Bharat IndicConformer (16kHz)   │
│  Offline Neural Translation      │  AI4Bharat IndicTrans2 (hin → sat)  │
│  Offline Audio / TTS             │  Ol Chiki Phonetic Waveform Synth   │
│  Database & Persistence          │  Room SQLite (4 Tables)             │
│  PDF Engine                      │  Android Native PdfDocument         │
│  ML Inference Runtime            │  ONNX Runtime Android (C++ JNI)     │
└──────────────────────────────────┴─────────────────────────────────────┘
```

---

## 📱 Installation & Quick Start

### Prerequisites
- Android 9.0+ (API level 28+)
- 2 GB RAM minimum

### 1. Install APK directly
Copy `app/build/outputs/apk/debug/app-debug.apk` to your Android device and tap to install.

### 2. Or install via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run in Airplane Mode
1. Enable **Airplane Mode** on your device (Wi-Fi OFF, SIM Data OFF).
2. Open **PALASH VoiceBridge**.
3. Tap **🎯 SIH Live Demo Showcase** or any module to test.

---

## 🧪 Testing & Verification

Run the automated test suite locally:
```bash
./gradlew testDebugUnitTest
```

Compile the debug APK:
```bash
./gradlew assembleDebug
```

---

## 📂 Documentation

- [Phase 1 Documentation](docs/phase1.md) — Foundation, UI & Database
- [Phase 2 Documentation](docs/phase2.md) — Offline IndicTrans2 Translation Engine
- [Phase 3 Documentation](docs/phase3.md) — Offline Speech Recognition & Audio Pipeline
- [Phase 4 Documentation](docs/phase4.md) — PDF Worksheet Generator & Flashcard Drill
- [Phase 5 Documentation](docs/phase5.md) — Final Integration, Hardening & Evaluation Guide

---

## 📜 License & Hackathon Context

Developed for the **Smart India Hackathon (SIH 2026)**.
Built with reference to open-source models from [AI4Bharat](https://ai4bharat.iitm.ac.in/).
