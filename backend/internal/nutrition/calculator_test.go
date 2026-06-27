package nutrition

import (
	"testing"

	"github.com/dmgdimas/FoodLens/backend/internal/product"
)

func TestCalculateByWeight(t *testing.T) {
	p := product.Product{
		CaloriesPer100G: 52,
		ProteinsPer100G: 0.3,
		FatsPer100G:     0.2,
		CarbsPer100G:    14,
	}

	result := CalculateByWeight(p, 140)

	if result.Calories != 72.8 {
		t.Fatalf("expected calories 72.8, got %.1f", result.Calories)
	}

	if result.Proteins != 0.4 {
		t.Fatalf("expected proteins 0.4, got %.1f", result.Proteins)
	}

	if result.Fats != 0.3 {
		t.Fatalf("expected fats 0.3, got %.1f", result.Fats)
	}

	if result.Carbs != 19.6 {
		t.Fatalf("expected carbs 19.6, got %.1f", result.Carbs)
	}
}
