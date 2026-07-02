"""
Скрипт для обучения модели YOLOv8 на датасете продуктов (детекция).
"""

from ultralytics import YOLO, settings
import os
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_YAML_PATH = PROJECT_ROOT / "dataset" / "LVIS_Fruits_And_Vegetables" / "data.yaml"

settings.update({'datasets_dir': str(PROJECT_ROOT / "dataset")})

def main():
    print(f"Используем data.yaml: {DATA_YAML_PATH}")
    if not DATA_YAML_PATH.exists():
        print("Ошибка: data.yaml не найден!")
        return

    # Загрузка базовой модели
    model = YOLO("yolov8n.pt")

    # Параметры обучения
    epochs = 80
    imgsz = 640
    batch_size = 16

    print(f"Начинаем обучение на {epochs} эпох...")
    
    results = model.train(
        data=str(DATA_YAML_PATH),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch_size,
        name="food_detection_augmented",
        project=str(PROJECT_ROOT / "runs" / "detect"),
        device="0",  # Использование CUDA
        
        degrees=15.0,
        hsv_s=0.5,
        hsv_v=0.4,
        copy_paste=0.0,
        mixup=0.0,
        mosaic=1.0,
        flipud=0.3,
        close_mosaic=10,
        patience=20,
        hsv_h=0.015,
        verbose=True

    )

    print("Обучение завершено. Результаты сохранены в runs/detect/food_detection_augmented")

if __name__ == "__main__":
    main()
