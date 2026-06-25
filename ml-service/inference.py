import cv2
import numpy as np
from ultralytics import YOLO
import json
import math
from pathlib import Path

# Абсолютный путь к модели
PROJECT_ROOT = Path(__file__).resolve().parent
MODEL_PATH = PROJECT_ROOT / "runs" / "segment" / "food_segmentation_augmented" / "weights" / "best.pt"

# Допущения для расчета объема
DISTANCE_CM = 20.0
# Допустим, на расстоянии 20 см картинка 640x640 пикселей охватывает область 30x30 см.
# Значит, 1 пиксель = 30 / 640 = 0.046875 см
PIXEL_TO_CM_RATIO = 30.0 / 640.0

# Загружаем модель глобально при импорте модуля
model = YOLO(str(MODEL_PATH))

def calculate_volume(mask_area_px: int, class_name: str = "unknown") -> float:
    """
    Вычисляет физический объем объекта в куб. см на основе площади пикселей маски.
    Использует допущение Z=20 см и аппроксимацию до эллипсоида (сжатого шара).
    """
    # Физическая площадь в кв. см
    area_cm2 = mask_area_px * (PIXEL_TO_CM_RATIO ** 2)
    
    if area_cm2 <= 0:
        return 0.0
        
    # Эквивалентный радиус проекции (если смотреть сверху)
    radius_cm = math.sqrt(area_cm2 / math.pi)
    
    volume_cm3 = (4.0 / 3.0) * math.pi * (radius_cm ** 3)
    flatness_factor = 0.6 
    
    return round(volume_cm3 * flatness_factor, 2)

def run_inference(image_path: str):
    """
    Прогоняет картинку через YOLO-Seg, считает объем и возвращает JSON.
    """
    img = cv2.imread(image_path)
    if img is None:
        raise ValueError(f"Не удалось прочитать изображение: {image_path}")

    # Инференс модели (сразу делаем ресайз до 640)
    results = model(img, imgsz=640)
    
    output_data = []
    
    for result in results:
        # result.boxes содержит классы и уверенность
        # result.masks содержит сами маски
        if result.masks is None:
            continue
            
        boxes = result.boxes
        masks = result.masks.data  # Tensor (N, H, W)
        
        # Получаем оригинальные размеры, чтобы знать масштаб маски
        orig_shape = result.orig_shape
        names = model.names
        
        for i in range(len(boxes)):
            class_id = int(boxes.cls[i].item())
            class_name = names[class_id]
            confidence = float(boxes.conf[i].item())
            
            # Маска для текущего объекта
            mask = masks[i].cpu().numpy()
            
            area_px = int(np.sum(mask > 0.5))
            
            # Вычисляем объем
            volume_cm3 = calculate_volume(area_px, class_name)
            
            output_data.append({
                "class": class_name,
                "confidence": round(confidence, 3),
                "volume_cm3": volume_cm3
            })
            
    return json.dumps(output_data, indent=4, ensure_ascii=False)

if __name__ == "__main__":
    import sys
    if len(sys.argv) > 1:
        img_path = sys.argv[1]
        print(run_inference(img_path))
    else:
        print("Использование: python inference.py <путь_к_картинке>")
