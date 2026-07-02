# FoodLens final run checklist

## 1. Requirements

Before running the project, make sure the following tools are installed:

- Docker Desktop
- Docker Compose
- Go
- Android Studio
- ngrok or another public tunnel tool

The ML model file is not stored in GitHub. It must be placed locally:

```text
ml-service/weights/best.pt
2. Start project

From the project root:

make rebuild

This command recreates the PostgreSQL volume, rebuilds Docker images and starts all services.

3. Check backend
make health
make products
make calculate
4. Check image analysis

Place a test image in the project root as test.jpg and run:

make analyze IMAGE=test.jpg

Expected successful response:

{
  "status": "success",
  "detections": []
}

or:

{
  "status": "success",
  "detections": [
    {
      "class": "apple",
      "name_ru": "яблоко",
      "name_en": "apple",
      "confidence": 0.496,
      "estimated_volume_cm3": 150.0,
      "estimated_weight_g": 126.0,
      "nutrients": {
        "calories": 65.5,
        "proteins": 0.4,
        "fats": 0.3,
        "carbs": 17.6
      }
    }
  ]
}
5. Start public tunnel for Android
ngrok http 8000

Use the generated HTTPS URL as Android API base URL.

Example:

https://example.ngrok-free.dev
6. Android check

In the Android application:

Open settings.
Set backend base URL.
Check backend connection.
Open camera screen.
Take or choose a food image.
Send the image for analysis.
Check the result screen.
7. Expected scenarios

If the model detects food, the application shows nutrition result.

If the model detects nothing, backend returns:

{
  "status": "success",
  "detections": []
}

The Android application should show a message that the product was not found.

8. Services

Default local addresses:

Backend:    http://localhost:8000
ML service: http://localhost:9000
PostgreSQL: localhost:5432

Коммит:

```bash
git add docs/final_run_checklist.md
git commit -m "Added final project run checklist"
