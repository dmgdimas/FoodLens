package main

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"os"

	"github.com/dmgdimas/FoodLens/backend/internal/config"
	"github.com/dmgdimas/FoodLens/backend/internal/logger"
)

type HealthResponse struct {
	Status string `json:"status"`
}

func main() {
	cfg := config.LoadConfig()

	log := logger.New(slog.LevelInfo, os.Stdout)

	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_ = json.NewEncoder(w).Encode(HealthResponse{Status: "ok"})
	})

	log.Info("FoodLens backend started", "port", cfg.AppPort, "env", cfg.AppEnv)

	if err := http.ListenAndServe(":"+cfg.AppPort, nil); err != nil {
		log.Error("failed to start server", "error", err)
		os.Exit(1)
	}
}
