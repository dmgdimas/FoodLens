package httpserver

import (
	"log/slog"
	"net/http"

	"github.com/dmgdimas/FoodLens/backend/internal/ml"
	"github.com/dmgdimas/FoodLens/backend/internal/product"
)

type Handler struct {
	log      *slog.Logger
	products *product.Repository
	mlClient *ml.Client
}

func NewRouter(log *slog.Logger, productRepository *product.Repository, mlClient *ml.Client) http.Handler {
	handler := &Handler{
		log:      log,
		products: productRepository,
		mlClient: mlClient,
	}

	mux := http.NewServeMux()

	mux.HandleFunc("/health", handler.healthHandler)
	mux.HandleFunc("/api/v1/products", handler.productsHandler)
	mux.HandleFunc("/api/v1/calculate", handler.calculateHandler)
	mux.HandleFunc("/api/v1/analyze", handler.analyzeHandler)

	return loggingMiddleware(log, mux)
}
