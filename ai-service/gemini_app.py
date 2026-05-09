from fastapi import FastAPI
from pydantic import BaseModel
import google.generativeai as genai
import json

app = FastAPI(title="Gemini Finance AI")

# =========================
# GEMINI CONFIG
# =========================

GEMINI_API_KEY = "AIzaSyC16REUTLZVuYzLbTND9Sk4DcKXq9KJP1o"

genai.configure(api_key=GEMINI_API_KEY)

model = genai.GenerativeModel("gemini-2.5-flash")

# =========================
# DTO
# =========================

class SpendingTrendRequest(BaseModel):
    expenses: list[int]

# =========================
# API
# =========================

@app.get("/")
def root():
    return {"message": "Gemini AI running"}

@app.post("/predict-spending")
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
    except:
        return {
            "prediction": 0,
            "trend": "unknown",
            "analysis": text
        }