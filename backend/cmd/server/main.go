package main

import (
	"context"
	"log/slog"
	"os"

	"github.com/dmgdimas/FoodLens/backend/internal/app"
	"github.com/dmgdimas/FoodLens/backend/internal/config"
	"github.com/dmgdimas/FoodLens/backend/internal/logger"
)

func main() {
	cfg := config.Load()
	log := logger.New(slog.LevelInfo, os.Stdout)

	ctx := context.Background()

	application, err := app.New(ctx, cfg, log)
	if err != nil {
		log.Error("failed to initialize application", "error", err)
		os.Exit(1)
	}
	defer application.Close()

	if err := application.Run(); err != nil {
		log.Error("failed to run application", "error", err)
		os.Exit(1)
	}
}
