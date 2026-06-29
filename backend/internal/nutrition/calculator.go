package nutrition

import "github.com/dmgdimas/FoodLens/backend/internal/product"

type Nutrients struct {
	Calories float64 `json:"calories"`
	Proteins float64 `json:"proteins"`
	Fats     float64 `json:"fats"`
	Carbs    float64 `json:"carbs"`
}

func EstimateWeightByVolume(volumeCM3 float64, densityGPerCM3 float64) float64 {
	return roundToOneDecimal(volumeCM3 * densityGPerCM3)
}

func CalculateByWeight(p product.Product, weightG float64) Nutrients {
	ratio := weightG / 100

	return Nutrients{
		Calories: roundToOneDecimal(ratio * p.CaloriesPer100G),
		Proteins: roundToOneDecimal(ratio * p.ProteinsPer100G),
		Fats:     roundToOneDecimal(ratio * p.FatsPer100G),
		Carbs:    roundToOneDecimal(ratio * p.CarbsPer100G),
	}
}

func roundToOneDecimal(value float64) float64 {
	return float64(int(value*10+0.5)) / 10
}
