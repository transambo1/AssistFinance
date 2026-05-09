from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import numpy as np

app = FastAPI()

class TransactionRequest(BaseModel):
    amounts: List[float]
    newAmount: float

@app.post("/detect-anomaly")
def detect_anomaly(data: TransactionRequest):

    amounts = np.array(data.amounts)

    mean = np.mean(amounts)
    std = np.std(amounts)

    if std == 0:
        return {
            "anomaly": False,
            "z_score": 0
        }

    z_score = (data.newAmount - mean) / std

    is_anomaly = abs(z_score) > 2.5

    return {
        "anomaly": bool(is_anomaly),
        "z_score": float(z_score),
        "mean": float(mean),
        "std": float(std)
    }