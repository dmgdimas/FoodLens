CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,

    ml_class VARCHAR(120) UNIQUE NOT NULL,
    name_ru VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    aliases TEXT[] DEFAULT ARRAY[]::TEXT[],

    density_g_per_cm3 NUMERIC(8,4) NOT NULL CHECK (density_g_per_cm3 > 0),
    calories_per_100g NUMERIC(8,2) NOT NULL CHECK (calories_per_100g >= 0),
    proteins_per_100g NUMERIC(8,2) NOT NULL CHECK (proteins_per_100g >= 0),
    fats_per_100g NUMERIC(8,2) NOT NULL CHECK (fats_per_100g >= 0),
    carbs_per_100g NUMERIC(8,2) NOT NULL CHECK (carbs_per_100g >= 0),

    source_nutrition TEXT,
    source_density TEXT,

    is_supported BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_products_ml_class ON products (ml_class);
CREATE INDEX IF NOT EXISTS idx_products_is_supported ON products (is_supported);
CREATE INDEX IF NOT EXISTS idx_products_name_ru ON products (name_ru);
CREATE INDEX IF NOT EXISTS idx_products_name_en ON products (name_en);
