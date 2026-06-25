"""
Скрипт для обучения модели YOLOv8-seg на датасете продуктов.
"""

from ultralytics import YOLO
import os
from pathlib import Path

# Корневая папка проекта
PROJECT_ROOT = Path(__file__).resolve().parent.parent
DATA_YAML_PATH = PROJECT_ROOT / "dataset" / "data.yaml"

def main():
    print(f"Используем data.yaml: {DATA_YAML_PATH}")
    if not DATA_YAML_PATH.exists():
        print("Ошибка: data.yaml не найден!")
        return

    # Загружаем базовую модель для сегментации (самую легкую)
    model = YOLO("yolov8s-seg.pt")

    # Параметры обучения
    epochs = 50  # Увеличено для полноценного обучения (было 2)
    imgsz = 640
    batch_size = 16

    print(f"Начинаем обучение на {epochs} эпох...")
    
    results = model.train(
        data=str(DATA_YAML_PATH),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch_size,
        name="food_segmentation_augmented",
        project=str(PROJECT_ROOT / "runs" / "segment"),
        device="0",  # Использование CUDA
        
        degrees=15.0,
        hsv_s=0.5,
        hsv_v=0.4,
        copy_paste=0.3, 
        mixup=0.2,
        mosaic=1.0,
        verbose=True
    )

    print("Обучение завершено. Результаты сохранены в runs/segment/food_segmentation_test")

if __name__ == "__main__":
    main()
