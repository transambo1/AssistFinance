from fastapi import FastAPI
from parse_api import router as parse_router
from anomaly_api import router as anomaly_router
from gemini_app import router as gemini_router

app = FastAPI(title="AssistFinance AI Aggregator")

@app.get("/")
def root():
    return {"message": "AssistFinance AI Aggregator is running"}

@app.get("/health")
def health():
    return {"status": "ok"}

app.include_router(parse_router, tags=["Parse"])
app.include_router(anomaly_router, tags=["Anomaly"])
app.include_router(gemini_router, tags=["Gemini"])