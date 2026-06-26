package app

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/dmgdimas/FoodLens/backend/internal/config"
	"github.com/dmgdimas/FoodLens/backend/internal/httpserver"
)

type App struct {
	cfg    config.Config
	log    *slog.Logger
	server *http.Server
}

func New(cfg config.Config, log *slog.Logger) *App {
	router := httpserver.NewRouter(log)

	server := &http.Server{
		Addr:              cfg.Addr(),
		Handler:           router,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	return &App{
		cfg:    cfg,
		log:    log,
		server: server,
	}
}

func (a *App) Run() error {
	a.log.Info(
		"FoodLens backend started",
		"addr", a.server.Addr,
		"env", a.cfg.AppEnv,
	)

	return a.server.ListenAndServe()
}
