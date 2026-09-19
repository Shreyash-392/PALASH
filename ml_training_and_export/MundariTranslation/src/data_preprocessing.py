import os
import sys
import unicodedata
import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RAW_DATA_PATH = os.path.join(BASE_DIR, 'data', 'raw', 'translation-hi-unr (2).tsv')
PROCESSED_DIR = os.path.join(BASE_DIR, 'data', 'processed')
os.makedirs(PROCESSED_DIR, exist_ok=True)

def inspect_zwnj_zwj(df):
    print("\n" + "="*50)
    print("STEP 2: ZWNJ (\\u200c) AND ZWJ (\\u200d) ANALYSIS")
    print("="*50)
    
    zwnj = '\u200c'
    zwj = '\u200d'
    
    hi_zwnj = df['hindi'].astype(str).str.contains(zwnj).sum()
    hi_zwj = df['hindi'].astype(str).str.contains(zwj).sum()
    unr_zwnj = df['mundari'].astype(str).str.contains(zwnj).sum()
    unr_zwj = df['mundari'].astype(str).str.contains(zwj).sum()
    
    print(f"Hindi ZWNJ (\\u200c) count: {hi_zwnj} sentences")
    print(f"Hindi ZWJ  (\\u200d) count: {hi_zwj} sentences")
    print(f"Mundari ZWNJ (\\u200c) count: {unr_zwnj} sentences")
    print(f"Mundari ZWJ  (\\u200d) count: {unr_zwj} sentences")
    
    print("\nExample sentences with ZWNJ (\\u200c) in Mundari:")
    for idx, row in df[df['mundari'].astype(str).str.contains(zwnj)].head(3).iterrows():
        print(f"  Row {idx}: HI='{row['hindi']}' | UNR='{row['mundari']}'")
        
    print("\nExample sentences with ZWJ (\\u200d) in Mundari:")
    for idx, row in df[df['mundari'].astype(str).str.contains(zwj)].head(3).iterrows():
        print(f"  Row {idx}: HI='{row['hindi']}' | UNR='{row['mundari']}'")

def clean_text(text):
    if not isinstance(text, str):
        return ""
    
    # Characters to remove: control / PUA characters
    # \ue00c, \ue013, \ue015, \ue008, \x91, \x92, and non-printable C-category chars except space/tabs/zwnj/zwj
    cleaned = []
    for char in text:
        code = ord(char)
        cat = unicodedata.category(char)
        
        # PUA characters & specific Windows-1252 smart quote garbage
        if 0xE000 <= code <= 0xF8FF or code in [0x91, 0x92, 0x93, 0x94, 0x08]:
            continue
        # Control characters except normal whitespace or ZWNJ/ZWJ
        if cat.startswith('C') and char not in ['\u200c', '\u200d', '\t', '\n']:
            continue
        cleaned.append(char)
        
    res = "".join(cleaned).strip()
    # Normalize multiple whitespace
    res = " ".join(res.split())
    return res

