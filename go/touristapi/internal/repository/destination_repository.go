package repository

import (
	"database/sql"
	"minhnn.fr/touristapi/internal/models"
)

type DestinationRepository struct {
	db *sql.DB
}

func NewDestinationRepository(db *sql.DB) *DestinationRepository {
	return &DestinationRepository{db: db}
}

func (repository *DestinationRepository) FindByTypesAndCoordinates(types []string, latitude float64, longitude float64) ([]models.Destination, error) {
	query := "SELECT * FROM destinations WHERE "
	params := make([]interface{}, 0)

	if len(types) > 0 {
		query += "types @> ?::jsonb "
		params = append(params, convertTypesToJsonString(types))
	}

	query += " AND (6371 * acos(cos(radians(?)) * cos(radians(latitude)) * " +
		"cos(radians(longitude) - radians(?)) + sin(radians(?)) * sin(radians(latitude)))) <= ?"
	params = append(params, latitude, longitude)
}

func convertTypesToJsonString(types []string) string {
	jsonString := "["
	for i, t := range types {
		jsonString += "\"" + t + "\""
		if i < len(types)-1 {
			jsonString += ", "
		}
	}
	jsonString += "]"
	return jsonString
}