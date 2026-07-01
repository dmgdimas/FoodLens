import os
import cv2
import numpy as np
from ultralytics import YOLO
from pathlib import Path
import logging

logger = logging.getLogger(__name__)

# Путь к модели: сначала из переменной окружения, затем fallback на локальный путь
PROJECT_ROOT = Path(__file__).resolve().parent
MODEL_PATH = Path(
    os.environ.get(
        "MODEL_PATH",
        str(PROJECT_ROOT / "runs" / "segment" / "food_segmentation_augmented-5" / "weights" / "best.pt")
    )
)

# Загружаем модель глобально
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

def calculate_volume(mask: np.ndarray, depth_map: np.ndarray, intrinsics: dict) -> float:
    """
    Вычисляет физический объем объекта в куб. см на основе карты глубин и маски.
    
    mask: бинарная маска (H, W) со значениями 0 и 1 (размер совпадает с depth_map)
    depth_map: карта глубин (H, W) со значениями глубины (предполагаем, что значения уже в сантиметрах)
    intrinsics: dict с fx, fy, cx, cy
    """
    fx = float(intrinsics.get("fx", 500.0))
    fy = float(intrinsics.get("fy", 500.0))
    
    kernel = np.ones((15, 15), np.uint8)
    dilated_mask = cv2.dilate(mask.astype(np.uint8), kernel, iterations=1)
    border_mask = dilated_mask - mask
    
    border_depths = depth_map[border_mask > 0.5]
    if len(border_depths) > 0:
        table_z = float(np.median(border_depths))
    else:
        mask_depths = depth_map[mask > 0.5]
        table_z = float(np.max(mask_depths)) if len(mask_depths) > 0 else 0.0
        
    if table_z <= 0:
        return 0.0

    # Векторизованный расчёт объёма (вместо попиксельного Python-цикла)
    mask_depths = depth_map[mask > 0.5]
    
    # Отфильтровываем невалидные глубины (z <= 0) и пиксели на уровне стола или дальше
    valid = (mask_depths > 0) & (mask_depths < table_z)
    valid_depths = mask_depths[valid]
    
    if len(valid_depths) == 0:
        return 0.0
    
    # Интегрируем объём усечённых пирамид (frustum) для всех пикселей разом
    # V = Σ (table_z³ - z³) / (3 · fx · fy)
    volume_cm3 = np.sum(table_z**3 - valid_depths**3) / (3.0 * fx * fy)
    
    return round(float(volume_cm3), 2)

def process_inference(image: np.ndarray, depth_map: np.ndarray, intrinsics: dict):
    """
    Прогоняет картинку через YOLO-Seg, считает объем по Depth Map и возвращает данные.
    Ожидает, что image и depth_map имеют одинаковое разрешение.
    """
    m = get_model()
    
    results = m(image, imgsz=640)
    
    output_data = []
    
    for result in results:
        if result.masks is None or result.boxes is None:
            continue
            
        boxes = result.boxes
        names = m.names
        
        polygons = result.masks.xy 
        
        for i in range(len(boxes)):
            class_id = int(boxes.cls[i].item())
            class_name = names[class_id]
            confidence = float(boxes.conf[i].item())
            
            poly = polygons[i]
            if len(poly) < 3:
                continue
                
            mask = np.zeros(depth_map.shape[:2], dtype=np.uint8)
            cv2.fillPoly(mask, [poly.astype(np.int32)], 1)
            
            volume_cm3 = calculate_volume(mask, depth_map, intrinsics)
            
            output_data.append({
                "class": class_name,
                "confidence": round(confidence, 3),
                "volume_cm3": volume_cm3
            })
            
    return output_data
