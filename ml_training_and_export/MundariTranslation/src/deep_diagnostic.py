import os
import sys
import json
import torch
import numpy as np
import pandas as pd
from transformers import (
    M2M100Tokenizer, 
    M2M100ForConditionalGeneration,
    GenerationConfig
)

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(os.path.join(BASE_DIR, 'src'))

from dataset import BidirectionalTranslationDataset

CHECKPOINT_DIR = os.path.join(BASE_DIR, 'saved_models', 'best_model')
VAL_FILE = os.path.join(BASE_DIR, 'data', 'processed', 'val.tsv')
TRAIN_FILE = os.path.join(BASE_DIR, 'data', 'processed', 'train.tsv')

def run_deep_diagnostic():
    print("="*70)
    print("DEEP DIAGNOSTIC OF SMOKE-TEST CHECKPOINT & DATASET ENCODING")
    print("="*70)
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using Device: {device}")
    
    # 1. Inspect Saved Checkpoint Artifacts
    print("\n--- 1. CHECKPOINT ARTIFACTS & TOKENIZER CONFIG INSPECTION ---")
    if not os.path.exists(CHECKPOINT_DIR):
        print(f"ERROR: Checkpoint directory {CHECKPOINT_DIR} does not exist!")
        return
        
    checkpoint_files = os.listdir(CHECKPOINT_DIR)
    print(f"Files in {CHECKPOINT_DIR}: {checkpoint_files}")
    
    # Pass src_lang='hi' to prevent M2M100Tokenizer.__init__ from throwing KeyError on 'unr'
    tokenizer = M2M100Tokenizer.from_pretrained(CHECKPOINT_DIR, src_lang='hi', tgt_lang='hi')
    model = M2M100ForConditionalGeneration.from_pretrained(CHECKPOINT_DIR).to(device)
    
    vocab_size = len(tokenizer)
    embed_size = model.config.vocab_size
    lm_head_size = model.lm_head.out_features if hasattr(model, 'lm_head') else 'N/A'
    
    print(f"Tokenizer Vocab Size:   {vocab_size}")
    print(f"Model Config Vocab Size:{embed_size}")
    print(f"LM Head Output Features:{lm_head_size}")
    
    unr_token_in_tokenizer = '__unr__' in tokenizer.additional_special_tokens
    unr_token_id = tokenizer.convert_tokens_to_ids('__unr__') if unr_token_in_tokenizer else None
    print(f"__unr__ present in saved tokenizer: {unr_token_in_tokenizer} (Token ID: {unr_token_id})")
    
    # Ensure token mappings are registered in runtime tokenizer dicts
    if unr_token_id is not None:
        tokenizer.lang_code_to_token['unr'] = '__unr__'
        tokenizer.lang_code_to_id['unr'] = unr_token_id
        tokenizer.lang_token_to_id['__unr__'] = unr_token_id

    # 2. Generation Configuration
    print("\n--- 2. GENERATION CONFIGURATION ---")
    gen_config = GenerationConfig.from_model_config(model.config)
    gen_config.num_beams = 1
    gen_config.early_stopping = False
    print(f"decoder_start_token_id: {model.config.decoder_start_token_id}")
    print(f"bos_token_id:           {model.config.bos_token_id}")
    print(f"eos_token_id:           {model.config.eos_token_id}")
    print(f"pad_token_id:           {model.config.pad_token_id}")
    print(f"GenerationConfig num_beams: {gen_config.num_beams}, early_stopping: {gen_config.early_stopping}")

    # 3. Label & Dataset Encoding Verification
    print("\n--- 3. DATASET LABEL ENCODING VERIFICATION ---")
    train_dataset = BidirectionalTranslationDataset(TRAIN_FILE, tokenizer, max_length=128)
    
    hi2unr_sample = None
    unr2hi_sample = None
    
    for item in train_dataset.pairs:
        if item['direction'] == 'hi2unr' and hi2unr_sample is None:
            hi2unr_sample = item
        if item['direction'] == 'unr2hi' and unr2hi_sample is None:
            unr2hi_sample = item
        if hi2unr_sample and unr2hi_sample:
            break
            
    def verify_sample_encoding(sample, name):
        print(f"\nVerifying {name}:")
        print(f"  Source Text: '{sample['src_text']}'")
        print(f"  Target Text: '{sample['tgt_text']}'")
        
        tokenizer.src_lang = sample['src_lang']
        tokenizer.tgt_lang = sample['tgt_lang']
        
        src_enc = tokenizer(sample['src_text'], return_tensors='pt', max_length=128, truncation=True)
        tgt_enc = tokenizer(text_target=sample['tgt_text'], return_tensors='pt', max_length=128, truncation=True)
        
        labels = tgt_enc['input_ids'][0].clone()
        pad_id = tokenizer.pad_token_id if tokenizer.pad_token_id is not None else 0
        
        decoded_clean_labels = tokenizer.decode([t for t in labels if t != -100 and t != pad_id], skip_special_tokens=True)
        
        print(f"  Encoder Input IDs: {src_enc['input_ids'][0].tolist()[:10]}...")
        print(f"  Encoder Tokens:    {tokenizer.convert_ids_to_tokens(src_enc['input_ids'][0][:10])}...")
        print(f"  Decoder Labels:    {labels.tolist()[:10]}...")
        print(f"  Decoder Tokens:    {tokenizer.convert_ids_to_tokens(labels[:10])}...")
        print(f"  Reconstructed Target Text: '{decoded_clean_labels}'")
        
        invalid_minus_100 = any(t == -100 for t in labels[:-1])
        print(f"  Labels valid (-100 only for padding): {not invalid_minus_100}")

    verify_sample_encoding(hi2unr_sample, "Hindi -> Mundari Training Pair")
    verify_sample_encoding(unr2hi_sample, "Mundari -> Hindi Training Pair")

    # 4. Inference on 10 Held-out Validation Examples & Error Metrics
    print("\n--- 4. INFERENCE ON 10 HELD-OUT VALIDATION EXAMPLES ---")
    val_df = pd.read_csv(VAL_FILE, sep='\t').head(10)
    
    def evaluate_validation_subset(src_col, tgt_col, src_lang, tgt_lang, label_name):
        print(f"\n==================================================")
        print(f"DIRECTION: {label_name} ({src_col} -> {tgt_col})")
        print(f"==================================================")
        
        tokenizer.src_lang = src_lang
        tokenizer.tgt_lang = tgt_lang
        forced_bos_id = tokenizer.lang_code_to_id[tgt_lang]
        
        preds = []
        refs = []
        unk_counts = 0
        empty_counts = 0
        repetition_counts = 0
        gen_lengths = []
        ref_lengths = []
        
        for idx, row in val_df.iterrows():
            s_text = str(row[src_col]).strip()
            r_text = str(row[tgt_col]).strip()
            
            inputs = tokenizer(s_text, return_tensors='pt', max_length=128, truncation=True).to(device)
            
            with torch.no_grad():
                out = model.generate(
                    **inputs,
                    forced_bos_token_id=forced_bos_id,
                    generation_config=gen_config,
                    max_length=64
                )
                
            pred_text = tokenizer.decode(out[0], skip_special_tokens=True).strip()
            
            preds.append(pred_text)
            refs.append(r_text)
            
            print(f"Example {idx+1}:")
            print(f"  SOURCE ({src_col}): {s_text}")
            print(f"  REFERENCE ({tgt_col}): {r_text}")
            print(f"  PREDICTION: {pred_text}\n")
            
            gen_tokens = tokenizer.tokenize(pred_text)
            ref_tokens = tokenizer.tokenize(r_text)
            
            gen_lengths.append(len(gen_tokens))
            ref_lengths.append(len(ref_tokens))
            
            if tokenizer.unk_token in gen_tokens:
                unk_counts += 1
            if not pred_text:
                empty_counts += 1
            if len(gen_tokens) > 3 and len(set(gen_tokens)) <= 2:
                repetition_counts += 1
                
        num_samples = len(val_df)
        print(f"--- STATISTICAL METRICS ({label_name}) ---")
        print(f"  % Predictions containing <unk>: {unk_counts / num_samples * 100:.1f}%")
        print(f"  % Predictions empty:            {empty_counts / num_samples * 100:.1f}%")
        print(f"  % Predictions repeated tokens:  {repetition_counts / num_samples * 100:.1f}%")
        print(f"  Avg Generated Token Length:     {np.mean(gen_lengths):.2f} tokens")
        print(f"  Avg Reference Token Length:     {np.mean(ref_lengths):.2f} tokens")

    evaluate_validation_subset('hindi', 'mundari', 'hi', 'unr', 'Hindi -> Mundari')
    evaluate_validation_subset('mundari', 'hindi', 'unr', 'hi', 'Mundari -> Hindi')

if __name__ == '__main__':
    run_deep_diagnostic()
