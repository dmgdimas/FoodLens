# Backend API Contract

## Назначение документа

Документ фиксирует API-контракт backend-части FoodLens для согласования работы Android-приложения, Go backend, ML-сервиса и PostgreSQL-БД.

Go backend является единой точкой входа для мобильного приложения. Android-клиент не обращается напрямую к ML-сервису и PostgreSQL. Все внешние запросы от приложения проходят через Go backend.

## Общая схема взаимодействия

```text
Android app
    |
    | multipart/form-data: image
    v
Go backend
    |
    | multipart/form-data: image
    v
ML service
    |
    | JSON: class, confidence, bbox, estimated_volume_cm3
    v
Go backend
    |
    | SELECT product by ml_class
    v
PostgreSQL products
    |
    | density + calories/proteins/fats/carbs per 100g
    v
Go backend
    |
    | JSON: product + weight + nutrients
    v
Android app
```

## Формат данных

Во внешнем API используется JSON в стиле `snake_case`.

Все значения веса возвращаются в граммах. Все значения объёма возвращаются в кубических сантиметрах. Калорийность возвращается в килокалориях. БЖУ возвращаются в граммах.

## Base URL

Во время разработки Android-приложение обращается к backend через локальный адрес или публичный URL туннеля.

Примеры:

```text
http://localhost:8000
https://<generated-url>.ngrok-free.app
https://<generated-url>.trycloudflare.com
```

## Внешние endpoint'ы для Android

### GET /health

Проверка доступности backend-сервиса.

#### Успешный ответ

HTTP 200

```json
{
  "status": "ok",
  "service": "foodlens-backend",
  "version": "0.1.0"
}
```

---

### GET /api/v1/products

Получение списка продуктов, которые backend умеет использовать для расчёта. Endpoint нужен Android-разработчику для экрана ручной коррекции, если пользователь хочет изменить класс продукта после распознавания.

#### Query-параметры

| Параметр | Тип | Обязательный | Описание |
|---|---|---|---|
| supported_only | boolean | нет | Если `true`, возвращаются только продукты, поддерживаемые текущей ML-моделью |

#### Пример запроса

```text
GET /api/v1/products?supported_only=true
```

#### Успешный ответ

HTTP 200

```json
{
  "status": "success",
  "products": [
    {
      "id": 1,
      "ml_class": "Apple",
      "name_ru": "яблоко",
      "name_en": "apple",
      "is_supported": true
    },
    {
      "id": 2,
      "ml_class": "Banana",
      "name_ru": "банан",
      "name_en": "banana",
      "is_supported": true
    }
  ]
}
```

---

### POST /api/v1/analyze

Основной endpoint для анализа фотографии блюда.

Android отправляет изображение на Go backend. Backend передаёт изображение в ML-сервис, получает результат распознавания, обращается к PostgreSQL за плотностью и БЖУ, рассчитывает вес и итоговую пищевую ценность.

#### Формат запроса

```text
Content-Type: multipart/form-data
```

| Поле | Тип | Обязательное | Описание |
|---|---|---|---|
| image | file | да | Фото блюда в формате jpg, jpeg или png |
| client_request_id | string | нет | Идентификатор запроса на стороне Android для сопоставления ответа |

#### Ограничения файла

| Ограничение | Значение |
|---|---|
| Форматы | jpg, jpeg, png |
| Максимальный размер | 10 MB |
| Количество файлов | 1 |

#### Пример успешного ответа

HTTP 200

```json
{
  "status": "success",
  "client_request_id": "android-req-001",
  "processing_time_ms": 1830,
  "detections": [
    {
      "class": "Apple",
      "name_ru": "яблоко",
      "name_en": "apple",
      "confidence": 0.92,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 240,
        "height": 210
      },
      "estimated_volume_cm3": 150.5,
      "estimated_weight_g": 126.4,
      "nutrients": {
        "calories": 65.7,
        "proteins": 0.4,
        "fats": 0.3,
        "carbs": 17.7
      }
    }
  ]
}
```

#### Ответ без найденных объектов

HTTP 200

```json
{
  "status": "success",
  "client_request_id": "android-req-002",
  "processing_time_ms": 910,
  "detections": []
}
```

Такой ответ означает, что запрос обработан корректно, но ML-сервис не нашёл поддерживаемые продукты на изображении.

---

### POST /api/v1/calculate

Расчёт пищевой ценности без повторного обращения к ML-сервису. Endpoint нужен для ручной коррекции результата в Android-приложении.

Например, пользователь может изменить продукт или вес после распознавания, а backend пересчитает калории и БЖУ по данным из PostgreSQL.

#### Формат запроса

```json
{
  "ml_class": "Apple",
  "weight_g": 140.0
}
```

Альтернативный вариант, если Android передаёт объём:

```json
{
  "ml_class": "Apple",
  "volume_cm3": 165.0
}
```

#### Успешный ответ

HTTP 200

