package ml

type AnalyzeResponse struct {
	Status       string      `json:"status"`
	ModelName    string      `json:"model_name"`
	ModelVersion string      `json:"model_version"`
	Detections   []Detection `json:"detections"`
}

type Detection struct {
	Class              string  `json:"class"`
	Confidence         float64 `json:"confidence"`
	BBox               BBox    `json:"bbox"`
	EstimatedVolumeCM3 float64 `json:"estimated_volume_cm3"`
}

type BBox struct {
	X      int `json:"x"`
	Y      int `json:"y"`
	Width  int `json:"width"`
	Height int `json:"height"`
}
