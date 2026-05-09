from fastapi import APIRouter
from anomaly.schemas import AnomalyRequest
from anomaly.detector import detect_anomaly

router = APIRouter()

@router.post("/anomaly-detection")
def anomaly_detection(req: AnomalyRequest):

    result = detect_anomaly(
        req.amount,
        req.history
    )

    return result