import torch
from torch.utils.data import Dataset
import pandas as pd

class BidirectionalTranslationDataset(Dataset):
    """
    Dataset that constructs bidirectional translation pairs for Hindi <-> Mundari.
    Uses custom registered special token '__unr__' for Mundari language code 'unr'.
    
    For Hindi -> Mundari:
        src_lang = 'hi', tgt_lang = 'unr'
        Source: [__hi__, hi_tokens..., </s>]
        Target: [__unr__, unr_tokens..., </s>]
    For Mundari -> Hindi:
        src_lang = 'unr', tgt_lang = 'hi'
        Source: [__unr__, unr_tokens..., </s>]
        Target: [__hi__, hi_tokens..., </s>]
    """
    def __init__(self, tsv_file, tokenizer, max_length=128, mode='bidirectional'):
        self.df = pd.read_csv(tsv_file, sep='\t')
        self.tokenizer = tokenizer
        self.max_length = max_length
        self.mode = mode
        
        self.pairs = []
        for _, row in self.df.iterrows():
            hi_text = str(row['hindi']).strip()
            unr_text = str(row['mundari']).strip()
            
            if not hi_text or not unr_text:
                continue
                
            if mode in ['bidirectional', 'hi2unr']:
                # Hindi -> Mundari
                self.pairs.append({
                    'src_text': hi_text,
                    'tgt_text': unr_text,
                    'src_lang': 'hi',
                    'tgt_lang': 'unr',
                    'direction': 'hi2unr'
                })
            if mode in ['bidirectional', 'unr2hi']:
                # Mundari -> Hindi
                self.pairs.append({
                    'src_text': unr_text,
                    'tgt_text': hi_text,
                    'src_lang': 'unr',
                    'tgt_lang': 'hi',
                    'direction': 'unr2hi'
                })

    def __len__(self):
        return len(self.pairs)

    def __getitem__(self, idx):
        item = self.pairs[idx]
        
        # Set tokenizer language codes for this direction
        self.tokenizer.src_lang = item['src_lang']
        self.tokenizer.tgt_lang = item['tgt_lang']
        
        # Tokenize source
        src_encoding = self.tokenizer(
            item['src_text'],
            max_length=self.max_length,
            padding='max_length',
            truncation=True,
            return_tensors='pt'
        )
        
        # Tokenize target
        tgt_encoding = self.tokenizer(
            text_target=item['tgt_text'],
            max_length=self.max_length,
            padding='max_length',
            truncation=True,
            return_tensors='pt'
        )

        labels = tgt_encoding['input_ids'].squeeze(0)
        # Replace padding token id with -100 so cross-entropy loss ignores it
        pad_token_id = self.tokenizer.pad_token_id if self.tokenizer.pad_token_id is not None else 0
        labels[labels == pad_token_id] = -100

        return {
            'input_ids': src_encoding['input_ids'].squeeze(0),
            'attention_mask': src_encoding['attention_mask'].squeeze(0),
            'labels': labels,
            'direction': item['direction'],
            'tgt_lang': item['tgt_lang'],
            'raw_src': item['src_text'],
            'raw_tgt': item['tgt_text']
        }
