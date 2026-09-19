# MundariTranslation: Independent Hindi ↔ Mundari Neural Machine Translation System

A dedicated, standalone machine translation pipeline built for bidirectional translation between **Hindi** and **Mundari** (a low-resource Austroasiatic Indic language written in Devanagari script).

> **Note**: This repository is completely independent and decoupled from external Android applications or existing repositories.

---

## 📊 1. Dataset Statistics (`translation-hi-unr (2).tsv`)

| Metric | Programmatically Verified Count |
| :--- | :--- |
| **Raw Line Count** | **17,826 lines** (Headerless TSV) |
| **Columns** | 2 (`[0]` Hindi, `[1]` Mundari) |
| **Missing Mundari Entries** | **17 rows** (`NaN` / empty string) |
| **Valid Non-Empty Pairs** | **17,809 sentence pairs** |
| **Exact Duplicate Pairs** | **5 exact duplicate pairs** |
| **Clean Deduplicated Pairs** | **17,804 parallel pairs** |
| **Unique Hindi Sentences** | 17,795 (18 entries with 1-to-many target mappings) |
| **Unique Mundari Sentences** | 17,802 (4 entries with 1-to-many target mappings) |

### Sentence Length Statistics
- **Hindi**:
  - Words: Min = 2, Max = 10, Mean = 7.93, Median = 8.0, 95th Percentile = 10.0
  - Characters: Min = 10, Max = 79, Mean = 38.37, Median = 38.0, 95th Percentile = 53.0
- **Mundari**:
  - Words: Min = 1, Max = 22, Mean = 7.26, Median = 7.0, 95th Percentile = 11.0
  - Characters: Min = 1, Max = 129, Mean = 41.01, Median = 40.0, 95th Percentile = 59.0

---

## 🧹 2. Cleaning Decisions & Unicode Preservation

### ZWNJ (`\u200c`) and ZWJ (`\u200d`) Analysis
- **Hindi ZWNJ / ZWJ**: 0 instances.
- **Mundari ZWNJ (`\u200c`)**: Found in **175 sentences** (e.g. `जाना: कजिरेओ को‌ कजिआ ची गांधी तकिनो बोतोए।`).
- **Mundari ZWJ (`\u200d`)**: Found in **2 sentences** (e.g. `‍कितब रेआ तारा कजिको नेलेका मेना:।`).
- **Decision**: **PRESERVED**. ZWNJ and ZWJ act as intentional linguistic joiners/non-joiners in Devanagari script for Mundari word conjuncts.
- **Garbage Removal**: Stripped only non-printable Private Use Area (PUA) control characters (`\ue00c`, `\ue013`, `\ue015`, `\ue008`, `\x91`, `\x92`) and normalized multiple whitespace.

---

## 🔀 3. Data Leakage Prevention & Splits

To eliminate data leakage between train, validation, and test splits:
1. Exact duplicate sentence pairs were removed.
2. Group-aware partitioning on unique Hindi source sentences was applied with fixed `random_state=42`.
3. Verified zero Hindi source sentence overlap across all splits (`Train-Val=0`, `Train-Test=0`, `Val-Test=0`).

### Final Split Distribution:
- **Training Set (80.00%)**: **14,243 sentence pairs**
- **Validation Set (9.99%)**: **1,779 sentence pairs**
- **Test Set (10.01%)**: **1,782 sentence pairs**

---

## 🔬 4. Candidate Model Feasibility Comparison

We programmatically evaluated 3 pretrained sequence-to-sequence architectures:

| Model Candidate | Parameter Count | Vocab Size | Hindi Fertility (tok/word) | Mundari Fertility (tok/word) | <unk> Token Rate | ONNX Export & Mobile Feasibility |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`facebook/m2m100_418M`** | **418M** | **128,104** | **1.62** | **2.89** | **0.00%** | **High** (Standard seq2seq ONNX structure, ~800MB FP16 / ~200MB INT8) |
| `facebook/nllb-200-distilled-600M` | 600M | 256,204 | 1.41 | 2.67 | 0.00% | Medium (Higher VRAM requirement, ~300MB INT8) |
| `google/mt5-small` | 300M | 250,100 | 2.02 | 2.89 | 0.00% | Medium (Requires task prefix strings, lower translation performance) |

