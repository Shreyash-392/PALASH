# 🌿 PALASH VoiceBridge: Multimodal Offline Tribal Language Assistant

> **Empowering Education & Communication in Indigenous Languages (Santali Ol Chiki & Mundari Devanagari)**  
> 100% Offline Multi-modal Android Solution: ASR + NMT Neural Translation + Native Speaker TTS + Interactive FLN Worksheets + Video Lessons.

---

## 📌 Project Overview

**PALASH VoiceBridge** is an offline multimodal educational assistant engineered for schools, teachers, and student communities in tribal regions. It bridges the linguistic divide by enabling seamless bidirectional translation and interactive learning between **Hindi**, **Santali (Ol Chiki script)**, and **Mundari (Devanagari script)** without requiring any internet connection.

---

## 🌟 Key Features & Capabilities

### 🗣️ 1. Dual-Language Voice Translation Slate
- **Santali (sat_Olck):** Instant human-verified database lookup + IndicTrans2 distilled neural transliterator into **Ol Chiki script**.
- **Mundari (unr_Deva):** Quantized **M2M100 418M ONNX INT8** neural network with zero-copy JNI memory optimizations & hybrid 0ms verified phrase lookup.
- **Offline Speech-to-Text (ASR):** Voice dictation in Hindi using Vosk offline speech recognition.
- **Direct Media Speaker Audio (TTS):** Spoken audio played directly through the device speaker for both Santali (Ol Chiki → Devanagari transliterated speech) and Mundari.

---

### 📝 2. PALASH Setu Interactive Worksheets (FLN Section)
Designed for Foundational Literacy & Numeracy (FLN) in primary education:
- **Bilingual Topics:**
  - 💧 **Water Cycle (जल चक्र / दाः चक्र)**
  - 🧍 **Human Body (मानव शरीर / हड़मो)**
  - 🌱 **Plant Growth / Seed Germination (पौधे का विकास / दारु हाराओः)**
- **Dynamic Script Toggle:** Instant 1-tap switching between **Ol Chiki (Santali)** and **Devanagari (Mundari/Hindi)** scripts.
- **Offline PDF Generator:** Generate and export print-ready PDF worksheets directly from the device using Android's native `PdfDocument` engine.

---

### 🎥 3. Offline Video Lessons (Multimedia Classroom Section)
- **Embedded Demonstration Video:** Native offline video player section rendering introductory video lessons (`palash_intro_video.mp4`).
- **Zero Internet Requirement:** Video assets are bundled locally for seamless playback in remote rural classrooms.

---

### ⚙️ 4. Hybrid Dual-Tier Translation Engine
```
                  ┌─────────────────────────────────────────┐
                  │          Hindi Input Sentence           │
                  └────────────────────┬────────────────────┘
                                       │
                         Is phrase in Verified DB?
                                      /                                 Yes  /   \ No
                                    /                                        ▼       ▼
    ┌──────────────────────────────────┐  ┌──────────────────────────────────┐
    │  TIER 1: Instant Database Match  │  │   TIER 2: Optimized ONNX Neural  │
    │   - Latency: 0 ms (Instant)      │  │   - Latency: ~1.5 seconds        │
    │   - Memory: 0 MB extra RAM       │  │   - RAM: Reduced from 1.15GB → 500MB│
    │   - Stability: 100% Bulletproof  │  │   - Tokenizer: Viterbi-aligned   │
    └──────────────────────────────────┘  └──────────────────────────────────┘
```

---

## 📂 Repository Structure

```
PALASH_VoiceBridge/
├── android_app/               # Complete Android Application
│   ├── app/src/main/java/com/palash/voicebridge/
│   │   ├── data/              # Room DB, Verified Phrase DAO (0ms Instant Lookup), Repositories
│   │   ├── domain/            # Domain Models & Engine Interfaces
│   │   ├── ml/                # ONNX INT8 Engine, SentencePiece Tokenizer, Vosk ASR, Direct Speaker TTS
│   │   ├── navigation/        # App Screen Navigation & Routes
│   │   ├── ui/                # Compose UI Screens:
│   │   │   ├── voice/         # Voice & Text Translation Slate
│   │   │   ├── worksheet/     # Interactive Bilingual Worksheets & PDF Export
│   │   │   ├── video/         # Offline Video Lessons Player
│   │   │   ├── home/          # Main Navigation Hub
│   │   │   └── settings/      # System Diagnostics & Engine Status
│   │   └── util/              # Native Android PdfDocument Generator
│   └── app/src/main/res/      # UI Layouts, Vector Icons, & Embedded Video Assets
│
├── ml_training_and_export/    # Machine Learning Pipeline
│   ├── MundariTranslation/    # PyTorch M2M100 Training & Fine-Tuning Scripts
│   └── android_export/        # ONNX INT8 Quantization & Vocab Files
│
├── .gitignore                 # Excludes heavy model weights, datasets, & build binaries
└── README.md                  # Project Documentation
```

---

## 🚀 Quick Setup & Usage Guide

### 📱 Option 1: Direct Install via APK (For Judges / End Users)
1. Download **`palash-release.apk`** to your Android phone (Android 9 to 12+) and tap **Install**.
2. Copy the exported `onnx_int8` model folder to your phone's storage:
   👉 **`Internal Storage` → `Download` → `onnx_int8`**  
   *(containing `encoder_model.onnx`, `decoder_model.onnx`, `decoder_with_past_model.onnx`, `vocab.json`)*
3. Launch **PALASH** and start translating!

---

### 💻 Option 2: Run from Source (For Developers in Android Studio)
1. Clone this repository:
   ```bash
   git clone https://github.com/Shreyash-392/PALASH.git
   ```
2. Open **Android Studio** → select `Open` → select the `android_app` folder.
3. Connect your Android phone via USB (with **USB Debugging** enabled).
4. Click **Run ▶️** (`Shift + F10`) to compile and launch.

---

## 📄 License
This project is developed for educational and humanitarian purposes under the MIT License.
