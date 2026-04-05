from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
from pathlib import Path
import re
import joblib
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification

app = FastAPI(title="Finance AI Service")

# =========================
# PATH
# =========================
BASE_DIR = Path(__file__).resolve().parent
MODELS_DIR = BASE_DIR / "models"

TYPE_MODEL_DIR = MODELS_DIR / "phobert_type_model"
CATEGORY_MODEL_DIR = MODELS_DIR / "phobert_category_model"
TYPE_ENCODER_PATH = MODELS_DIR / "type_encoder.pkl"
CATEGORY_ENCODER_PATH = MODELS_DIR / "category_encoder.pkl"
METADATA_PATH = MODELS_DIR / "finance_nlu_metadata.json"

# =========================
# CHECK FILE / FOLDER
# =========================
required_paths = [
    TYPE_MODEL_DIR,
    CATEGORY_MODEL_DIR,
    TYPE_ENCODER_PATH,
    CATEGORY_ENCODER_PATH,
]

missing_paths = [str(p) for p in required_paths if not p.exists()]
if missing_paths:
    raise FileNotFoundError(
        "Thiếu model files. Hãy chạy: python download_model.py\n"
        + "\n".join(missing_paths)
    )

# =========================
# DEVICE
# =========================
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

# =========================
# LOAD TOKENIZER + MODEL + ENCODER
# =========================
# Dùng tokenizer riêng cho từng model để an toàn
type_tokenizer = AutoTokenizer.from_pretrained(
    str(TYPE_MODEL_DIR),
    local_files_only=True,
    use_fast=False
)

category_tokenizer = AutoTokenizer.from_pretrained(
    str(CATEGORY_MODEL_DIR),
    local_files_only=True,
    use_fast=False
)

type_model = AutoModelForSequenceClassification.from_pretrained(
    str(TYPE_MODEL_DIR),
    local_files_only=True
).to(device)

category_model = AutoModelForSequenceClassification.from_pretrained(
    str(CATEGORY_MODEL_DIR),
    local_files_only=True
).to(device)

type_encoder = joblib.load(TYPE_ENCODER_PATH)
category_encoder = joblib.load(CATEGORY_ENCODER_PATH)

type_model.eval()
category_model.eval()

# =========================
# DTO
# =========================
class ParseRequest(BaseModel):
    text: str

# =========================
# HELPERS
# =========================
def clean_text(text: str) -> str:
    if text is None:
        return ""
    text = str(text).lower().strip()
    text = re.sub(r"[.:;!?()]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    return text


def split_transactions(text: str) -> List[str]:
    """
    Tách 1 câu thành nhiều giao dịch.
    Ví dụ:
    - 'ăn sáng 50k nhưng nhận 200k'
    - 'uống trà sữa 40k rồi đổ xăng 50k'
    """
    text = clean_text(text)

    separators = [
        " nhưng ",
        " rồi ",
        " và ",
        " với ",
        " xong ",
        " sau đó ",
        ","
    ]

    parts = [text]

    for sep in separators:
        new_parts = []
        for part in parts:
            new_parts.extend(part.split(sep))
        parts = new_parts

    return [p.strip() for p in parts if p and p.strip()]


def parse_amount(text: str) -> int:
    text = clean_text(text)

    # 1. triệu / tr / củ
    million_match = re.search(r"(\d+(?:[.,]\d+)?)\s*(triệu|tr|củ)", text)
    if million_match:
        value = float(million_match.group(1).replace(",", "."))
        return int(value * 1_000_000)

    # 2. k / nghìn / ngàn
    thousand_match = re.search(r"(\d+(?:[.,]\d+)?)\s*(k|nghìn|ngàn)", text)
    if thousand_match:
        value = float(thousand_match.group(1).replace(",", "."))
        return int(value * 1000)

    # 3. dạng số lớn trực tiếp, ví dụ 50000
    dong_match = re.search(r"\b(\d{5,})\b", text)
    if dong_match:
        return int(dong_match.group(1))

    # 4. fallback: nếu chỉ có số nhỏ thì hiểu là nghìn
    plain_match = re.search(r"\b(\d+)\b", text)
    if plain_match:
        value = int(plain_match.group(1))
        if value < 1000:
            return value * 1000
        return value

    return 0


def extract_action(text: str) -> str:
    s = clean_text(text)

    prefixes = [
        "hôm nay", "hôm qua", "sáng nay", "trưa nay", "chiều nay",
        "tối nay", "tối qua", "lúc nãy", "hồi chiều", "hồi sáng",
        "mới nãy", "vừa nãy", "nay", "chiều qua"
    ]

    changed = True
    while changed:
        changed = False
        for p in prefixes:
            if s.startswith(p + " "):
                s = s[len(p):].strip()
                changed = True

    s = re.sub(
        r"\b\d+(?:[.,]\d+)?\s*(k|nghìn|ngàn|tr|triệu|củ|đ|vnd|vnđ)?\b",
        " ",
        s,
        flags=re.IGNORECASE
    )

    noise_words = [
        "hết", "mất", "tốn", "khoảng", "tầm", "gần", "cỡ",
        "luôn", "á", "nè", "huhu", "xong", "rồi", "nhưng"
    ]
    for w in noise_words:
        s = re.sub(rf"\b{re.escape(w)}\b", " ", s)

    s = re.sub(r"\s+", " ", s).strip()
    return s


def predict_label(
    text: str,
    tokenizer,
    model,
    encoder,
    max_length: int = 64
) -> str:
    inputs = tokenizer(
        text,
        return_tensors="pt",
        truncation=True,
        padding="max_length",
        max_length=max_length
    )
    inputs = {k: v.to(device) for k, v in inputs.items()}

    with torch.no_grad():
        outputs = model(**inputs)
        pred_id = torch.argmax(outputs.logits, dim=1).item()

    return encoder.inverse_transform([pred_id])[0]


def predict_type(text: str) -> str:
    return predict_label(text, type_tokenizer, type_model, type_encoder)


def predict_category(text: str) -> str:
    return predict_label(text, category_tokenizer, category_model, category_encoder)


def parse_transaction_text(text: str) -> List[dict]:
    parts = split_transactions(text)
    results = []

    for part in parts:
        cleaned = clean_text(part)

        item = {
            "amount": parse_amount(cleaned),
            "type": predict_type(cleaned),
            "category": predict_category(cleaned),
            "description": extract_action(cleaned)
        }

        if item["amount"] > 0 and item["type"] and item["category"]:
            results.append(item)

    return results

# =========================
# API
# =========================
@app.get("/")
def root():
    return {"message": "Finance AI Service is running"}


@app.get("/health")
def health():
    return {
        "status": "ok",
        "device": str(device),
        "type_model_loaded": TYPE_MODEL_DIR.exists(),
        "category_model_loaded": CATEGORY_MODEL_DIR.exists(),
        "type_encoder_loaded": TYPE_ENCODER_PATH.exists(),
        "category_encoder_loaded": CATEGORY_ENCODER_PATH.exists(),
    }


@app.post("/parse-transaction")
def parse_transaction(req: ParseRequest):
    return parse_transaction_text(req.text)