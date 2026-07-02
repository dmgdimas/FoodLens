# FoodLens

FoodLens — мобильное приложение для расчёта калорийности и пищевой ценности продуктов по фотографии.

Проект разрабатывается в рамках технологической практики. Основная идея приложения — позволить пользователю сделать фотографию продукта, отправить изображение на сервер, получить результат распознавания продукта и рассчитать примерную калорийность, вес и БЖУ.

## Основной функционал

* отправка фотографии продукта из Android-приложения;
* анализ изображения через ML-сервис;
* определение класса продукта;
* расчёт примерного объёма и веса продукта;
* расчёт калорийности и БЖУ;
* получение списка поддерживаемых продуктов;
* ручной расчёт пищевой ценности по классу продукта и весу;
* хранение справочника продуктов в PostgreSQL;
* запуск backend, ML-сервиса и базы данных через Docker Compose.

## Архитектура проекта

Проект состоит из нескольких основных частей:

```text
Android app
    |
    v
Go backend
    |
    |---- PostgreSQL
    |
    v
FastAPI ML-service
```

Android-приложение не подключается напрямую к базе данных или ML-сервису. Все запросы идут через Go backend.

## Структура проекта

```text
FoodLens/
├── android/        # Android-приложение
├── backend/        # Go backend API
├── db/             # SQL init-скрипты PostgreSQL
├── docs/           # Документация проекта
├── ml-service/     # FastAPI ML-сервис
├── docker-compose.yml
└── README.md
```

## Backend

Backend написан на Go и отвечает за:

* обработку HTTP-запросов от Android-приложения;
* подключение к PostgreSQL;
* получение списка продуктов;
* ручной расчёт калорийности по весу;
* отправку изображения в ML-сервис;
* обработку ответа ML-сервиса;
* расчёт веса, калорийности и БЖУ;
* возврат результата в Android-приложение.

Основные endpoint’ы:

```text
GET  /health
GET  /api/v1/products
POST /api/v1/calculate
POST /api/v1/analyze
```

## ML-service

ML-сервис написан на Python с использованием FastAPI.

Он принимает изображение, выполняет распознавание продукта и возвращает результат в формате:

```json
{
  "success": true,
  "predictions": [
    {
      "class": "apple",
      "confidence": 0.496,
      "volume_cm3": 150.0
    }
  ]
}
```

Файл модели не хранится в GitHub. Для локального запуска его нужно разместить по пути:

```text
ml-service/weights/best.pt
```

## PostgreSQL

PostgreSQL используется для хранения справочника продуктов.

В базе данных хранятся:

* ML-класс продукта;
* русское и английское название;
* aliases для альтернативных названий;
* плотность продукта;
* калорийность;
* белки, жиры и углеводы.

Seed-данные находятся в:

```text
db/init/
```

## Запуск проекта

Для запуска проекта требуется Docker Desktop.

Запуск всех сервисов:

```bash
docker compose up --build
```

Полный перезапуск с пересозданием базы данных:

```bash
docker compose down -v
docker compose up --build
```

После запуска сервисы доступны по адресам:

```text
Backend:    http://localhost:8000
ML-service: http://localhost:9000
PostgreSQL: localhost:5432
```

## Проверка backend

Проверка health endpoint:

```bash
curl http://localhost:8000/health
```

Получение списка продуктов:

```bash
curl http://localhost:8000/api/v1/products
```

Ручной расчёт калорийности:

```bash
curl -X POST http://localhost:8000/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"ml_class":"apple","weight_g":140}'
```

Анализ изображения:

```bash
curl -X POST http://localhost:8000/api/v1/analyze \
  -F "image=@test.jpg"
```

## Android-интеграция

Для проверки Android-приложения на реальном телефоне backend можно открыть через публичный HTTPS URL с помощью ngrok:

```bash
ngrok http 8000
```

Полученный URL нужно указать в Android-приложении как backend base URL.

Пример:

```text
https://example.ngrok-free.dev
```

## Используемые технологии

* Go
* PostgreSQL
* Python
* FastAPI
* YOLO
* Android
* Kotlin
* Docker
* Docker Compose
* ngrok

## Статус проекта

Проект находится на стадии MVP. Реализован основной сценарий:

```text
Android отправляет фото
→ backend принимает изображение
→ backend отправляет изображение в ML-сервис
→ ML-сервис возвращает распознанный продукт
→ backend рассчитывает вес, калории и БЖУ
→ Android отображает результат
```
