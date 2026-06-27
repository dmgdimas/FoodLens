package httpserver

import "testing"

func TestValidateCalculateRequest(t *testing.T) {
	validWeight := 140.0

	tests := []struct {
		name        string
		request     CalculateRequest
		wantMessage string
	}{
		{
			name: "valid request",
			request: CalculateRequest{
				MLClass: "Apple",
				WeightG: &validWeight,
			},
			wantMessage: "",
		},
		{
			name: "empty ml class",
			request: CalculateRequest{
				MLClass: "",
				WeightG: &validWeight,
			},
			wantMessage: "ml_class is required",
		},
		{
			name: "missing weight",
			request: CalculateRequest{
				MLClass: "Apple",
				WeightG: nil,
			},
			wantMessage: "weight_g is required",
		},
		{
			name: "zero weight",
			request: CalculateRequest{
				MLClass: "Apple",
				WeightG: float64Pointer(0),
			},
			wantMessage: "weight_g must be greater than zero",
		},
		{
			name: "negative weight",
			request: CalculateRequest{
				MLClass: "Apple",
				WeightG: float64Pointer(-10),
			},
			wantMessage: "weight_g must be greater than zero",
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := validateCalculateRequest(tt.request)

			if tt.wantMessage == "" {
				if err != nil {
					t.Fatalf("expected no validation error, got %v", err)
				}

				return
			}

			if err == nil {
				t.Fatalf("expected validation error")
			}

			if err.Message != tt.wantMessage {
				t.Fatalf("expected message %q, got %q", tt.wantMessage, err.Message)
			}
		})
	}
}

func float64Pointer(value float64) *float64 {
	return &value
}
