from fastapi import FastAPI, UploadFile, File, HTTPException
import uvicorn
import cv2
import numpy as np
from inference import process_inference
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="FoodLens ML API")

@app.post("/internal/ml/analyze")
async def analyze(
    image: UploadFile = File(...)
):
    try:
        img_bytes = await image.read()
        nparr = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise HTTPException(status_code=400, detail="Invalid RGB image file")

        result = process_inference(img)
        return {"success": True, "predictions": result}
        
    except Exception as e:
        logger.error(f"Error during inference: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=9000)
