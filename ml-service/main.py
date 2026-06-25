from fastapi import FastAPI, UploadFile, File

app = FastAPI(title="FoodLens ML Service")


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/predict")
async def predict(image: UploadFile = File(...)):
    return {
        "status": "success",
        "detections": [
            {
                "class": "Apple",
                "confidence": 0.92,
                "estimated_volume_cm3": 150.5
            }
        ]
    }