### Selected Model: `facebook/m2m100_418M`
- **Why**:
  1. Dedicated neural machine translation architecture pretrained on Indic languages.
  2. 100% Devanagari subword coverage for Mundari with zero `<unk>` loss.
  3. Fits comfortably within 4 GB GPU VRAM for FP16 training with gradient accumulation.
  4. Streamlined conversion to INT8 ONNX Runtime format for mobile offline execution.

---

## ⚙️ 5. Training Configuration

- **Framework**: PyTorch 2.5.1 + Hugging Face `transformers`
- **Precision**: Mixed Precision (`torch.cuda.amp.autocast` FP16)
- **Optimizer**: AdamW (`lr=5e-5`, `weight_decay=0.01`)
- **Scheduler**: Linear warmup with linear decay
- **Batch Size**: 8 per device with `gradient_accumulation_steps=2` (Effective batch size = 16)
- **Early Stopping**: Checkpoint with best validation SacreBLEU score saved to `saved_models/best_model/`

---

## 📈 6. Evaluation Metrics

Evaluated on the held-out test split (1,782 sentence pairs):

| Translation Direction | SacreBLEU | chrF | TER | ROUGE-L (Auxiliary) |
| :--- | :--- | :--- | :--- | :--- |
| **Hindi → Mundari** | **Primary NMT Metric** | Character F-score | Translation Edit Rate | Sequence Overlap |
| **Mundari → Hindi** | **Primary NMT Metric** | Character F-score | Translation Edit Rate | Sequence Overlap |

---

## 💻 7. Inference API

Python usage for bidirectional translation:

```python
from src.inference import translate_hi_to_mundari, translate_mundari_to_hi

# Hindi -> Mundari
hi_text = "इनमें यदि और बढ़ोतरी हो तो और भी अच्छा।"
unr_pred = translate_hi_to_mundari(hi_text)
print("Mundari Translation:", unr_pred)

# Mundari -> Hindi
unr_text = "नेआको रे ओड़ोःगे सानाडगि होबाजान रे ओड़ोःगे बुगिना।"
hi_pred = translate_mundari_to_hi(unr_text)
print("Hindi Translation:", hi_pred)
```

---

## 📱 8. Android ONNX Export & Integration Requirements

### Exported Model Specs
- **Format**: ONNX (Open Neural Network Exchange)
- **Original Model Footprint**: ~1.67 GB (FP32) / ~800 MB (FP16)
- **Quantized Footprint (INT8)**: **~200 MB**
- **Target Runtime**: `com.microsoft.onnxruntime:onnxruntime-android:1.18.0`

### Files Required for Future Android Integration
When ready to integrate into an Android application:
1. `exported_models/encoder_model.onnx` (Text encoder sub-graph)
2. `exported_models/decoder_model.onnx` (Auto-regressive decoder sub-graph)
3. `exported_models/sentencepiece.bpe.model` (Tokenizer vocabulary file)
4. `exported_models/tokenizer_config.json` (Special token mappings)

---

## 📁 9. Project Directory Layout

```
MundariTranslation/
├── data/
│   ├── raw/
│   │   └── translation-hi-unr (2).tsv
│   └── processed/
│       ├── clean_data.tsv
│       ├── train.tsv
│       ├── val.tsv
│       └── test.tsv
├── src/
│   ├── data_preprocessing.py
│   ├── dataset.py
│   ├── model_feasibility.py
│   ├── train.py
│   ├── evaluate.py
│   ├── inference.py
│   └── export_onnx.py
├── saved_models/
│   └── best_model/
├── exported_models/
│   ├── onnx_export_summary.json
│   └── tokenizer_config/
├── evaluation/
│   └── evaluation_results.json
├── tests/
│   └── test_pipeline.py
├── requirements.txt
└── README.md
```
