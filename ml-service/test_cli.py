"""
CLI-тест для инференса без запуска сервера.

Использование:
    python test_cli.py <путь_к_картинке> [--model <путь_к_модели>] [--output <путь_сохранения>]

Примеры:
    python test_cli.py photo.jpg
    python test_cli.py photo.jpg --model custom_yolo.pt --output result.jpg
"""
import sys
import argparse
import os
import cv2
import numpy as np
import logging
import time

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(name)s] %(message)s")
logger = logging.getLogger("test_cli")

def main():
    parser = argparse.ArgumentParser(description="CLI-тест для инференса")
    parser.add_argument("image", help="Путь к изображению")
    parser.add_argument("--model", help="Путь к кастомной модели YOLO (переопределяет дефолтную)")
    parser.add_argument("--output", default="output.jpg", help="Путь для сохранения результата с рамками")
    
    args = parser.parse_args()

    # Переопределяем путь к модели ДО импорта inference
    if args.model:
        os.environ["MODEL_PATH"] = args.model
        logger.info(f"Используем кастомную модель: {args.model}")

    # Читаем изображение
    img = cv2.imread(args.image)
    if img is None:
        print(f"Ошибка: не удалось открыть изображение '{args.image}'")
        sys.exit(1)

    h, w = img.shape[:2]
    print(f"Изображение: {args.image} ({w}x{h})")

    # Импортируем процесс инференса
    from inference import process_inference

    print("\nЗапуск инференса...")
    t0 = time.time()
    results = process_inference(img, None)
    elapsed = time.time() - t0

    # Выводим результат
    print(f"\nГотово за {elapsed:.2f} сек.")
    print(f"Обнаружено объектов: {len(results)}")
    print("-" * 50)

    if not results:
        print("Объекты не найдены.")
    else:
        for i, item in enumerate(results, 1):
            print(f"  {i}. {item['class']}")
            print(f"     Уверенность: {item['confidence']:.1%}")
            print(f"     Объём:       {item['volume_cm3']} см³")
            if 'bbox' in item:
                print(f"     BBox:        {item['bbox']}")
            print()

            # Отрисовываем BBox на изображении
            if 'bbox' in item:
                x1, y1, x2, y2 = item['bbox']
                label = f"{item['class']} {item['confidence']:.2f}"
                cv2.rectangle(img, (x1, y1), (x2, y2), (0, 255, 0), 2)
                cv2.putText(img, label, (x1, max(y1 - 10, 0)), cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0), 2)

    # Сохраняем результат
    cv2.imwrite(args.output, img)
    print(f"Результат с рамками сохранен в: {args.output}")


if __name__ == "__main__":
    main()
