# Подключение Android-приложения к backend

Документ описывает, как Android-приложение должно подключаться к backend API проекта FoodLens во время разработки и тестирования MVP.

## Общая схема подключения

Android-приложение не подключается напрямую к PostgreSQL и не обращается напрямую к ML-сервису.

Правильная схема работы:

```text
Android-приложение
        |
        | HTTP-запросы
        v
Go backend API
        |
        | внутренняя сеть Docker Compose
        v
PostgreSQL

Go backend API
        |
        | внутренняя сеть Docker Compose
        v
ML-сервис
```

Android-приложение должно знать только публичный URL backend API.

PostgreSQL и ML-сервис остаются внутренними сервисами Docker Compose и не открываются напрямую для мобильного приложения.

## Почему телефон не использует localhost

Когда backend запускается локально через Docker Compose, он доступен на компьютере разработчика по адресу:

```text
http://localhost:8000
```

Этот адрес подходит для проверки с того же компьютера.

Но Android-телефон не может использовать `localhost:8000` для подключения к backend на компьютере разработчика. Для телефона `localhost` означает сам телефон, а не компьютер.

Поэтому для тестирования с телефона локальный backend нужно открыть через публичный HTTPS URL. Для этого используется ngrok.

## Запуск backend

Из корня проекта нужно запустить Docker Compose:

```bash
docker compose up --build
```

После запуска нужно проверить, что backend доступен локально:

```bash
curl http://localhost:8000/health
```

Ожидаемый ответ:

```json
{
  "status": "ok"
}
```

## Открытие backend через ngrok

Чтобы Android-приложение могло обращаться к backend с телефона, нужно открыть локальный порт `8000` через ngrok:

```bash
ngrok http 8000
```

После запуска ngrok выдаст публичный HTTPS URL, например:

```text
https://example-id.ngrok-free.app
```

Этот адрес нужно использовать в Android-приложении как базовый URL API.

Пример:

```text
API_BASE_URL=https://example-id.ngrok-free.app
```

Важно: бесплатный ngrok URL обычно меняется после перезапуска ngrok. Если ngrok был перезапущен, новый URL нужно снова передать Android-разработчику или заменить в настройках приложения.

## Базовый URL для Android

Android-приложение должно использовать только URL backend API.

Пример базового URL:

```text
https://example-id.ngrok-free.app
```

Примеры endpoint'ов:

```text
GET  https://example-id.ngrok-free.app/health
GET  https://example-id.ngrok-free.app/api/v1/products
POST https://example-id.ngrok-free.app/api/v1/calculate
POST https://example-id.ngrok-free.app/api/v1/analyze
```

## Проверка endpoint'ов через публичный URL

Во всех командах ниже нужно заменить `<BACKEND_PUBLIC_URL>` на HTTPS URL, который выдал ngrok.

Например:

```text
<BACKEND_PUBLIC_URL> = https://example-id.ngrok-free.app
```

### Проверка health

```bash
curl <BACKEND_PUBLIC_URL>/health
```

Ожидаемый ответ:

```json
{
  "status": "ok"
}
```

### Проверка списка продуктов

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

В реальном ответе массив `products` должен содержать продукты из PostgreSQL.

### Проверка ручного расчёта БЖУ

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/calculate \
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

### Проверка анализа изображения

Endpoint анализа изображения:

```text
POST /api/v1/analyze
```

Android-приложение должно отправлять изображение как `multipart/form-data`.

Название поля с изображением должно быть строго:

```text
image
```

Пример проверки через curl:

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/analyze \
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

Фактические значения `class`, `confidence`, `bbox` и `estimated_volume_cm3` зависят от ответа ML-сервиса.

## Важные требования к Android-запросу

### Формат запроса для анализа фото

Для endpoint'а `/api/v1/analyze` Android должен отправлять:

```text
Content-Type: multipart/form-data
Field name: image
```

Название поля должно быть именно `image`.

Неправильные названия полей, например `file`, `photo` или `picture`, backend не обработает.

### Поддерживаемые форматы изображений

Backend принимает изображения следующих форматов:

```text
jpg
jpeg
png
```

Максимальный размер изображения:

```text
10 MB
```

Если изображение отсутствует, имеет неподдерживаемый формат или слишком большой размер, backend вернёт JSON-ошибку.

## Требования к классам ML-модели

ML-сервис должен возвращать названия классов, которые совпадают с полем `ml_class` в таблице `products`.

Примеры классов:

```text
apple
banana
tomato
potato
sweetpotato
```

Backend ищет продукт в PostgreSQL по полю `ml_class`.

Если ML-сервис вернёт класс, которого нет в базе, backend вернёт ошибку:

```json
{
  "status": "error",
  "error": {
    "code": "PRODUCT_NOT_SUPPORTED",
    "message": "Detected product class is not supported by backend catalog"
  }
}
```

Поэтому список классов ML-модели и seed-данные PostgreSQL должны быть согласованы.

## Доступ к базе данных

Android-приложение не должно подключаться к PostgreSQL напрямую.

Правильно:

```text
Android-приложение -> Go backend -> PostgreSQL
```

Неправильно:

```text
Android-приложение -> PostgreSQL
```

Только Go backend работает с базой данных и ML-сервисом.

## Краткий порядок запуска для интеграции

1. Запустить backend через Docker Compose:

```bash
docker compose up --build
```

2. Проверить backend локально:

```bash
curl http://localhost:8000/health
```

3. Открыть backend через ngrok:

```bash
ngrok http 8000
```

4. Скопировать HTTPS URL, который выдал ngrok.

5. Передать этот URL Android-разработчику как API base URL.

6. Проверить endpoint'ы через публичный URL:

```bash
curl <BACKEND_PUBLIC_URL>/health
curl <BACKEND_PUBLIC_URL>/api/v1/products
```

7. Проверить ручной расчёт:

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple","weight_g":140}'
```

8. Проверить анализ изображения:

```bash
curl -X POST <BACKEND_PUBLIC_URL>/api/v1/analyze \
  -F "image=@test.jpg"
```

## Итог

Для интеграции Android-приложения backend должен быть запущен локально через Docker Compose и открыт наружу через ngrok.

Android-приложение использует только публичный backend URL.

PostgreSQL и ML-сервис остаются внутренними сервисами и не открываются напрямую для телефона.
