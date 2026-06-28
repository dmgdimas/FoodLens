package httpserver

import (
	"encoding/json"
	"errors"
	"net/http"
	"strings"

	"github.com/dmgdimas/FoodLens/backend/internal/nutrition"
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

type CalculateRequest struct {
	MLClass string   `json:"ml_class"`
	WeightG *float64 `json:"weight_g"`
}

type CalculateResponse struct {
	Status           string              `json:"status"`
	Product          ProductShort        `json:"product"`
	EstimatedWeightG float64             `json:"estimated_weight_g"`
	Nutrients        nutrition.Nutrients `json:"nutrients"`
}

type ProductShort struct {
	MLClass string `json:"ml_class"`
	NameRU  string `json:"name_ru"`
	NameEN  string `json:"name_en"`
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

func (h *Handler) calculateHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.Header().Set("Allow", http.MethodPost)
		writeError(w, http.StatusMethodNotAllowed, "METHOD_NOT_ALLOWED", "Method is not allowed")
		return
	}

	var request CalculateRequest

	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()

	if err := decoder.Decode(&request); err != nil {
		writeError(w, http.StatusBadRequest, "INVALID_JSON", "Request body must be valid JSON")
		return
	}

	request.MLClass = strings.TrimSpace(request.MLClass)

	if validationError := validateCalculateRequest(request); validationError != nil {
		writeError(w, http.StatusBadRequest, validationError.Code, validationError.Message)
		return
	}

	foundProduct, err := h.products.GetByMLClass(r.Context(), request.MLClass)
	if err != nil {
		if errors.Is(err, product.ErrNotFound) {
			writeError(w, http.StatusNotFound, "PRODUCT_NOT_SUPPORTED", "Product is not supported by backend catalog")
			return
		}

		h.log.Error("failed to get product by ml_class", "ml_class", request.MLClass, "error", err)
		writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to calculate nutrients")
		return
	}

	nutrients := nutrition.CalculateByWeight(foundProduct, *request.WeightG)

	writeJSON(w, http.StatusOK, CalculateResponse{
		Status: "success",
		Product: ProductShort{
			MLClass: foundProduct.MLClass,
			NameRU:  foundProduct.NameRU,
			NameEN:  foundProduct.NameEN,
		},
		EstimatedWeightG: *request.WeightG,
		Nutrients:        nutrients,
	})
}

type ValidationError struct {
	Code    string
	Message string
}

func validateCalculateRequest(request CalculateRequest) *ValidationError {
	if request.MLClass == "" {
		return &ValidationError{
			Code:    "INVALID_INPUT",
			Message: "ml_class is required",
		}
	}

	if request.WeightG == nil {
		return &ValidationError{
			Code:    "INVALID_INPUT",
			Message: "weight_g is required",
		}
	}

	if *request.WeightG <= 0 {
		return &ValidationError{
			Code:    "INVALID_INPUT",
			Message: "weight_g must be greater than zero",
		}
	}

	return nil
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
