import cv2
import numpy as np
import requests
import time
import subprocess
import sys

def main():
    print("Запуск ML-сервиса локально на порту 9000...")
    
    server_proc = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "main:app", "--port", "9000"],
        cwd="c:\\FoodLens\\ml-service",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True
    )
    
    # Ожидание ответа сервера
    max_retries = 15
    server_ready = False
    url = "http://127.0.0.1:9000/docs" # Swagger UI для проверки готовности
    
    print("Ожидание загрузки модели PyTorch (это может занять до 15-20 секунд)...")
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
        print("Генерация тестового изображения и карты глубин...")
        img = np.zeros((640, 640, 3), dtype=np.uint8)
        cv2.rectangle(img, (200, 200), (400, 400), (255, 255, 255), -1)
        _, img_encoded = cv2.imencode('.jpg', img)

        depth = np.ones((640, 640), dtype=np.uint16) * 50 
        _, depth_encoded = cv2.imencode('.png', depth)

        analyze_url = "http://127.0.0.1:9000/internal/ml/analyze"
        print(f"Отправка POST запроса на {analyze_url}...")
        
        files = {
            'image': ('test_image.jpg', img_encoded.tobytes(), 'image/jpeg'),
            'depth_map': ('test_depth.png', depth_encoded.tobytes(), 'image/png')
        }
        data = {
            'fx': '500.0',
            'fy': '500.0'
        }
        
        resp = requests.post(analyze_url, files=files, data=data)
        
        print("\n--- РЕЗУЛЬТАТ ---")
        print(f"Код ответа: {resp.status_code}")
        try:
            print("JSON:\n", resp.json())
        except Exception:
            print("Текст:\n", resp.text)
            
    except Exception as e:
        print(f"Ошибка при запросе: {e}")
    finally:
        print("\nОстановка сервера...")
        server_proc.terminate()
        server_proc.wait()

if __name__ == "__main__":
    main()
