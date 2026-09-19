import os
import sys
import pandas as pd
import torch
from transformers import AutoTokenizer, AutoConfig

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CLEAN_DATA_PATH = os.path.join(BASE_DIR, 'data', 'processed', 'clean_data.tsv')

df = pd.read_csv(CLEAN_DATA_PATH, sep='\t')
sample_hi = df['hindi'].head(100).tolist()
sample_unr = df['mundari'].head(100).tolist()

models_to_test = [
    ("facebook/m2m100_418M", "M2M-100 (418M)"),
    ("facebook/nllb-200-distilled-600M", "NLLB-200 Distilled (600M)"),
    ("google/mt5-small", "mT5-Small (300M)")
]

def analyze_model(model_name, label):
    print(f"\n" + "="*60)
    print(f"MODEL FEASIBILITY ANALYSIS: {label} ({model_name})")
    print("="*60)
    
    try:
        tokenizer = AutoTokenizer.from_pretrained(model_name)
    except Exception as e:
        print(f"Error loading tokenizer for {model_name}: {e}")
        return

    # Vocab size
    vocab_size = len(tokenizer)
    print(f"Vocabulary Size: {vocab_size:,}")
    
    # Check special tokens and lang codes
    additional_tokens = getattr(tokenizer, 'additional_special_tokens', [])
    lang_dict = getattr(tokenizer, 'lang_code_to_id', {})
    
    has_hi = 'hi' in lang_dict or 'hin_Deva' in lang_dict or '__hi__' in additional_tokens
    has_unr = 'unr' in lang_dict or 'unr_Deva' in lang_dict or 'mun' in lang_dict or 'san' in lang_dict
    
    print(f"Explicit Language Token Support: Hindi={'hi' if 'hi' in lang_dict else ('hin_Deva' if 'hin_Deva' in lang_dict else False)}, Mundari={has_unr}")
    
    # Tokenization fertility testing on sample sentences
    def compute_fertility(texts):
        total_words = 0
        total_tokens = 0
        unk_tokens = 0
        
        for t in texts:
            words = t.split()
            total_words += len(words)
            tokens = tokenizer.tokenize(t)
            total_tokens += len(tokens)
            if hasattr(tokenizer, 'unk_token') and tokenizer.unk_token in tokens:
                unk_tokens += tokens.count(tokenizer.unk_token)
            
        fertility = total_tokens / max(1, total_words)
        unk_rate = unk_tokens / max(1, total_tokens)
        return fertility, unk_rate, total_tokens, total_words
    
    hi_fert, hi_unk, hi_toks, hi_words = compute_fertility(sample_hi)
    unr_fert, unr_unk, unr_toks, unr_words = compute_fertility(sample_unr)
    
    print(f"Hindi Fertility (tokens/word): {hi_fert:.2f} (Total Words: {hi_words}, Total Tokens: {hi_toks}, <unk> count: {hi_unk*hi_toks:.0f})")
    print(f"Mundari Fertility (tokens/word): {unr_fert:.2f} (Total Words: {unr_words}, Total Tokens: {unr_toks}, <unk> count: {unr_unk*unr_toks:.0f})")
    
    # Sample tokenization representation
    print("\nTokenization Example (Sentence 1):")
    print("  HI Original :", sample_hi[0])
    print("  HI Tokens   :", tokenizer.tokenize(sample_hi[0]))
    print("  UNR Original:", sample_unr[0])
    print("  UNR Tokens  :", tokenizer.tokenize(sample_unr[0]))
    
    # Config parameters
    try:
        config = AutoConfig.from_pretrained(model_name)
        d_model = getattr(config, 'd_model', getattr(config, 'hidden_size', 'N/A'))
        num_layers = getattr(config, 'num_hidden_layers', getattr(config, 'encoder_layers', 'N/A'))
        print(f"Architecture Config: Hidden Dim={d_model}, Layers={num_layers}")
    except Exception as e:
        print(f"Config error: {e}")

if __name__ == '__main__':
    for m, lbl in models_to_test:
        analyze_model(m, lbl)
