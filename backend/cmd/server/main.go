package main

import (
	"log/slog"
	"os"

	"github.com/dmgdimas/FoodLens/backend/internal/app"
	"github.com/dmgdimas/FoodLens/backend/internal/config"
	"github.com/dmgdimas/FoodLens/backend/internal/logger"
)

func main() {
	cfg := config.Load()
	log := logger.New(slog.LevelInfo, os.Stdout)

	application := app.New(cfg, log)

	if err := application.Run(); err != nil {
		log.Error("failed to run application", "error", err)
		os.Exit(1)
	}
}
