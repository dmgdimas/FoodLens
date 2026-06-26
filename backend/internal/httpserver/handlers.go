package httpserver

import (
	"net/http"
	"strings"

	"github.com/dmgdimas/FoodLens/backend/internal/product"
)

type ProductsResponse struct {
	Status   string            `json:"status"`
	Products []ProductResponse `json:"products"`
}

type ProductResponse struct {
	ID          int64  `json:"id"`
	MLClass     string `json:"ml_class"`
	NameRU      string `json:"name_ru"`
	NameEN      string `json:"name_en"`
	IsSupported bool   `json:"is_supported"`
}

func (h *Handler) healthHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		w.Header().Set("Allow", http.MethodGet)
		writeError(w, http.StatusMethodNotAllowed, "METHOD_NOT_ALLOWED", "Method is not allowed")
		return
	}

	writeJSON(w, http.StatusOK, HealthResponse{
		Status: "ok",
	})
}

func (h *Handler) productsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		w.Header().Set("Allow", http.MethodGet)
		writeError(w, http.StatusMethodNotAllowed, "METHOD_NOT_ALLOWED", "Method is not allowed")
		return
	}

	supportedOnly, ok := parseSupportedOnlyQuery(r)
	if !ok {
		writeError(w, http.StatusBadRequest, "INVALID_QUERY_PARAM", "supported_only must be true or false")
		return
	}

	products, err := h.products.GetAll(r.Context(), supportedOnly)
	if err != nil {
		h.log.Error("failed to get products", "error", err)
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to get products")
		return
	}

	writeJSON(w, http.StatusOK, ProductsResponse{
		Status:   "success",
		Products: toProductResponses(products),
	})
}

func parseSupportedOnlyQuery(r *http.Request) (bool, bool) {
	value := strings.ToLower(r.URL.Query().Get("supported_only"))

	switch value {
	case "", "false":
		return false, true
	case "true":
		return true, true
	default:
		return false, false
	}
}

func toProductResponses(products []product.Product) []ProductResponse {
	response := make([]ProductResponse, 0, len(products))

	for _, item := range products {
		response = append(response, ProductResponse{
			ID:          item.ID,
			MLClass:     item.MLClass,
			NameRU:      item.NameRU,
			NameEN:      item.NameEN,
			IsSupported: item.IsSupported,
		})
	}

	return response
}
