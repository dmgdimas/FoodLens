package ml

import (
	"encoding/json"
	"testing"
)

func TestAnalyzeResponseUnmarshal(t *testing.T) {
	payload := []byte(`{
		"success": true,
		"predictions": [
			{
				"class": "apple",
				"confidence": 0.496,
				"volume_cm3": 150.0
			}
		]
	}`)

	var response AnalyzeResponse

	if err := json.Unmarshal(payload, &response); err != nil {
		t.Fatalf("failed to unmarshal analyze response: %v", err)
	}

	if !response.Success {
		t.Fatal("expected success to be true")
	}

	if len(response.Predictions) != 1 {
		t.Fatalf("expected 1 prediction, got %d", len(response.Predictions))
	}

	prediction := response.Predictions[0]

	if prediction.Class != "apple" {
		t.Fatalf("expected class apple, got %s", prediction.Class)
	}

	if prediction.Confidence != 0.496 {
		t.Fatalf("expected confidence 0.496, got %f", prediction.Confidence)
	}

	if prediction.VolumeCM3 != 150.0 {
		t.Fatalf("expected volume 150.0, got %f", prediction.VolumeCM3)
	}
}

func TestAnalyzeResponseEmptyPredictions(t *testing.T) {
	payload := []byte(`{
		"success": true,
		"predictions": []
	}`)

	var response AnalyzeResponse

	if err := json.Unmarshal(payload, &response); err != nil {
		t.Fatalf("failed to unmarshal analyze response: %v", err)
	}

	if !response.Success {
		t.Fatal("expected success to be true")
	}

	if len(response.Predictions) != 0 {
		t.Fatalf("expected empty predictions, got %d", len(response.Predictions))
	}
}
