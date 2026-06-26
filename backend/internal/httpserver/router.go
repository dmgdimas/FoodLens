package httpserver

import (
	"log/slog"
	"net/http"

	"github.com/dmgdimas/FoodLens/backend/internal/product"
)

type Handler struct {
	log      *slog.Logger
	products *product.Repository
}

func NewRouter(log *slog.Logger, productRepository *product.Repository) http.Handler {
	handler := &Handler{
		log:      log,
		products: productRepository,
	}

	mux := http.NewServeMux()

	mux.HandleFunc("/health", handler.healthHandler)
	mux.HandleFunc("/api/v1/products", handler.productsHandler)

	return loggingMiddleware(log, mux)
}
