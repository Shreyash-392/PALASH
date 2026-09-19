import sys
import torch
import pandas as pd
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, M2M100Tokenizer, M2M100ForConditionalGeneration

sys.stdout.reconfigure(encoding='utf-8')

def run_diagnostics():
    print("="*60)
    print("M2M100 DIAGNOSTIC & MUNDARI ADAPTATION CHECK")
    print("="*60)
    
    model_name = 'facebook/m2m100_418M'
    tokenizer = M2M100Tokenizer.from_pretrained(model_name)
    model = M2M100ForConditionalGeneration.from_pretrained(model_name)
    
    vocab_before = len(tokenizer)
    embed_before = model.config.vocab_size
    print(f"1. Tokenizer Vocab Size Before: {vocab_before}")
    print(f"2. Model Embedding Size Before: {embed_before}")
    
    all_lang_codes = sorted(list(tokenizer.lang_code_to_token.keys()))
    print(f"3. Total Native M2M100 Language Codes: {len(all_lang_codes)}")
    print(f"   Native Language Codes Sample: {all_lang_codes[:15]}...")
    
    mundari_exists = 'unr' in tokenizer.lang_code_to_token or 'mundari' in tokenizer.lang_code_to_token
    print(f"4. Does Native Mundari ('unr') exist in M2M100? -> {mundari_exists}")
    
    # 5. Adapt for Mundari by adding '__unr__' special token
    print("\n--- ADAPTING M2M100 FOR MUNDARI ('__unr__') ---")
    if '__unr__' not in tokenizer.additional_special_tokens:
        num_added = tokenizer.add_special_tokens({'additional_special_tokens': ['__unr__']})
        model.resize_token_embeddings(len(tokenizer))
        print(f"Added new special token '__unr__': {num_added}")
        
    unr_token_id = tokenizer.convert_tokens_to_ids('__unr__')
    tokenizer.lang_code_to_token['unr'] = '__unr__'
    tokenizer.lang_code_to_id['unr'] = unr_token_id
    tokenizer.lang_token_to_id['__unr__'] = unr_token_id
    
    vocab_after = len(tokenizer)
    embed_after = model.config.vocab_size
    print(f"6. Tokenizer Vocab Size After: {vocab_after}")
    print(f"7. Model Embedding Size After: {embed_after}")
    print(f"8. '__unr__' Token ID: {unr_token_id}")
    print(f"9. Decoder Start Token ID (Default): {model.config.decoder_start_token_id}")

    # 6. Minimal Tokenizer Test
    hi_sample = "आप कैसे हैं?"
    unr_sample = "नेआको रे ओड़ोःगे सानाडगि होबाजान रे ओड़ोःगे बुगिना।"
    
    print("\n--- TOKENIZATION TEST: Hindi -> Mundari ---")
    tokenizer.src_lang = 'hi'
    tokenizer.tgt_lang = 'unr'
    
    enc_hi = tokenizer(hi_sample, return_tensors='pt')
    enc_unr_target = tokenizer(text_target=unr_sample, return_tensors='pt')
    
    print("Hindi Source Input IDs:", enc_hi['input_ids'][0].tolist())
    print("Hindi Source Tokens:   ", tokenizer.convert_ids_to_tokens(enc_hi['input_ids'][0]))
    print("Mundari Target Input IDs:", enc_unr_target['input_ids'][0].tolist())
    print("Mundari Target Tokens:   ", tokenizer.convert_ids_to_tokens(enc_unr_target['input_ids'][0]))
    
    print("\n--- TOKENIZATION TEST: Mundari -> Hindi ---")
    tokenizer.src_lang = 'unr'
    tokenizer.tgt_lang = 'hi'
    
    enc_unr_src = tokenizer(unr_sample, return_tensors='pt')
    enc_hi_target = tokenizer(text_target=hi_sample, return_tensors='pt')
    
    print("Mundari Source Input IDs:", enc_unr_src['input_ids'][0].tolist())
    print("Mundari Source Tokens:   ", tokenizer.convert_ids_to_tokens(enc_unr_src['input_ids'][0]))
    print("Hindi Target Input IDs:", enc_hi_target['input_ids'][0].tolist())
    print("Hindi Target Tokens:   ", tokenizer.convert_ids_to_tokens(enc_hi_target['input_ids'][0]))
    
    # 7. Minimal Forward Pass Test
    print("\n--- FORWARD PASS TEST ---")
    labels = enc_hi_target['input_ids']
    outputs = model(input_ids=enc_unr_src['input_ids'], attention_mask=enc_unr_src['attention_mask'], labels=labels)
    print(f"Forward Pass Successful! Loss: {outputs.loss.item():.4f}")
    print(f"Logits Shape: {outputs.logits.shape}")

if __name__ == '__main__':
    run_diagnostics()
