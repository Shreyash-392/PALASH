import os
import sys
import json
import pandas as pd
import torch
import sacrebleu
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, GenerationConfig
from rouge_score import rouge_scorer

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEST_FILE = os.path.join(BASE_DIR, 'data', 'processed', 'test.tsv')
SAVED_MODEL_PATH = os.path.join(BASE_DIR, 'saved_models', 'best_model')
DEFAULT_MODEL = 'facebook/m2m100_418M'
EVAL_DIR = os.path.join(BASE_DIR, 'evaluation')
os.makedirs(EVAL_DIR, exist_ok=True)

def evaluate_direction(model, tokenizer, gen_config, test_df, src_col, tgt_col, src_lang, tgt_lang, device, max_samples=500):
    refs = []
    preds = []
    sources = []
    
    subset = test_df.head(max_samples)
    model.eval()
    scorer = rouge_scorer.RougeScorer(['rouge1', 'rouge2', 'rougeL'], use_stemmer=False)
    
    print(f"\nEvaluating direction ({src_col} -> {tgt_col}) on {len(subset)} test samples...")
    tokenizer.src_lang = src_lang
    tokenizer.tgt_lang = tgt_lang
    forced_bos_id = tokenizer.lang_code_to_id[tgt_lang]
    
    with torch.no_grad():
        for idx, row in subset.iterrows():
            src_text = str(row[src_col]).strip()
            tgt_text = str(row[tgt_col]).strip()
            
            inputs = tokenizer(src_text, return_tensors='pt', padding=True, truncation=True, max_length=128).to(device)
            
            outputs = model.generate(
                **inputs,
                forced_bos_token_id=forced_bos_id,
                generation_config=gen_config
            )
            pred_text = tokenizer.decode(outputs[0], skip_special_tokens=True).strip()
            
            sources.append(src_text)
            preds.append(pred_text)
            refs.append(tgt_text)
            
    # Metrics calculation
    refs_sacre = [[r] for r in refs]
    bleu = sacrebleu.corpus_bleu(preds, refs_sacre).score
    chrf = sacrebleu.corpus_chrf(preds, refs_sacre).score
    ter = sacrebleu.corpus_ter(preds, refs_sacre).score
    
    # ROUGE
    r1, r2, rl = [], [], []
    for p, r in zip(preds, refs):
        scores = scorer.score(r, p)
        r1.append(scores['rouge1'].fmeasure)
        r2.append(scores['rouge2'].fmeasure)
        rl.append(scores['rougeL'].fmeasure)
        
    rouge1 = sum(r1) / len(r1) * 100
    rouge2 = sum(r2) / len(r2) * 100
    rougeL = sum(rl) / len(rl) * 100
    
    examples = []
    failures = []
    for s, r, p in zip(sources[:10], refs[:10], preds[:10]):
        examples.append({'source': s, 'reference': r, 'prediction': p})
        
    for s, r, p in zip(sources, refs, preds):
        if sacrebleu.sentence_bleu(p, [r]).score < 10.0 and len(failures) < 5:
            failures.append({'source': s, 'reference': r, 'prediction': p})
            
    results = {
        'direction': f"{src_col} -> {tgt_col}",
        'sample_count': len(subset),
        'sacreBLEU': round(bleu, 2),
        'chrF': round(chrf, 2),
        'TER': round(ter, 2),
        'ROUGE-1': round(rouge1, 2),
        'ROUGE-2': round(rouge2, 2),
        'ROUGE-L': round(rougeL, 2),
        'examples': examples,
        'failures': failures
    }
    
    return results

def main():
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model_path = SAVED_MODEL_PATH if (os.path.exists(SAVED_MODEL_PATH) and os.path.exists(os.path.join(SAVED_MODEL_PATH, "config.json"))) else DEFAULT_MODEL
    
    print("="*60)
    print(f"EVALUATION SUITE - MODEL: {model_path}")
    print("="*60)
    
    tokenizer = AutoTokenizer.from_pretrained(model_path, src_lang='hi', tgt_lang='hi')
    if '__unr__' in tokenizer.additional_special_tokens:
        unr_token_id = tokenizer.convert_tokens_to_ids('__unr__')
        tokenizer.lang_code_to_token['unr'] = '__unr__'
        tokenizer.lang_code_to_id['unr'] = unr_token_id
        tokenizer.lang_token_to_id['__unr__'] = unr_token_id

    model = AutoModelForSeq2SeqLM.from_pretrained(model_path).to(device)
    
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
    
    test_df = pd.read_csv(TEST_FILE, sep='\t')
    print(f"Loaded test split: {len(test_df)} pairs")
    
    hi2unr_res = evaluate_direction(
        model, tokenizer, gen_config, test_df, 
        src_col='hindi', tgt_col='mundari', 
        src_lang='hi', tgt_lang='unr',
        device=device
    )
    
    unr2hi_res = evaluate_direction(
        model, tokenizer, gen_config, test_df, 
        src_col='mundari', tgt_col='hindi', 
        src_lang='unr', tgt_lang='hi',
        device=device
    )
    
    full_report = {
        'model_path': model_path,
        'hindi_to_mundari': hi2unr_res,
        'mundari_to_hindi': unr2hi_res
    }
    
    report_json_path = os.path.join(EVAL_DIR, 'evaluation_results.json')
    with open(report_json_path, 'w', encoding='utf-8') as f:
        json.dump(full_report, f, ensure_ascii=False, indent=2)
        
    print("\n" + "="*60)
    print("EVALUATION SUMMARY RESULTS")
    print("="*60)
    print(f"1. Hindi -> Mundari:")
    print(f"   SacreBLEU: {hi2unr_res['sacreBLEU']} | chrF: {hi2unr_res['chrF']} | TER: {hi2unr_res['TER']} | ROUGE-L: {hi2unr_res['ROUGE-L']}")
    print(f"2. Mundari -> Hindi:")
    print(f"   SacreBLEU: {unr2hi_res['sacreBLEU']} | chrF: {unr2hi_res['chrF']} | TER: {unr2hi_res['TER']} | ROUGE-L: {unr2hi_res['ROUGE-L']}")
    
    print(f"\nFull report saved to {report_json_path}")

if __name__ == '__main__':
    main()
