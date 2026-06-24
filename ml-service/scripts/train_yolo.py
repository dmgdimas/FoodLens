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
    model = YOLO("yolov8n-seg.pt")

    # Параметры обучения
    # Для быстрой проверки делаем всего 1-2 эпохи. 
    # В реальном пайплайне epochs=50..100
    epochs = 2
    imgsz = 640
    batch_size = 16

    print("Начинаем обучение...")
    results = model.train(
        data=str(DATA_YAML_PATH),
        epochs=epochs,
        imgsz=imgsz,
        batch=batch_size,
        name="food_segmentation_test",
        # Базовые аугментации YOLOv8 включены по умолчанию
        # (hsv_h, hsv_s, hsv_v, degrees, translate, scale, shear, perspective, flipud, fliplr, mosaic, mixup)
        # Можем немного усилить поворот:
        degrees=15.0,
        project=str(PROJECT_ROOT / "runs" / "segment"),
        device="0"  # Использование CUDA
    )

    print("Обучение завершено. Результаты сохранены в runs/segment/food_segmentation_test")

if __name__ == "__main__":
    main()
