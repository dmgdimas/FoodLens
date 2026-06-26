import numpy as np
import cv2
from inference import calculate_volume

def test_volume_calculation():
    # 1. Создаем фиктивную маску 100x100 пикселей.
    # Пусть еда занимает квадрат 50x50 пикселей в центре.
    mask = np.zeros((100, 100), dtype=np.uint8)
    mask[25:75, 25:75] = 1
    
    # 2. Создаем фиктивную карту глубин 100x100.
    # Пусть "стол" находится на глубине 50 см (50.0).
    depth_map = np.full((100, 100), 50.0, dtype=np.float32)
    
    # Сделаем "еду" выпуклой.
    # В центре глубина будет 40 см (то есть она возвышается над столом на 10 см).
    # По краям квадрата глубина плавно спускается к 50 см.
    for y in range(25, 75):
        for x in range(25, 75):
            # Расстояние от центра (50, 50)
            dist_y = abs(y - 50)
            dist_x = abs(x - 50)
            max_dist = max(dist_y, dist_x)
            # Если в центре (dist=0), то высота 10 см (глубина 40). 
            # На краю (dist=25), высота 0 (глубина 50).
            height = 10.0 * (1.0 - max_dist / 25.0)
            depth_map[y, x] = 50.0 - height
            
    # Параметры камеры (Intrinsics)
    # Пусть fx = fy = 500 пикселей.
    intrinsics = {
        "fx": 500.0,
        "fy": 500.0,
        "cx": 50.0,
        "cy": 50.0
    }
    
    # Вычисляем объем
    volume = calculate_volume(mask, depth_map, intrinsics)
    
    print("--- ТЕСТ ИНТЕГРАЦИИ AR DEPTH MAP ---")
    print(f"Размер маски: 50x50 пикселей")
    print(f"Глубина стола: 50 см")
    print(f"Максимальная высота объекта: 10 см")
    print(f"Рассчитанный объем: {volume} куб. см")
    print("------------------------------------")
    
if __name__ == "__main__":
    test_volume_calculation()
