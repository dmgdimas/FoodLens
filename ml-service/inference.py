import os
import cv2
import numpy as np
from ultralytics import YOLO
from pathlib import Path
import logging

logger = logging.getLogger(__name__)

# Путь к модели
PROJECT_ROOT = Path(__file__).resolve().parent
MODEL_PATH = Path(
    os.environ.get(
        "MODEL_PATH",
        str(PROJECT_ROOT / "runs" / "detect" / "food_detection_augmented-3" / "weights" / "best.pt")
    )
)

model = None
if MODEL_PATH.exists():
    model = YOLO(str(MODEL_PATH))

def get_model():
    global model
    if model is None:
        if not MODEL_PATH.exists():
            raise FileNotFoundError(f"Модель не найдена по пути: {MODEL_PATH}. Сначала запустите обучение.")
        model = YOLO(str(MODEL_PATH))
    return model

HARDCODED_VOLUMES = {
    "almond": 2.0,
    "apple": 150.0,
    "apricot": 35.0,
    "artichoke": 200.0,
    "asparagus": 15.0,
    "avocado": 170.0,
    "banana": 120.0,
    "bean curd/tofu": 250.0,
    "bell pepper/capsicum": 160.0,
    "blackberry": 5.0,
    "blueberry": 1.0,
    "broccoli": 200.0,
    "brussels sprouts": 15.0,
    "cantaloup/cantaloupe": 1000.0,
    "carrot": 80.0,
    "cauliflower": 300.0,
    "cayenne/cayenne spice/cayenne pepper/cayenne pepper spice/red pepper/red pepper": 5.0,
    "celery": 40.0,
    "cherry": 8.0,
    "chickpea/garbanzo": 1.0,
    "chili/chili vegetable/chili pepper/chili pepper vegetable/chilli/chilli vegetable/chilly/chilly": 10.0,
    "clementine": 60.0,
    "coconut/cocoanut": 800.0,
    "edible corn/corn/maize": 150.0,
    "cucumber/cuke": 250.0,
    "date/date fruit": 10.0,
    "eggplant/aubergine": 350.0,
    "fig/fig fruit": 40.0,
    "garlic/ail": 30.0,
    "ginger/gingerroot": 50.0,
    "Strawberry": 15.0,
    "gourd": 500.0,
    "grape": 5.0,
    "green bean": 5.0,
    "green onion/spring onion/scallion": 10.0,
    "Tomato": 130.0,
    "kiwi fruit": 75.0,
    "lemon": 100.0,
    "lettuce": 300.0,
    "lime": 50.0,
    "mandarin orange": 80.0,
    "melon": 1500.0,
    "mushroom": 20.0,
    "onion": 110.0,
    "orange/orange fruit": 140.0,
    "papaya": 400.0,
    "pea/pea food": 1.0,
    "peach": 130.0,
    "pear": 140.0,
    "persimmon": 120.0,
    "pickle": 50.0,
    "pineapple": 1000.0,
    "potato": 170.0,
    "prune": 20.0,
    "pumpkin": 3000.0,
    "radish/daikon": 50.0,
    "raspberry": 3.0,
    "strawberry": 15.0,
    "sweet potato": 200.0,
    "tomato": 130.0,
    "turnip": 150.0,
    "watermelon": 4000.0,
    "zucchini/courgette": 300.0,
}

def get_volume_for_class(class_name: str) -> float:
    return HARDCODED_VOLUMES.get(class_name, 0.0)

def process_inference(image: np.ndarray, intrinsics: dict = None):
    m = get_model()
    
    # YOLO inference (detection)
    results = m(image, imgsz=640)
    
    output_data = []
    for result in results:
        if result.boxes is None:
            continue
            
        boxes = result.boxes
        names = m.names
        
        for i in range(len(boxes)):
            class_id = int(boxes.cls[i].item())
            class_name = names[class_id]
            confidence = float(boxes.conf[i].item())
            
            volume_cm3 = get_volume_for_class(class_name)
            
            output_data.append({
                "class": class_name,
                "confidence": round(confidence, 3),
                "volume_cm3": volume_cm3
            })
            
    return output_data
