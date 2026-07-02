import cv2
import numpy as np
import requests
import time
import subprocess
import sys
import os

def main():
    print("Запуск ML-сервиса локально на порту 9000...")
    
    # Настраиваем окружение для сервера, чтобы он использовал правильную модель
    env = os.environ.copy()
    model_path = os.path.join("runs", "detect", "food_detection_augmented-3", "weights", "best.pt")
    if not os.path.exists(model_path):
        model_path = "yolov8n.pt"
    env["MODEL_PATH"] = model_path
    print(f"Сервер будет запущен с моделью: {model_path}")
    
    server_proc = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "main:app", "--port", "9000"],
        cwd="c:\\FoodLens\\ml-service",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        env=env,
        text=True
    )
    
    # Ожидание ответа сервера
    max_retries = 15
    server_ready = False
    url = "http://127.0.0.1:9000/docs" # Swagger UI для проверки готовности
    
    print("Ожидание загрузки модели PyTorch...")
    for i in range(max_retries):
        time.sleep(2)
        if server_proc.poll() is not None:
            stdout, stderr = server_proc.communicate()
            print("ОШИБКА: Сервер упал при запуске!")
            print("STDOUT:\n", stdout)
            print("STDERR:\n", stderr)
            return
            
        try:
            r = requests.get(url)
            if r.status_code == 200:
                server_ready = True
                break
        except requests.ConnectionError:
            pass
            
    if not server_ready:
        print("ОШИБКА: Сервер не запустился вовремя.")
        server_proc.terminate()
        return

    print("Сервер готов!")

    try:
        analyze_url = "http://127.0.0.1:9000/internal/ml/analyze"

        # ============================================================
        # Тест 1: Запрос с валидным изображением (позитивный тест)
        # ============================================================
        print("\n" + "=" * 60)
        print("ТЕСТ 1: Запрос с валидным изображением (без fx/fy)")
        print("=" * 60)

        # Создаем синтетическое изображение
        img = np.zeros((480, 640, 3), dtype=np.uint8)
        cv2.circle(img, (320, 240), 100, (0, 200, 100), -1)
        _, img_encoded = cv2.imencode('.jpg', img)

        files = {
            'image': ('test_photo.jpg', img_encoded.tobytes(), 'image/jpeg'),
        }

        resp = requests.post(analyze_url, files=files)

        print(f"Код ответа: {resp.status_code}")
        try:
            result = resp.json()
            print("JSON:\n", result)
            assert resp.status_code == 200, f"Ожидался 200, получен {resp.status_code}"
            assert result.get("success") is True, "success должен быть True"
            assert "predictions" in result, "В ответе должно быть поле predictions"
            
            # Проверяем структуру предсказаний
            for pred in result["predictions"]:
                assert "class" in pred, "Каждое предсказание должно содержать class"
                assert "confidence" in pred, "Каждое предсказание должно содержать confidence"
                assert "volume_cm3" in pred, "Каждое предсказание должно содержать volume_cm3"
                assert "bbox" not in pred, "В ответе НЕ должно быть координат bbox"
            
            print("[OK] Тест 1 ПРОЙДЕН")
        except Exception as e:
            print(f"[FAIL] Тест 1 ПРОВАЛЕН: {e}")

        # ============================================================
        # Тест 2: Запрос с невалидным файлом (негативный тест)
        # ============================================================
        print("\n" + "=" * 60)
        print("ТЕСТ 2: Запрос с невалидным файлом")
        print("=" * 60)

        files2 = {
            'image': ('test.txt', b'this is not an image file', 'text/plain'),
        }

        resp2 = requests.post(analyze_url, files=files2)

        print(f"Код ответа: {resp2.status_code}")
        try:
            print("Ответ сервера:", resp2.text)
            assert resp2.status_code in [400, 500], f"Ожидался код 400 или 500, получен {resp2.status_code}"
            print("[OK] Тест 2 ПРОЙДЕН")
        except Exception as e:
            print(f"[FAIL] Тест 2 ПРОВАЛЕН: {e}")

    except Exception as e:
        print(f"Ошибка при запросе: {e}")
    finally:
        print("\nОстановка сервера...")
        server_proc.terminate()
        server_proc.wait()

if __name__ == "__main__":
    main()
