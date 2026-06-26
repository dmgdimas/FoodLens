package app

import (
	"context"
	"errors"
	"log/slog"
	"net/http"
	"time"

	"github.com/dmgdimas/FoodLens/backend/internal/config"
	"github.com/dmgdimas/FoodLens/backend/internal/database"
	"github.com/dmgdimas/FoodLens/backend/internal/httpserver"
	"github.com/dmgdimas/FoodLens/backend/internal/product"
	"github.com/jackc/pgx/v5/pgxpool"
)

type App struct {
	cfg      config.Config
	log      *slog.Logger
	db       *pgxpool.Pool
	products *product.Repository
	server   *http.Server
}

func New(ctx context.Context, cfg config.Config, log *slog.Logger) (*App, error) {
	db, err := database.NewPostgresPool(ctx, cfg.DatabaseDSN())
	if err != nil {
		return nil, err
	}

	productRepository := product.NewRepository(db)

	router := httpserver.NewRouter(log, productRepository)

	server := &http.Server{
		Addr:              cfg.Addr(),
		Handler:           router,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       10 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	return &App{
		cfg:      cfg,
		log:      log,
		db:       db,
		products: productRepository,
		server:   server,
	}, nil
}

func (a *App) Run() error {
	a.log.Info(
		"FoodLens backend started",
		"addr", a.server.Addr,
		"env", a.cfg.AppEnv,
	)

	err := a.server.ListenAndServe()
	if errors.Is(err, http.ErrServerClosed) {
		return nil
	}

	return err
}

func (a *App) Close() {
	if a.db != nil {
		a.db.Close()
		a.log.Info("PostgreSQL connection pool closed")
	}
}
