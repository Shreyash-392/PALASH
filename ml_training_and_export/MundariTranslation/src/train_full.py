import os
import sys
import time
import json
import argparse
import numpy as np
import pandas as pd
import torch
from torch.utils.data import DataLoader
from transformers import (
    M2M100Tokenizer, 
    M2M100ForConditionalGeneration, 
    get_linear_schedule_with_warmup,
    GenerationConfig
)
import sacrebleu

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(os.path.join(BASE_DIR, 'src'))

from dataset import BidirectionalTranslationDataset

INITIAL_CHECKPOINT = os.path.join(BASE_DIR, 'saved_models', 'best_model')
FULL_SAVE_DIR = os.path.join(BASE_DIR, 'saved_models', 'full_training', 'best_model')
os.makedirs(FULL_SAVE_DIR, exist_ok=True)

def setup_full_training_model(model_name='facebook/m2m100_418M'):
    checkpoint_to_load = INITIAL_CHECKPOINT if (os.path.exists(INITIAL_CHECKPOINT) and os.path.exists(os.path.join(INITIAL_CHECKPOINT, 'config.json'))) else model_name
    
    print(f"Loading Base Checkpoint: {checkpoint_to_load}...", flush=True)
    tokenizer = M2M100Tokenizer.from_pretrained(checkpoint_to_load, src_lang='hi', tgt_lang='hi')
    
    if '__unr__' not in tokenizer.additional_special_tokens:
        tokenizer.add_special_tokens({'additional_special_tokens': ['__unr__']})
        
    model = M2M100ForConditionalGeneration.from_pretrained(checkpoint_to_load)
    
    if model.config.vocab_size != len(tokenizer):
        model.resize_token_embeddings(len(tokenizer))
        
    unr_token_id = tokenizer.convert_tokens_to_ids('__unr__')
    tokenizer.lang_code_to_token['unr'] = '__unr__'
    tokenizer.lang_code_to_id['unr'] = unr_token_id
    tokenizer.lang_token_to_id['__unr__'] = unr_token_id
    
    for param in ['max_length', 'num_beams', 'early_stopping']:
        if hasattr(model.config, param):
            delattr(model.config, param)
            
    gen_config = GenerationConfig(
        max_length=128,
        num_beams=1,
        early_stopping=False,
        bos_token_id=model.config.bos_token_id,
        eos_token_id=model.config.eos_token_id,
        pad_token_id=model.config.pad_token_id,
        decoder_start_token_id=model.config.decoder_start_token_id
    )
    model.generation_config = gen_config
    
    return model, tokenizer, gen_config

