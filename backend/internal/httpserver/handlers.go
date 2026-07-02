package httpserver

import (
	"encoding/json"
	"errors"
	"net/http"
	"path/filepath"
	"strings"

	"github.com/dmgdimas/FoodLens/backend/internal/nutrition"
	"github.com/dmgdimas/FoodLens/backend/internal/product"
)

const maxImageSizeBytes = 10 << 20

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

type AnalyzeResponse struct {
	Status     string              `json:"status"`
	Detections []DetectionResponse `json:"detections"`
}

type DetectionResponse struct {
	Class              string              `json:"class"`
	NameRU             string              `json:"name_ru"`
	NameEN             string              `json:"name_en"`
	Confidence         float64             `json:"confidence"`
	EstimatedVolumeCM3 float64             `json:"estimated_volume_cm3"`
	EstimatedWeightG   float64             `json:"estimated_weight_g"`
	Nutrients          nutrition.Nutrients `json:"nutrients"`
}

type ValidationError struct {
	Code    string
	Message string
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

func (h *Handler) analyzeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		w.Header().Set("Allow", http.MethodPost)
		writeError(w, http.StatusMethodNotAllowed, "METHOD_NOT_ALLOWED", "Method is not allowed")
		return
	}

	r.Body = http.MaxBytesReader(w, r.Body, maxImageSizeBytes)

	if err := r.ParseMultipartForm(maxImageSizeBytes); err != nil {
		writeError(w, http.StatusBadRequest, "INVALID_MULTIPART_FORM", "Failed to parse multipart form")
		return
	}

	file, fileHeader, err := r.FormFile("image")
	if err != nil {
		writeError(w, http.StatusBadRequest, "IMAGE_REQUIRED", "Image file is required")
		return
	}
	defer file.Close()

	if validationError := validateImageFile(fileHeader.Filename, fileHeader.Size); validationError != nil {
		writeError(w, http.StatusBadRequest, validationError.Code, validationError.Message)
		return
	}

	mlResponse, err := h.mlClient.AnalyzeImage(r.Context(), file, fileHeader.Filename)
	if err != nil {
		h.log.Error("failed to analyze image with ML service", "error", err)
		writeError(w, http.StatusBadGateway, "ML_SERVICE_UNAVAILABLE", "Failed to analyze image")
		return
	}

	if !mlResponse.Success {
		writeError(w, http.StatusBadGateway, "INVALID_ML_RESPONSE", "ML service returned unsuccessful response")
		return
	}

	if len(mlResponse.Predictions) == 0 {
		writeJSON(w, http.StatusOK, AnalyzeResponse{
			Status:     "success",
			Detections: []DetectionResponse{},
		})
		return
	}

	detections := make([]DetectionResponse, 0, len(mlResponse.Predictions))

	for _, prediction := range mlResponse.Predictions {
		mlClass := strings.ToLower(strings.TrimSpace(prediction.Class))
		if mlClass == "" {
			writeError(w, http.StatusBadGateway, "INVALID_ML_RESPONSE", "ML service returned empty class")
			return
		}

		if prediction.VolumeCM3 <= 0 {
			writeError(w, http.StatusBadGateway, "INVALID_ML_RESPONSE", "ML service returned invalid volume")
			return
		}

		productItem, err := h.products.GetByMLClass(r.Context(), mlClass)
		if err != nil {
			if errors.Is(err, product.ErrNotFound) {
				writeError(w, http.StatusNotFound, "PRODUCT_NOT_SUPPORTED", "Product is not supported by backend catalog")
				return
			}

			h.log.Error("failed to get product by ML class", "error", err, "ml_class", mlClass)
			writeError(w, http.StatusInternalServerError, "INTERNAL_ERROR", "Failed to get product")
			return
		}

		estimatedWeightG := nutrition.EstimateWeightByVolume(
			prediction.VolumeCM3,
			productItem.DensityGPerCM3,
		)

		nutrients := nutrition.CalculateByWeight(productItem, estimatedWeightG)

		detections = append(detections, DetectionResponse{
			Class:              productItem.MLClass,
			NameRU:             productItem.NameRU,
			NameEN:             productItem.NameEN,
			Confidence:         prediction.Confidence,
			EstimatedVolumeCM3: prediction.VolumeCM3,
			EstimatedWeightG:   estimatedWeightG,
			Nutrients:          nutrients,
		})
	}

	writeJSON(w, http.StatusOK, AnalyzeResponse{
		Status:     "success",
		Detections: detections,
	})
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

func validateImageFile(filename string, size int64) *ValidationError {
	if size <= 0 {
		return &ValidationError{
			Code:    "IMAGE_REQUIRED",
			Message: "Image file is empty",
		}
	}

	if size > maxImageSizeBytes {
		return &ValidationError{
			Code:    "IMAGE_TOO_LARGE",
			Message: "Image size must be less than or equal to 10 MB",
		}
	}

	extension := strings.ToLower(filepath.Ext(filename))

	switch extension {
	case ".jpg", ".jpeg", ".png":
		return nil
	default:
		return &ValidationError{
			Code:    "INVALID_IMAGE_FORMAT",
			Message: "Only jpg, jpeg and png images are supported",
		}
	}
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