```json
{
  "status": "success",
  "product": {
    "ml_class": "Apple",
    "name_ru": "яблоко",
    "name_en": "apple"
  },
  "estimated_weight_g": 140.0,
  "nutrients": {
    "calories": 72.8,
    "proteins": 0.4,
    "fats": 0.3,
    "carbs": 19.6
  }
}
```

## Внутренний контракт Go backend -> ML service

Этот endpoint не вызывается Android-приложением напрямую. Он нужен только для связи Go backend с ML-сервисом.

### POST /internal/ml/analyze

#### Формат запроса

```text
Content-Type: multipart/form-data
field: image
```

#### Ожидаемый ответ ML-сервиса

HTTP 200

```json
{
  "status": "success",
  "model_name": "foodlens-yolov8",
  "model_version": "0.1.0",
  "detections": [
    {
      "class": "Apple",
      "confidence": 0.92,
      "bbox": {
        "x": 120,
        "y": 80,
        "width": 240,
        "height": 210
      },
      "estimated_volume_cm3": 150.5
    }
  ]
}
```

#### Что backend берёт из ответа ML

| Поле | Назначение |
|---|---|
| class | Используется для поиска продукта в PostgreSQL по `products.ml_class` |
| confidence | Передаётся Android-клиенту для отображения уверенности модели |
| bbox | Может использоваться клиентом для визуализации найденного объекта |
| estimated_volume_cm3 | Используется backend для расчёта веса через плотность |

## Логика обработки /api/v1/analyze

1. Android отправляет изображение на `/api/v1/analyze`.
2. Go backend проверяет наличие файла, размер и формат.
3. Go backend передаёт изображение в ML-сервис.
4. ML-сервис возвращает список найденных объектов.
5. Для каждого объекта backend ищет продукт в PostgreSQL по `ml_class`.
6. Если продукт найден и `is_supported = true`, backend берёт плотность и БЖУ.
7. Backend рассчитывает вес:

```text
estimated_weight_g = estimated_volume_cm3 * density_g_per_cm3
```

8. Backend рассчитывает калорийность и БЖУ:

```text
value = estimated_weight_g / 100 * value_per_100g
```

9. Backend возвращает Android-клиенту итоговый JSON.

## Стандартный формат ошибок

Все ошибки backend возвращает в едином формате.

```json
{
  "status": "error",
  "error": {
    "code": "INVALID_IMAGE_FORMAT",
    "message": "Only jpg, jpeg and png images are supported",
    "details": {
      "allowed_formats": ["jpg", "jpeg", "png"]
    }
  }
}
```

## Основные ошибки

| HTTP-код | code | Когда возникает |
|---|---|---|
| 400 | IMAGE_REQUIRED | В запросе нет поля `image` |
| 400 | INVALID_IMAGE_FORMAT | Неподдерживаемый формат файла |
| 413 | IMAGE_TOO_LARGE | Размер изображения больше допустимого |
| 404 | PRODUCT_NOT_SUPPORTED | ML вернул класс, которого нет в `products`, или `is_supported = false` |
| 502 | ML_SERVICE_UNAVAILABLE | ML-сервис недоступен или вернул ошибку |
| 500 | INTERNAL_ERROR | Неожиданная ошибка backend |

## Пример ошибки неподдерживаемого класса

HTTP 404

```json
{
  "status": "error",
  "error": {
    "code": "PRODUCT_NOT_SUPPORTED",
    "message": "Detected product class is not supported by backend catalog",
    "details": {
      "ml_class": "UnknownClass"
    }
  }
}
```

## Пример ошибки ML-сервиса

HTTP 502

```json
{
  "status": "error",
  "error": {
    "code": "ML_SERVICE_UNAVAILABLE",
    "message": "ML service is unavailable or returned invalid response",
    "details": null
  }
}
```

## Требования для Android-разработчика

Android-приложение должно:

1. Отправлять изображение через `multipart/form-data` в поле `image`.
2. Использовать endpoint `/api/v1/analyze` для основного сценария распознавания.
3. Использовать поле `detections` из ответа для отображения результата.
4. Сохранять историю локально на устройстве.
5. При ручной коррекции веса или класса использовать `/api/v1/calculate`.
6. Не обращаться напрямую к ML-сервису и PostgreSQL.

## Требования для ML-разработчика

ML-сервис должен:

1. Принимать изображение от Go backend.
2. Возвращать класс продукта в поле `class`.
3. Возвращать уверенность модели в поле `confidence`.
4. Возвращать bounding box в поле `bbox`.
5. Возвращать примерный объём в поле `estimated_volume_cm3`.
6. Использовать стабильные названия классов, согласованные с `products.ml_class`.

## Совместимость с MVP

Контракт соответствует MVP-ограничениям:

- нет авторизации;
- нет облачной истории;
- Android хранит историю локально;
- backend хранит только справочник продуктов;
- ML-сервис не вызывается Android-приложением напрямую;
- backend возвращает готовые значения веса, калорийности и БЖУ.
