package config

import (
	"fmt"
	"os"
)

type Config struct {
	AppEnv       string
	AppPort      string
	DBHost       string
	DBPort       string
	DBUser       string
	DBPassword   string
	DBName       string
	DBSSLMode    string
	MLServiceURL string
}

func LoadConfig() Config {
	return Config{
		AppEnv:       getEnv("APP_ENV", "development"),
		AppPort:      getEnv("APP_PORT", "8000"),
		DBHost:       getEnv("DB_HOST", "localhost"),
		DBPort:       getEnv("DB_PORT", "5432"),
		DBUser:       getEnv("DB_USER", "foodlens"),
		DBPassword:   getEnv("DB_PASSWORD", "foodlens_password"),
		DBName:       getEnv("DB_NAME", "foodlens"),
		DBSSLMode:    getEnv("DB_SSLMODE", "disable"),
		MLServiceURL: getEnv("ML_SERVICE_URL", "http://localhost:9000"),
	}
}

func (c Config) DatabaseDSN() string {
	return fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=%s",
		c.DBHost,
		c.DBPort,
		c.DBUser,
		c.DBPassword,
		c.DBName,
		c.DBSSLMode,
	)
}

func getEnv(key string, defaultValue string) string {
	value := os.Getenv(key)
	if value == "" {
		return defaultValue
	}

	return value
}
