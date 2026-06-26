package product

type Product struct {
	ID              int64    `json:"id"`
	MLClass         string   `json:"ml_class"`
	NameRU          string   `json:"name_ru"`
	NameEN          string   `json:"name_en"`
	Aliases         []string `json:"aliases"`
	DensityGPerCM3  float64  `json:"density_g_per_cm3"`
	CaloriesPer100G float64  `json:"calories_per_100g"`
	ProteinsPer100G float64  `json:"proteins_per_100g"`
	FatsPer100G     float64  `json:"fats_per_100g"`
	CarbsPer100G    float64  `json:"carbs_per_100g"`
	IsSupported     bool     `json:"is_supported"`
}
