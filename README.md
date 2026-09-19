# 🌿 PALASH VoiceBridge: Offline Multimodal Tribal Language Assistant

> **Empowering Education & Communication in Tribal Languages (Santali Ol Chiki & Mundari Devanagari)**  
> 100% Offline Multimodal Pipeline: ASR + Neural Machine Translation (NMT) + TTS + Interactive FLN Worksheets + Video Lessons.

---

## 📌 Project Overview

**PALASH VoiceBridge** is an AI-powered offline educational assistant engineered for schools, teachers, and communities in tribal regions. It bridges the linguistic divide by enabling seamless bidirectional translation and interactive learning between **Hindi**, **Santali (Ol Chiki script)**, and **Mundari (Devanagari script)** without requiring internet connectivity.

### 🌟 Key Highlights
- **100% Offline Multimodal Architecture:** Speech Recognition (ASR), Neural Translation (NMT), and Text-to-Speech (TTS) run entirely on-device.
- **Dual-Language NMT:**
  - **Santali (sat_Olck):** Instant pre-verified database + IndicTrans2 distilled neural transliterator.
  - **Mundari (unr_Deva):** Quantized **M2M100 418M ONNX INT8** neural network with zero-copy JNI memory optimizations.
- **Interactive Setu Worksheets:** FLN-aligned educational worksheets (Water Cycle, Human Body, Plant Growth) with bilingual Ol Chiki / Devanagari script switching and offline PDF export.
- **Offline Video Lessons:** Embedded video demonstrations for interactive classroom learning.
- **Direct Speaker TTS Audio:** Speech output played directly through the device media speaker using Android's native speech engine.

---

## 📁 Repository Structure

```
PALASH-VoiceBridge/
├── android_app/               # Complete Android App (Jetpack Compose, Kotlin, ONNX, Vosk, TTS)
│   ├── app/src/main/java/com/palash/voicebridge/
│   │   ├── data/              # Database, Repositories, Content Loader
│   │   ├── domain/            # Domain Models & Engine Interfaces
│   │   ├── ml/                # ONNX Inference Engine, SentencePiece Tokenizer, ASR, TTS
│   │   ├── navigation/        # App Screen Routing
│   │   ├── ui/                # Compose UI Screens (Voice, Worksheets, Video, Settings)
│   │   └── util/              # Worksheet PDF Generator & Utilities
│   └── app/src/main/res/      # UI Resources & Offline Media Assets
│
├── ml_training_and_export/    # Machine Learning Pipeline
│   ├── MundariTranslation/    # PyTorch M2M100 Training & Quantization Scripts
│   └── android_export/        # ONNX INT8 Quantized Models & Vocab
│
├── .gitignore                 # Excludes heavy build binaries (.gradle, build/, .venv, *.apk)
└── README.md                  # Project Documentation
```

---

## 🚀 Quick Setup & Installation

### 1. Android App Setup (Android Studio)
1. Clone this repository:
   ```bash
   git clone https://github.com/YourUsername/PALASH-VoiceBridge.git
   ```
2. Open **Android Studio** and select `Open Project` → select the `android_app` directory.
3. Connect your Android device (Android 9 to 12+) via USB with **USB Debugging** enabled.
4. Click **Run ▶️** (`Shift + F10`) to compile and install on your device.

### 2. Mundari ONNX Model Deployment
To enable offline Mundari AI translation on your mobile device:
1. Connect your device to PC via USB.
2. Copy the exported INT8 model folder from `ml_training_and_export/android_export/onnx/int8` into your phone's storage:
   👉 **`Internal Storage` → `Download` → `onnx_int8`**  
   *(or `Android/data/com.palash.voicebridge/files/models/onnx_int8/`)*

Inside `onnx_int8/`, ensure the following 4 files are present:
- `encoder_model.onnx`
- `decoder_model.onnx`
- `decoder_with_past_model.onnx`
- `vocab.json`

---

## 🏗️ Architecture & Technical Innovations

### 1. Hybrid Dual-Tier Translation Engine
- **Tier 1 (Instant Database Match - 0ms):** Frequently used school & FLN sentences (*"आज हम गिनती सीखेंगे"*, *"पानी लाओ"*, *"नमस्ते"*) are matched against a local SQLite database for 0ms response time and 0MB memory overhead.
- **Tier 2 (Optimized ONNX Neural Engine - ~1.5s):** Custom inputs trigger an optimized M2M100 INT8 transformer engine running on 4 CPU cores (`setIntraOpNumThreads(4)`), utilizing zero-copy direct `FloatBuffer` processing to eliminate JNI memory leaks.

### 2. On-Device Speech & Audio Pipeline
- **ASR:** Offline Hindi speech recognition via Vosk / IndicConformer.
- **TTS:** Dual-script text-to-speech engine using native Android speech synthesis with Ol Chiki → Devanagari transliteration mapping.

---

## 📄 License
This project is developed for educational and humanitarian purposes under the MIT License.
