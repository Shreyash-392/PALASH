import os
import sys
import time
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

def setup_model_and_tokenizer(model_name='facebook/m2m100_418M'):
    tokenizer = M2M100Tokenizer.from_pretrained(model_name)
    model = M2M100ForConditionalGeneration.from_pretrained(model_name)
    
    # Register custom '__unr__' language token for Mundari
    if '__unr__' not in tokenizer.additional_special_tokens:
        tokenizer.add_special_tokens({'additional_special_tokens': ['__unr__']})
        model.resize_token_embeddings(len(tokenizer))
        
    unr_token_id = tokenizer.convert_tokens_to_ids('__unr__')
    tokenizer.lang_code_to_token['unr'] = '__unr__'
    tokenizer.lang_code_to_id['unr'] = unr_token_id
    tokenizer.lang_token_to_id['__unr__'] = unr_token_id
    
    # Clean model.config to remove generation parameters and prevent warnings
    for param in ['max_length', 'num_beams', 'early_stopping']:
        if hasattr(model.config, param):
            delattr(model.config, param)
            
    # Set proper GenerationConfig
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

def parse_args():
    parser = argparse.ArgumentParser(description="Fine-tune Bidirectional Hindi <-> Mundari Translation Model")
    parser.add_argument('--model_name', type=str, default='facebook/m2m100_418M', help='Pretrained model checkpoint')
    parser.add_argument('--epochs', type=int, default=5, help='Number of training epochs')
    parser.add_argument('--batch_size', type=int, default=4, help='Batch size per device')
    parser.add_argument('--lr', type=float, default=5e-5, help='Learning rate')
    parser.add_argument('--grad_accum', type=int, default=4, help='Gradient accumulation steps')
    parser.add_argument('--max_len', type=int, default=128, help='Maximum sequence length')
    parser.add_argument('--sample_pairs', type=int, default=0, help='Subsample N training pairs (0 = full dataset)')
    parser.add_argument('--smoke_test', action='store_true', help='Run small smoke test (100 pairs) to verify pipeline')
    parser.add_argument('--eval_baseline', action='store_true', help='Evaluate baseline model before training')
    return parser.parse_args()

def compute_bleu(model, tokenizer, gen_config, dataloader, device, num_samples=100):
    model.eval()
    refs = []
    preds = []
    
    count = 0
    with torch.no_grad():
        for batch in dataloader:
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
            
            decoded_preds = tokenizer.batch_decode(outputs, skip_special_tokens=True)
            
            for p, r in zip(decoded_preds, raw_tgts):
                preds.append(p.strip())
                refs.append([r.strip()])
                count += 1
                if count >= num_samples:
                    break
            if count >= num_samples:
                break
                
    if device.type == 'cuda':
        torch.cuda.empty_cache()
        
    bleu = sacrebleu.corpus_bleu(preds, refs)
    return bleu.score, preds[:3], [r[0] for r in refs[:3]]

def main():
    args = parse_args()
    print("="*60, flush=True)
    print(f"TRAINING PIPELINE - MODEL: {args.model_name}", flush=True)
    print(f"Sample Pairs: {args.sample_pairs} | Epochs: {args.epochs} | Baseline Eval: {args.eval_baseline}", flush=True)
    print("="*60, flush=True)
    
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    print(f"Using device: {device}", flush=True)
    
    train_file = os.path.join(BASE_DIR, 'data', 'processed', 'train.tsv')
    val_file = os.path.join(BASE_DIR, 'data', 'processed', 'val.tsv')
    save_dir = os.path.join(BASE_DIR, 'saved_models', 'best_model')
    os.makedirs(save_dir, exist_ok=True)
    
    print(f"Loading & Adapting Tokenizer & Model: {args.model_name}...", flush=True)
    model, tokenizer, gen_config = setup_model_and_tokenizer(args.model_name)
    model = model.to(device)
    
    print(f"Tokenizer Vocab Size: {len(tokenizer)} | Model Embedding Size: {model.config.vocab_size}", flush=True)

    train_dataset = BidirectionalTranslationDataset(train_file, tokenizer, max_length=args.max_len)
    val_dataset = BidirectionalTranslationDataset(val_file, tokenizer, max_length=args.max_len)
    
    if args.smoke_test:
        args.sample_pairs = 100
        args.epochs = 1
        
    if args.sample_pairs > 0:
        print(f"\n[CONTROLLED EXPERIMENT] Subsampling {args.sample_pairs} training pairs & 300 validation pairs...", flush=True)
        train_dataset.pairs = train_dataset.pairs[:args.sample_pairs]
        val_dataset.pairs = val_dataset.pairs[:min(300, len(val_dataset.pairs))]
        
    train_loader = DataLoader(train_dataset, batch_size=args.batch_size, shuffle=True)
    val_loader = DataLoader(val_dataset, batch_size=args.batch_size, shuffle=False)
    
    print(f"Train Dataset size (bidirectional pairs): {len(train_dataset)}", flush=True)
    print(f"Val Dataset size (bidirectional pairs):   {len(val_dataset)}", flush=True)
    
    if args.eval_baseline:
        print("\n--- EVALUATING UN-FINETUNED BASELINE MODEL ---", flush=True)
        base_bleu, sample_preds, sample_refs = compute_bleu(model, tokenizer, gen_config, val_loader, device, num_samples=50)
        print(f"Baseline Validation BLEU Score: {base_bleu:.2f}", flush=True)
        for p, r in zip(sample_preds, sample_refs):
            print(f"  REF:  {r}", flush=True)
            print(f"  PRED: {p}\n", flush=True)
            
    optimizer = torch.optim.AdamW(model.parameters(), lr=args.lr, weight_decay=0.01)
    total_steps = (len(train_loader) // args.grad_accum) * args.epochs
    scheduler = get_linear_schedule_with_warmup(optimizer, num_warmup_steps=int(0.1*max(1, total_steps)), num_training_steps=max(1, total_steps))
    
    scaler = torch.amp.GradScaler('cuda', enabled=(device.type == 'cuda'))
    
    best_val_bleu = -1.0
    
    print("\n--- STARTING CONTROLLED TRAINING LOOP ---", flush=True)
    start_time = time.time()
    
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
                
            if (step + 1) % 50 == 0 or (step + 1) == len(train_loader):
                print(f"Epoch {epoch+1}/{args.epochs} | Step {step+1}/{len(train_loader)} | Train Loss: {loss.item()*args.grad_accum:.4f}", flush=True)
                
        avg_train_loss = total_train_loss / len(train_loader)
        
        # Validation
        val_bleu, val_preds, val_refs = compute_bleu(model, tokenizer, gen_config, val_loader, device, num_samples=100)
        print(f"\n---> Epoch {epoch+1} Completed | Avg Train Loss: {avg_train_loss:.4f} | Val BLEU: {val_bleu:.2f}", flush=True)
        
        # Save best checkpoint
        if val_bleu > best_val_bleu or epoch == (args.epochs - 1):
            best_val_bleu = val_bleu
            print(f"Saving checkpoint to {save_dir}...", flush=True)
            model.save_pretrained(save_dir)
            tokenizer.save_pretrained(save_dir)
            gen_config.save_pretrained(save_dir)
            
    elapsed = time.time() - start_time
    print(f"\nControlled Experiment Training Finished in {elapsed/60:.2f} minutes! Best Val BLEU: {best_val_bleu:.2f}", flush=True)

if __name__ == '__main__':
    main()
