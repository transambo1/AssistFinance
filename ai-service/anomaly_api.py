from fastapi import APIRouter
from pydantic import BaseModel
from typing import List
import numpy as np

router = APIRouter()

class TransactionRequest(BaseModel):
    amounts: List[float]
    newAmount: float


@router.get("/anomaly/health")
def anomaly_health():
    return {"status": "ok"}


@router.post("/detect-anomaly")
def detect_anomaly(data: TransactionRequest):
    amounts = np.array(data.amounts, dtype=float)

    if amounts.size == 0:
        return {
            "isAnomaly": False,
            "zScore": 0.0,
            "mean": 0.0,
            "std": 0.0
        }

    mean = float(np.mean(amounts))
    std = float(np.std(amounts))

    if std == 0:
        return {
            "isAnomaly": False,
            "zScore": 0.0,
            "mean": mean,
            "std": std
        }

    z_score = float((data.newAmount - mean) / std)
    is_anomaly = abs(z_score) > 2.5

    return {
        "isAnomaly": bool(is_anomaly),
        "zScore": z_score,
        "mean": mean,
        "std": std
    }