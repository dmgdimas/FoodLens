# Чеклист проверки backend API

Документ содержит чеклист для проверки backend API проекта FoodLens перед подключением Android-приложения.

Цель проверки — убедиться, что backend запускается, доступен локально и через публичный URL, а основные endpoint'ы работают корректно.

## 1. Запуск проекта

Из корня проекта нужно запустить Docker Compose:

```bash
docker compose up --build
```

После запуска должны быть доступны основные сервисы:

```text
backend
postgres
ml-service
```

Backend должен быть доступен локально на порту:

```text
8000
```

Локальный URL backend:

```text
http://localhost:8000
```

## 2. Проверка локального backend

### Health check

```bash
curl http://localhost:8000/health
```

Ожидаемый ответ:

```json
{
  "status": "ok"
}
```

Если этот запрос не работает, значит backend не запустился или порт `8000` не проброшен наружу.

## 3. Проверка списка продуктов

```bash
curl http://localhost:8000/api/v1/products
```

Ожидаемый формат ответа:

```json
{
  "status": "success",
  "products": []
}
```

В реальном ответе массив `products` должен содержать продукты из PostgreSQL.

Для MVP список продуктов должен быть согласован со списком классов ML-модели.

Примеры ожидаемых `ml_class`:

```text
apple
banana
cabbage
capsicum
cauliflower
corn
cucumber
eggplant
garlic
ginger
grapes
lemon
mango
onion
orange
pear
peas
pineapple
potato
radish
soybean
sweetpotato
tomato
watermelon
```

## 4. Проверка ручного расчёта БЖУ

Endpoint:

```text
POST /api/v1/calculate
```

Пример запроса:

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple","weight_g":140}'
```

Ожидаемый формат ответа:

```json
{
  "status": "success",
  "product": {
    "ml_class": "apple",
    "name_ru": "яблоко",
    "name_en": "apple"
  },
  "estimated_weight_g": 140,
  "nutrients": {
    "calories": 72.8,
    "proteins": 0.4,
    "fats": 0.3,
    "carbs": 19.6
  }
}
```

## 5. Проверка ошибок ручного расчёта

### Пустой `ml_class`

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"","weight_g":140}'
```

Ожидаемый код ошибки:

```text
INVALID_INPUT
```

### Отсутствует `weight_g`

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple"}'
```

Ожидаемый код ошибки:

```text
INVALID_INPUT
```

### Нулевой вес

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple","weight_g":0}'
```

Ожидаемый код ошибки:

```text
INVALID_INPUT
```

### Некорректный продукт

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"unknown_product","weight_g":100}'
```

Ожидаемый код ошибки:

```text
PRODUCT_NOT_SUPPORTED
```

## 6. Проверка анализа изображения

Endpoint:

```text
POST /api/v1/analyze
```

Изображение должно отправляться как `multipart/form-data`.

Название поля с изображением:

```text
image
```

Пример запроса:

```bash
curl -X POST http://localhost:8000/api/v1/analyze \
  -F "image=@test.jpg"
```

Ожидаемый формат ответа:

```json
{
  "status": "success",
  "detections": [
    {
      "class": "apple",
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

Фактические значения зависят от ответа ML-сервиса.

Если текущий ML-сервис возвращает тестовый класс `apple`, то это нормально для проверки backend-цепочки.

## 7. Проверка ошибок анализа изображения

### Запрос без изображения

```bash
curl -X POST http://localhost:8000/api/v1/analyze
```

Ожидаемый код ошибки:

```text
INVALID_MULTIPART_FORM
```

или:

```text
IMAGE_REQUIRED
```

Зависит от того, дошёл ли запрос до разбора multipart-формы.

### Неправильное название поля

```bash
curl -X POST http://localhost:8000/api/v1/analyze \
  -F "file=@test.jpg"
```

Ожидаемый код ошибки:

```text
IMAGE_REQUIRED
```

Правильное название поля:

```text
image
```

### Неподдерживаемый формат файла

```bash
echo "test" > test.txt

curl -X POST http://localhost:8000/api/v1/analyze \
  -F "image=@test.txt"
```

Ожидаемый код ошибки:

```text
INVALID_IMAGE_FORMAT
```

Backend принимает только:

```text
jpg
jpeg
png
```

## 8. Проверка через ngrok

Сначала нужно запустить backend:

```bash
docker compose up --build
```

Затем в отдельном терминале открыть порт backend через ngrok:

```bash
ngrok http 8000
```

ngrok выдаст публичный HTTPS URL, например:

```text
https://example-id.ngrok-free.app
```

Дальше нужно заменить `<BACKEND_PUBLIC_URL>` на этот URL.

### Проверка health через публичный URL

```bash
curl <BACKEND_PUBLIC_URL>/health
```

Ожидаемый ответ:

```json
{
  "status": "ok"
}
```

### Проверка списка продуктов через публичный URL

```bash
curl <BACKEND_PUBLIC_URL>/api/v1/products
```

Ожидаемый формат ответа:

```json
{
  "status": "success",
  "products": []
}
```

### Проверка ручного расчёта через публичный URL

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple","weight_g":140}'
```

### Проверка анализа изображения через публичный URL

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/analyze \
  -F "image=@test.jpg"
```

Если эти запросы работают через публичный URL, Android-приложение сможет обращаться к backend по этому же base URL.

## 9. Что передать Android-разработчику

Android-разработчику нужно передать:

```text
1. API base URL от ngrok
2. Список endpoint'ов
3. Формат запроса для /api/v1/analyze
4. Название поля изображения: image
5. Формат ответа /api/v1/analyze
6. Список поддерживаемых ml_class
```

Пример base URL:

```text
https://example-id.ngrok-free.app
```

Пример полного endpoint'а:

```text
POST https://example-id.ngrok-free.app/api/v1/analyze
```

## 10. Минимальный чеклист готовности backend

Перед интеграцией с Android нужно убедиться, что выполнены пункты:

```text
[ ] docker compose up --build запускается без ошибок
[ ] GET /health работает локально
[ ] GET /api/v1/products возвращает список продуктов
[ ] POST /api/v1/calculate считает БЖУ по весу
[ ] POST /api/v1/analyze принимает изображение
[ ] /api/v1/analyze возвращает class, weight и nutrients
[ ] ошибки валидации возвращаются в JSON-формате
[ ] ngrok создаёт публичный HTTPS URL
[ ] GET /health работает через ngrok URL
[ ] /api/v1/products работает через ngrok URL
[ ] /api/v1/calculate работает через ngrok URL
[ ] /api/v1/analyze работает через ngrok URL
[ ] Android-разработчику передан актуальный API base URL
```

## Итог

Если все пункты чеклиста выполнены, backend API готов к подключению Android-приложения.

Основной сценарий для проверки интеграции:

```text
Android отправляет фото
        ↓
Go backend принимает multipart/form-data
        ↓
Go backend отправляет изображение в ML-сервис
        ↓
ML-сервис возвращает class и estimated_volume_cm3
        ↓
Go backend ищет продукт в PostgreSQL
        ↓
Go backend рассчитывает вес, калории и БЖУ
        ↓
Android получает JSON-ответ
```
