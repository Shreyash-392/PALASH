import os
import sys
import unittest
import pandas as pd

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.append(os.path.join(BASE_DIR, 'src'))

from data_preprocessing import clean_text

class TestMundariPipeline(unittest.TestCase):
    
    def test_clean_text_preserves_zwnj_zwj(self):
        sample_zwnj = "कजिरेओ को\u200c कजिआ"
        sample_pua = "hello \ue00c world \x91 test"
        
        cleaned_zwnj = clean_text(sample_zwnj)
        cleaned_pua = clean_text(sample_pua)
        
        self.assertIn('\u200c', cleaned_zwnj)
        self.assertNotIn('\ue00c', cleaned_pua)
        self.assertNotIn('\x91', cleaned_pua)
        self.assertEqual(cleaned_pua, "hello world test")

    def test_data_splits_exist(self):
        train_path = os.path.join(BASE_DIR, 'data', 'processed', 'train.tsv')
        val_path = os.path.join(BASE_DIR, 'data', 'processed', 'val.tsv')
        test_path = os.path.join(BASE_DIR, 'data', 'processed', 'test.tsv')
        
        self.assertTrue(os.path.exists(train_path), "Train TSV missing")
        self.assertTrue(os.path.exists(val_path), "Val TSV missing")
        self.assertTrue(os.path.exists(test_path), "Test TSV missing")
        
        df_tr = pd.read_csv(train_path, sep='\t')
        df_va = pd.read_csv(val_path, sep='\t')
        df_te = pd.read_csv(test_path, sep='\t')
        
        self.assertEqual(len(df_tr), 14243)
        self.assertEqual(len(df_va), 1779)
        self.assertEqual(len(df_te), 1782)

if __name__ == '__main__':
    unittest.main()
