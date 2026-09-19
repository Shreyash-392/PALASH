import os
import sys
import torch
from transformers import AutoTokenizer, AutoModelForSeq2SeqLM, GenerationConfig

sys.stdout.reconfigure(encoding='utf-8')

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SAVED_MODEL_PATH = os.path.join(BASE_DIR, 'saved_models', 'best_model')
DEFAULT_MODEL_NAME = 'facebook/m2m100_418M'

_model = None
_tokenizer = None
_device = None
_gen_config = None

def load_model(model_path=None):
    global _model, _tokenizer, _device, _gen_config
    if _model is not None:
        return
        
    _device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    
    path_to_load = model_path if (model_path and os.path.exists(model_path)) else (
        SAVED_MODEL_PATH if (os.path.exists(SAVED_MODEL_PATH) and os.path.exists(os.path.join(SAVED_MODEL_PATH, "config.json"))) else DEFAULT_MODEL_NAME
    )
    
    print(f"Loading translation model from: {path_to_load}...")
    _tokenizer = AutoTokenizer.from_pretrained(path_to_load, src_lang='hi', tgt_lang='hi')
    if '__unr__' in _tokenizer.additional_special_tokens:
        unr_token_id = _tokenizer.convert_tokens_to_ids('__unr__')
        _tokenizer.lang_code_to_token['unr'] = '__unr__'
        _tokenizer.lang_code_to_id['unr'] = unr_token_id
        _tokenizer.lang_token_to_id['__unr__'] = unr_token_id

    _model = AutoModelForSeq2SeqLM.from_pretrained(path_to_load).to(_device)
    _model.eval()
    
    # Configure GenerationConfig cleanly
    for param in ['max_length', 'num_beams', 'early_stopping']:
        if hasattr(_model.config, param):
            delattr(_model.config, param)
            
    _gen_config = GenerationConfig(
        max_length=128,
        num_beams=1,
        early_stopping=False,
        bos_token_id=_model.config.bos_token_id,
        eos_token_id=_model.config.eos_token_id,
        pad_token_id=_model.config.pad_token_id,
        decoder_start_token_id=_model.config.decoder_start_token_id
    )

def translate_hi_to_mundari(text: str, max_length: int = 128) -> str:
    """
    Translate Hindi text to Mundari.
    """
    load_model()
    _tokenizer.src_lang = 'hi'
    _tokenizer.tgt_lang = 'unr'
    
    inputs = _tokenizer(text.strip(), return_tensors='pt', padding=True, truncation=True, max_length=max_length).to(_device)
    forced_bos_id = _tokenizer.lang_code_to_id['unr']
    
    with torch.no_grad():
        outputs = _model.generate(
            **inputs,
            forced_bos_token_id=forced_bos_id,
            generation_config=_gen_config
        )
        
    translation = _tokenizer.decode(outputs[0], skip_special_tokens=True).strip()
    return translation

def translate_mundari_to_hi(text: str, max_length: int = 128) -> str:
    """
    Translate Mundari text to Hindi.
    """
    load_model()
    _tokenizer.src_lang = 'unr'
    _tokenizer.tgt_lang = 'hi'
    
    inputs = _tokenizer(text.strip(), return_tensors='pt', padding=True, truncation=True, max_length=max_length).to(_device)
    forced_bos_id = _tokenizer.lang_code_to_id['hi']
    
    with torch.no_grad():
        outputs = _model.generate(
            **inputs,
            forced_bos_token_id=forced_bos_id,
            generation_config=_gen_config
        )
        
    translation = _tokenizer.decode(outputs[0], skip_special_tokens=True).strip()
    return translation

if __name__ == '__main__':
    sample_hi = "इनमें यदि और बढ़ोतरी हो तो और भी अच्छा।"
    sample_unr = "नेआको रे ओड़ोःगे सानाडगि होबाजान रे ओड़ोःगे बुगिना।"
    
    print("Testing Hindi -> Mundari:")
    print("  HI Source:", sample_hi)
    print("  UNR Pred :", translate_hi_to_mundari(sample_hi))
    
    print("\nTesting Mundari -> Hindi:")
    print("  UNR Source:", sample_unr)
    print("  HI Pred   :", translate_mundari_to_hi(sample_unr))
