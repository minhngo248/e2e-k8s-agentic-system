package models

import (
	"github.com/google/uuid"
)

type Destination struct {
	ID          uuid.UUID     `json:"uuid" db:"id"`
	Name        string  `json:"name" db:"name"`
	Description string  `json:"description" db:"description"`
	Types       []string `json:"types" db:"types"`
	DetailAddress string `json:"detailAddress" db:"detail_address"`
	City          string `json:"city" db:"city"`
	PostalCode    string `json:"postalCode" db:"postal_code"`
	Region        string `json:"region" db:"region"`
	Country       string `json:"country" db:"country"`
	CountryCode   string `json:"countryCode" db:"country_code"`
	Latitude    float64 `json:"latitude" db:"latitude"`
	Longitude   float64 `json:"longitude" db:"longitude"`
	UrlImages   []string `json:"urlImages" db:"url_images"`
	CreatedAt   string  `json:"createdAt" db:"created_at"`
	UpdatedAt   string  `json:"updatedAt" db:"updated_at"`
}

type DestinationType string

const (
	Beach    DestinationType = "beach"
	Mountain DestinationType = "mountain"
	City     DestinationType = "city"
	Historical DestinationType = "historical"
	Cultural  DestinationType = "cultural"
	Adventure DestinationType = "adventure"
	Nature    DestinationType = "nature"
	Relaxation DestinationType = "relaxation"
	Romantic   DestinationType = "romantic"
	Family     DestinationType = "family"
	Luxury     DestinationType = "luxury"
	Budget     DestinationType = "budget"
	Rural      DestinationType = "rural"
	Festival   DestinationType = "festival"
	Wildlife   DestinationType = "wildlife"
	Nightlife  DestinationType = "nightlife"
	Foodie     DestinationType = "foodie"
	Desert     DestinationType = "desert"
	Island     DestinationType = "island"
	WinterSports DestinationType = "winter_sports"
	Pilgrimage  DestinationType = "pilgrimage"
	EcoTourism   DestinationType = "eco_tourism"
	Cruise       DestinationType = "cruise"
	School       DestinationType = "school"
	Indoor       DestinationType = "indoor"
	Outdoor      DestinationType = "outdoor"
)

type DestinationOutput struct {
	ID          string   `json:"id"`
	Name        string   `json:"name"`
	Description string   `json:"description"`
	Types       []string `json:"types"`
	DetailAddress string `json:"detailAddress"`
	City        string   `json:"city"`
	PostalCode  string   `json:"postalCode"`
	Region      string   `json:"region"`
	Country     string   `json:"country"`
	CountryCode string   `json:"countryCode"`
	Latitude    float64  `json:"latitude"`
	Longitude   float64  `json:"longitude"`
	UrlImages   []string `json:"urlImages"`
}