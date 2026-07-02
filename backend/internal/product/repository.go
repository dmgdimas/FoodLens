package product

import (
	"context"
	"errors"
	"strings"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

var ErrNotFound = errors.New("product not found")

type Repository struct {
	db *pgxpool.Pool
}

func NewRepository(db *pgxpool.Pool) *Repository {
	return &Repository{
		db: db,
	}
}

func (r *Repository) GetAll(ctx context.Context, supportedOnly bool) ([]Product, error) {
	query := `
		SELECT
			id,
			ml_class,
			name_ru,
			name_en,
			aliases,
			density_g_per_cm3,
			calories_per_100g,
			proteins_per_100g,
			fats_per_100g,
			carbs_per_100g,
			is_supported
		FROM products
	`

	args := make([]any, 0)

	if supportedOnly {
		query += " WHERE is_supported = $1"
		args = append(args, true)
	}

	query += " ORDER BY id"

	rows, err := r.db.Query(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	products := make([]Product, 0)

	for rows.Next() {
		var p Product

		err := rows.Scan(
			&p.ID,
			&p.MLClass,
			&p.NameRU,
			&p.NameEN,
			&p.Aliases,
			&p.DensityGPerCM3,
			&p.CaloriesPer100G,
			&p.ProteinsPer100G,
			&p.FatsPer100G,
			&p.CarbsPer100G,
			&p.IsSupported,
		)
		if err != nil {
			return nil, err
		}

		products = append(products, p)
	}

	if err := rows.Err(); err != nil {
		return nil, err
	}

	return products, nil
}

func (r *Repository) GetByMLClass(ctx context.Context, mlClass string) (Product, error) {
	mlClass = strings.ToLower(strings.TrimSpace(mlClass))

	const query = `
		SELECT
			id,
			ml_class,
			name_ru,
			name_en,
			aliases,
			density_g_per_cm3,
			calories_per_100g,
			proteins_per_100g,
			fats_per_100g,
			carbs_per_100g,
			is_supported
		FROM products
		WHERE is_supported = TRUE
		  AND (
			  ml_class = $1
			  OR $1 = ANY(aliases)
		  )
		LIMIT 1
	`

	var p Product

	err := r.db.QueryRow(ctx, query, mlClass).Scan(
		&p.ID,
		&p.MLClass,
		&p.NameRU,
		&p.NameEN,
		&p.Aliases,
		&p.DensityGPerCM3,
		&p.CaloriesPer100G,
		&p.ProteinsPer100G,
		&p.FatsPer100G,
		&p.CarbsPer100G,
		&p.IsSupported,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return Product{}, ErrNotFound
		}

		return Product{}, err
	}

	return p, nil
}
