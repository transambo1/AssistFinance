from fastapi import APIRouter
from pydantic import BaseModel
from typing import Dict
import google.generativeai as genai
import json

router = APIRouter()

# =========================
# GEMINI CONFIG
# =========================
GEMINI_API_KEY = "your_gemini_api_key_here"

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-2.5-flash")

# =========================
# DTO
# =========================
class SpendingTrendRequest(BaseModel):
    expenses: list[int]


class SavingAdviceRequest(BaseModel):
    categories: Dict[str, float]

# =========================
# API
# =========================
@router.get("/gemini/health")
def gemini_health():
    return {"status": "ok"}


@router.post("/predict-spending")
def predict_spending(req: SpendingTrendRequest):
    prompt = f"""
    Bạn là AI chuyên phân tích tài chính cá nhân.

    {req.expenses}

    Hãy:
    1. Phân tích xu hướng chi tiêu
    2. Dự đoán tháng tiếp theo
    3. Cho biết trend: increasing / decreasing / stable

    Hãy trả lời hoàn toàn bằng TIẾNG VIỆT.
    Trả JSON:

    {{
      "prediction": number,
      "trend": "increasing/decreasing/stable",
      "analysis": "text"
    }}

    Chỉ trả JSON.
    """

    response = model.generate_content(prompt)
    text = response.text.strip()
    text = text.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(text)
    except Exception:
        return {
            "prediction": 0,
            "trend": "unknown",
            "analysis": text
        }


@router.post("/saving-advice")
def saving_advice(req: SavingAdviceRequest):
    prompt = f"""
    Bạn là AI chuyên tư vấn tài chính cá nhân.

    Đây là thống kê chi tiêu theo danh mục:

    {req.categories}

    Hãy:
    - phân tích chi tiêu
    - gợi ý tiết kiệm
    - đề xuất cắt giảm hợp lý
    - trả lời bằng tiếng Việt

    Chỉ trả về JSON hợp lệ:

    {{
      "analysis": "...",
      "tips": [
        "...",
        "..."
      ]
    }}
    """

    response = model.generate_content(prompt)
    text = response.text.strip()
    text = text.replace("```json", "").replace("```", "").strip()

    try:
        return json.loads(text)
    except Exception:
        return {
            "analysis": text,
            "tips": []
        }