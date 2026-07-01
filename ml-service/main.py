from typing import Optional

from fastapi import FastAPI, UploadFile, File, Form, HTTPException
import uvicorn
import cv2
import numpy as np
from inference import process_inference
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="FoodLens ML API with AR Depth Map Integration")

@app.post("/internal/ml/analyze")
async def analyze(
    image: UploadFile = File(...),
    depth_map: UploadFile = File(...),
    fx: float = Form(500.0),
    fy: float = Form(500.0),
    cx: Optional[float] = Form(None),
    cy: Optional[float] = Form(None)
):
    try:
        img_bytes = await image.read()
        nparr = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise HTTPException(status_code=400, detail="Invalid RGB image file")
            
        depth_bytes = await depth_map.read()
        depth_nparr = np.frombuffer(depth_bytes, np.uint8)
        d_map = cv2.imdecode(depth_nparr, cv2.IMREAD_UNCHANGED)
        if d_map is None:
            raise HTTPException(status_code=400, detail="Invalid depth map file")
            
        if d_map.dtype == np.uint16:
            d_map = d_map.astype(np.float32) / 10.0
        else:
            d_map = d_map.astype(np.float32)
            
        if img.shape[:2] != d_map.shape[:2]:
            logger.info(f"Resizing depth map from {d_map.shape[:2]} to {img.shape[:2]}")
            d_map = cv2.resize(d_map, (img.shape[1], img.shape[0]), interpolation=cv2.INTER_NEAREST)
            
        intrinsics = {
            "fx": fx,
            "fy": fy,
            "cx": cx if cx is not None else img.shape[1] / 2.0,
            "cy": cy if cy is not None else img.shape[0] / 2.0
        }
        
        result = process_inference(img, d_map, intrinsics)
        return {"success": True, "predictions": result}
        
    except Exception as e:
        logger.error(f"Error during inference: {e}")
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=9000)
