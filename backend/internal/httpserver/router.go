package httpserver

import (
	"log/slog"
	"net/http"
)

func NewRouter(log *slog.Logger) http.Handler {
	mux := http.NewServeMux()

	mux.HandleFunc("/health", healthHandler)

	return loggingMiddleware(log, mux)
}

func healthHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		w.Header().Set("Allow", http.MethodGet)
		writeJSON(w, http.StatusMethodNotAllowed, map[string]any{
			"status": "error",
			"error": map[string]any{
				"code":    "METHOD_NOT_ALLOWED",
				"message": "Method is not allowed",
			},
		})
		return
	}

	writeJSON(w, http.StatusOK, HealthResponse{
		Status: "ok",
	})
}
