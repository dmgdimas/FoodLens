package ml

type AnalyzeResponse struct {
	Class      string  `json:"class"`
	Confidence float64 `json:"confidence"`
	VolumeCM3  float64 `json:"volume_cm3"`
}