def main():
    print("="*50)
    print("STEP 1: RAW DATASET COUNT VERIFICATION")
    print("="*50)
    
    with open(RAW_DATA_PATH, 'r', encoding='utf-8', errors='ignore') as f:
        raw_lines = f.readlines()
    print(f"1. Raw lines in TSV file: {len(raw_lines)}")
    
    df_raw = pd.read_csv(RAW_DATA_PATH, sep="\t", header=None, quoting=3, engine='python')
    df_raw.columns = ['hindi', 'mundari']
    print(f"2. Raw dataframe shape: {df_raw.shape}")
    
    missing_hi = df_raw['hindi'].isna().sum()
    missing_unr = df_raw['mundari'].isna().sum()
    print(f"3. Missing Hindi: {missing_hi}")
    print(f"4. Missing Mundari: {missing_unr}")
    
    df_valid = df_raw.dropna(subset=['hindi', 'mundari']).copy()
    df_valid['hindi'] = df_valid['hindi'].astype(str).str.strip()
    df_valid['mundari'] = df_valid['mundari'].astype(str).str.strip()
    df_valid = df_valid[(df_valid['hindi'] != "") & (df_valid['mundari'] != "")]
    print(f"5. Valid non-empty pairs: {len(df_valid)}")
    
    dup_exact_raw = df_valid.duplicated(subset=['hindi', 'mundari']).sum()
    print(f"6. Exact duplicate pairs in raw valid: {dup_exact_raw}")
    
    # Step 2: ZWNJ/ZWJ Analysis and Data Cleaning
    inspect_zwnj_zwj(df_valid)
    
    print("\n" + "="*50)
    print("STEP 2 (Cont.): DATA CLEANING")
    print("="*50)
    
    df_clean = df_valid.copy()
    df_clean['hindi'] = df_clean['hindi'].apply(clean_text)
    df_clean['mundari'] = df_clean['mundari'].apply(clean_text)
    df_clean = df_clean[(df_clean['hindi'] != "") & (df_clean['mundari'] != "")]
    
    # Remove exact duplicate pairs after text cleaning
    exact_dups_after_clean = df_clean.duplicated(subset=['hindi', 'mundari']).sum()
    print(f"Exact duplicate pairs after text cleaning: {exact_dups_after_clean}")
    
    df_dedup = df_clean.drop_duplicates(subset=['hindi', 'mundari']).reset_index(drop=True)
    print(f"Cleaned deduplicated pairs count: {len(df_dedup)} (Expected ~17,804)")
    
    # Save clean dataset
    clean_tsv_path = os.path.join(PROCESSED_DIR, 'clean_data.tsv')
    df_dedup.to_csv(clean_tsv_path, sep='\t', index=False)
    print(f"Saved clean dataset to {clean_tsv_path}")
    
    print("\n" + "="*50)
    print("STEP 3 & 4: DATA LEAKAGE PREVENTION & SPLITTING")
    print("="*50)
    
    # Duplicate analysis on cleaned dataset
    dup_hi_sources = df_dedup.duplicated(subset=['hindi'], keep=False).sum()
    dup_unr_targets = df_dedup.duplicated(subset=['mundari'], keep=False).sum()
    print(f"Rows with Hindi source appearing multiple times: {dup_hi_sources}")
    print(f"Rows with Mundari target appearing multiple times: {dup_unr_targets}")
    
    # Split strategy: 80% train, 10% val, 10% test with random_state=42
    # To prevent leakage of exact identical source/target pairs across splits,
    # we use group-aware or stratified random split.
    # Grouping by 'hindi' source sentence ensures that if a Hindi sentence has 2 alternative Mundari translations,
    # all instances stay in the SAME split.
    
    # We assign split by unique Hindi source sentences to guarantee NO source overlap between splits!
    unique_hindi = df_dedup['hindi'].drop_duplicates().values
    
    train_hi, temp_hi = train_test_split(unique_hindi, test_size=0.20, random_state=42)
    val_hi, test_hi = train_test_split(temp_hi, test_size=0.50, random_state=42)
    
    set_train_hi = set(train_hi)
    set_val_hi = set(val_hi)
    set_test_hi = set(test_hi)
    
    df_train = df_dedup[df_dedup['hindi'].isin(set_train_hi)].reset_index(drop=True)
    df_val = df_dedup[df_dedup['hindi'].isin(set_val_hi)].reset_index(drop=True)
    df_test = df_dedup[df_dedup['hindi'].isin(set_test_hi)].reset_index(drop=True)
    
    total_len = len(df_dedup)
    print(f"Final Train Split count: {len(df_train)} ({len(df_train)/total_len*100:.2f}%)")
    print(f"Final Val Split count:   {len(df_val)} ({len(df_val)/total_len*100:.2f}%)")
    print(f"Final Test Split count:  {len(df_test)} ({len(df_test)/total_len*100:.2f}%)")
    
    # Check overlaps
    hi_overlap_tr_val = len(set(df_train['hindi']).intersection(set(df_val['hindi'])))
    hi_overlap_tr_te  = len(set(df_train['hindi']).intersection(set(df_test['hindi'])))
    hi_overlap_val_te = len(set(df_val['hindi']).intersection(set(df_test['hindi'])))
    
    unr_overlap_tr_val = len(set(df_train['mundari']).intersection(set(df_val['mundari'])))
    unr_overlap_tr_te  = len(set(df_train['mundari']).intersection(set(df_test['mundari'])))
    unr_overlap_val_te = len(set(df_val['mundari']).intersection(set(df_test['mundari'])))
    
    print("\n--- DATA LEAKAGE VERIFICATION ---")
    print(f"Hindi source sentence overlaps: Train-Val={hi_overlap_tr_val}, Train-Test={hi_overlap_tr_te}, Val-Test={hi_overlap_val_te}")
    print(f"Mundari target sentence overlaps: Train-Val={unr_overlap_tr_val}, Train-Test={unr_overlap_tr_te}, Val-Test={unr_overlap_val_te}")
    
    # Save splits
    train_path = os.path.join(PROCESSED_DIR, 'train.tsv')
    val_path = os.path.join(PROCESSED_DIR, 'val.tsv')
    test_path = os.path.join(PROCESSED_DIR, 'test.tsv')
    
    df_train.to_csv(train_path, sep='\t', index=False)
    df_val.to_csv(val_path, sep='\t', index=False)
    df_test.to_csv(test_path, sep='\t', index=False)
    
    print(f"\nSaved splits to:")
    print(f"  Train: {train_path}")
    print(f"  Val:   {val_path}")
    print(f"  Test:  {test_path}")

if __name__ == '__main__':
    main()
