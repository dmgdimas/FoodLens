# Backend Database Schema

## Назначение серверной базы данных

В backend-части FoodLens используется одна серверная база данных PostgreSQL. Она хранит справочник продуктов, пищевую ценность на 100 грамм и примерную плотность продукта.

Серверная БД используется только Go backend. Android-приложение не обращается к PostgreSQL напрямую. Локальная история пользовательских запросов хранится на стороне Android-приложения в SQLite/Room и не относится к зоне ответственности backend.

## Решение для MVP

Для MVP используется одна таблица `products`.

Такое решение выбрано по следующим причинам:

- ML-модель распознаёт ограниченный набор классов;
- backend должен быстро сопоставлять класс ML-модели с пищевой ценностью продукта;
- сложная схема из нескольких таблиц для MVP избыточна;
- одну таблицу проще реализовать, наполнить seed-данными и использовать в Go API;
- справочник можно расширить дополнительными записями без изменения backend-логики.

## Таблица products

| Поле | Тип | Назначение |
|---|---|---|
| id | BIGSERIAL PRIMARY KEY | Внутренний идентификатор продукта |
| ml_class | VARCHAR(120) UNIQUE NOT NULL | Класс продукта, который возвращает ML-модель |
| name_ru | VARCHAR(255) NOT NULL | Название продукта на русском языке |
| name_en | VARCHAR(255) NOT NULL | Название продукта на английском языке |
| aliases | TEXT[] DEFAULT '{}' | Дополнительные названия и синонимы |
| density_g_per_cm3 | NUMERIC(8,4) NOT NULL | Примерная плотность продукта в г/см³ |
| calories_per_100g | NUMERIC(8,2) NOT NULL | Калорийность на 100 г |
| proteins_per_100g | NUMERIC(8,2) NOT NULL | Белки на 100 г |
| fats_per_100g | NUMERIC(8,2) NOT NULL | Жиры на 100 г |
| carbs_per_100g | NUMERIC(8,2) NOT NULL | Углеводы на 100 г |
| source_nutrition | TEXT | Источник данных по пищевой ценности |
| source_density | TEXT | Источник данных по плотности |
| is_supported | BOOLEAN DEFAULT TRUE | Поддерживается ли продукт текущей ML-моделью |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | Дата создания записи |
| updated_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | Дата обновления записи |

## SQL-структура

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,

    ml_class VARCHAR(120) UNIQUE NOT NULL,
    name_ru VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    aliases TEXT[] DEFAULT '{}',

    density_g_per_cm3 NUMERIC(8,4) NOT NULL,
    calories_per_100g NUMERIC(8,2) NOT NULL,
    proteins_per_100g NUMERIC(8,2) NOT NULL,
    fats_per_100g NUMERIC(8,2) NOT NULL,
    carbs_per_100g NUMERIC(8,2) NOT NULL,

    source_nutrition TEXT,
    source_density TEXT,

    is_supported BOOLEAN DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT products_density_positive
        CHECK (density_g_per_cm3 > 0),

    CONSTRAINT products_calories_non_negative
        CHECK (calories_per_100g >= 0),

    CONSTRAINT products_proteins_non_negative
        CHECK (proteins_per_100g >= 0),

    CONSTRAINT products_fats_non_negative
        CHECK (fats_per_100g >= 0),

    CONSTRAINT products_carbs_non_negative
        CHECK (carbs_per_100g >= 0)
);
```

## Индексы

```sql
CREATE INDEX idx_products_ml_class
    ON products (ml_class);

CREATE INDEX idx_products_is_supported
    ON products (is_supported);

CREATE INDEX idx_products_name_ru
    ON products (name_ru);
```

Индекс по `ml_class` нужен для быстрого поиска продукта после ответа ML-сервиса. Индекс по `is_supported` нужен для фильтрации продуктов, которые поддерживаются текущей версией модели. Индекс по `name_ru` может использоваться Android-приложением при получении списка продуктов для ручной коррекции.

## Связь с ML-моделью

ML-сервис возвращает класс продукта, например:

```json
{
  "class": "Apple",
  "confidence": 0.92,
  "estimated_volume_cm3": 150.5
}
```

Go backend использует значение `class` для поиска записи в PostgreSQL:

```sql
SELECT *
FROM products
WHERE ml_class = 'Apple'
  AND is_supported = TRUE;
```

После получения записи backend использует плотность, калорийность и БЖУ для расчёта итоговых значений.

## Расчёт веса и пищевой ценности

Расчётный вес:

```text
estimated_weight_g = estimated_volume_cm3 * density_g_per_cm3
```

Расчёт калорийности и БЖУ:

```text
value = estimated_weight_g / 100 * value_per_100g
```

Например, если ML-сервис вернул объём 150 см³, а плотность продукта равна 0.84 г/см³, то вес составит:

```text
150 * 0.84 = 126 г
```

Если калорийность продукта равна 52 ккал на 100 г, итоговая калорийность составит:

```text
126 / 100 * 52 = 65.52 ккал
```

## Пример seed-записи

```sql
INSERT INTO products (
    ml_class,
    name_ru,
    name_en,
    aliases,
    density_g_per_cm3,
    calories_per_100g,
    proteins_per_100g,
    fats_per_100g,
    carbs_per_100g,
    source_nutrition,
    source_density,
    is_supported
) VALUES (
    'Apple',
    'яблоко',
    'apple',
    ARRAY['apples', 'яблоки'],
    0.8400,
    52.00,
    0.30,
    0.20,
    14.00,
    'USDA FoodData Central / open nutrition reference',
    'FAO/INFOODS density reference / estimated value for MVP',
    TRUE
);
```

## Расширяемость

Хотя MVP работает с ограниченным набором классов ML-модели, таблица `products` может быть расширена. Для продуктов, которые пока не поддерживаются текущей моделью, используется `is_supported = false`.

Такой подход позволяет заранее подготовить справочник продуктов, но не усложнять backend-логику.