def evaluate_validation(model, tokenizer, gen_config, val_loader, device, num_samples=100):
    model.eval()
    refs = []
    preds = []
    
    count = 0
    with torch.no_grad():
        for batch in val_loader:
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            raw_tgts = batch['raw_tgt']
            tgt_langs = batch['tgt_lang']
            
            tgt_lang_code = tgt_langs[0]
            forced_bos_id = tokenizer.lang_code_to_id.get(tgt_lang_code, tokenizer.lang_code_to_id['hi'])
            
            outputs = model.generate(
                input_ids=input_ids,
                attention_mask=attention_mask,
                forced_bos_token_id=forced_bos_id,
                generation_config=gen_config
            )
            
            decoded = tokenizer.batch_decode(outputs, skip_special_tokens=True)
            
            for p, r in zip(decoded, raw_tgts):
                p_clean = p.replace('__hi__', '').replace('__unr__', '').strip()
                preds.append(p_clean)
                refs.append([r.strip()])
                count += 1
                if count >= num_samples:
                    break
            if count >= num_samples:
                break
                
    if device.type == 'cuda':
        torch.cuda.empty_cache()
        
    bleu = sacrebleu.corpus_bleu(preds, refs).score
    chrf = sacrebleu.corpus_chrf(preds, refs).score
    return bleu, chrf, preds[:3], [r[0] for r in refs[:3]]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--epochs', type=int, default=3)
    parser.add_argument('--batch_size', type=int, default=4)
    parser.add_argument('--grad_accum', type=int, default=4)
    parser.add_argument('--lr', type=float, default=5e-5)
    args = parser.parse_args()
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print("="*70, flush=True)
    print("PHASE 1: FULL DATASET MULTI-EPOCH FINE-TUNING RUN", flush=True)
    print(f"Device: {device} | Epochs: {args.epochs} | Batch Size: {args.batch_size} | Grad Accum: {args.grad_accum}", flush=True)
    print("="*70, flush=True)
    
    train_file = os.path.join(BASE_DIR, 'data', 'processed', 'train.tsv')
    val_file = os.path.join(BASE_DIR, 'data', 'processed', 'val.tsv')
    
    model, tokenizer, gen_config = setup_full_training_model()
    model = model.to(device)
    
    print(f"Tokenizer Vocab: {len(tokenizer)} | Model Embeddings: {model.config.vocab_size}", flush=True)

    train_dataset = BidirectionalTranslationDataset(train_file, tokenizer, max_length=128)
    val_dataset = BidirectionalTranslationDataset(val_file, tokenizer, max_length=128)
    
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=args.batch_size, shuffle=False)
    
    print(f"Full Train Bidirectional Pairs: {len(train_dataset)}", flush=True)
    print(f"Full Val Bidirectional Pairs:   {len(val_dataset)}", flush=True)
    
    print("\n--- BASELINE EVALUATION BEFORE TRAINING ---", flush=True)
    base_bleu, base_chrf, sample_preds, sample_refs = evaluate_validation(model, tokenizer, gen_config, val_loader, device, num_samples=100)
    print(f"Baseline Val BLEU: {base_bleu:.2f} | Baseline Val chrF: {base_chrf:.2f}", flush=True)
    for p, r in zip(sample_preds, sample_refs):
        print(f"  REF:  {r}", flush=True)
        print(f"  PRED: {p}\n", flush=True)

    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    total_steps = (len(train_loader) // args.grad_accum) * args.epochs
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=int(0.1*total_steps), num_training_steps=max(1, total_steps))
    scaler = torch.amp.GradScaler('cuda', enabled=(device.type == 'cuda'))
    
    best_val_bleu = base_bleu
    epoch_results = []
    start_time = time.time()
    
    print("\n--- STARTING FULL TRAINING LOOP ---", flush=True)
    for epoch in range(args.epochs):
        model.train()
        total_train_loss = 0.0
        optimizer.zero_grad()
        
        for step, batch in enumerate(train_loader):
            input_ids = batch['input_ids'].to(device)
            attention_mask = batch['attention_mask'].to(device)
            labels = batch['labels'].to(device)
            
            with torch.amp.autocast('cuda', enabled=(device.type == 'cuda')):
                outputs = model(
                    input_ids=input_ids,
                    attention_mask=attention_mask,
                    labels=labels
                )
                loss = outputs.loss / args.grad_accum
                
            scaler.scale(loss).backward()
            total_train_loss += loss.item() * args.grad_accum
            
            if (step + 1) % args.grad_accum == 0 or (step + 1) == len(train_loader):
                scaler.step(optimizer)
                scaler.update()
                optimizer.zero_grad()
                scheduler.step()
                
            if (step + 1) % 100 == 0 or (step + 1) == len(train_loader):
                current_lr = scheduler.get_last_lr()[0]
                print(f"Epoch {epoch+1}/{args.epochs} | Step {step+1}/{len(train_loader)} | Train Loss: {loss.item()*args.grad_accum:.4f} | LR: {current_lr:.2e}", flush=True)
                
        avg_train_loss = total_train_loss / len(train_loader)
        
        # Validation Evaluation
        val_bleu, val_chrf, val_preds, val_refs = evaluate_validation(model, tokenizer, gen_config, val_loader, device, num_samples=300)
        print(f"\n---> Epoch {epoch+1} Completed | Train Loss: {avg_train_loss:.4f} | Val BLEU: {val_bleu:.2f} | Val chrF: {val_chrf:.2f}", flush=True)
        
        epoch_results.append({
            'epoch': epoch + 1,
            'avg_train_loss': round(avg_train_loss, 4),
            'val_bleu': round(val_bleu, 2),
            'val_chrf': round(val_chrf, 2)
        })
        
        # Save best checkpoint
        if val_bleu > best_val_bleu or epoch == (args.epochs - 1):
            best_val_bleu = val_bleu
            print(f"Saving BEST validation model checkpoint to {FULL_SAVE_DIR}...", flush=True)
            model.save_pretrained(FULL_SAVE_DIR)
            tokenizer.save_pretrained(FULL_SAVE_DIR)
            gen_config.save_pretrained(FULL_SAVE_DIR)
            
    total_time_min = (time.time() - start_time) / 60.0
    print(f"\nFull Training Completed in {total_time_min:.2f} minutes! Best Val BLEU: {best_val_bleu:.2f}", flush=True)
    
    history_file = os.path.join(BASE_DIR, 'saved_models', 'full_training', 'training_history.json')
    with open(history_file, 'w', encoding='utf-8') as f:
        json.dump({
            'training_duration_minutes': round(total_time_min, 2),
            'epochs': epoch_results,
            'best_val_bleu': round(best_val_bleu, 2)
        }, f, indent=2)

if __name__ == '__main__':
    main()
