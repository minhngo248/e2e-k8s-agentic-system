package tools

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"

	"github.com/modelcontextprotocol/go-sdk/mcp"
)

func init() {
	registerTool(WeatherTool())
}

func WeatherTool() MCPTool[WeatherToolParams, ListWeatherToolResult] {
	return MCPTool[WeatherToolParams, ListWeatherToolResult]{
		Name:        "weathertool",
		Description: "Get weather forecast for a location given latitude and longitude coordinates",
		Handler: func(ctx context.Context, cc *mcp.ServerSession, params *mcp.CallToolParamsFor[WeatherToolParams]) (*mcp.CallToolResultFor[ListWeatherToolResult], error) {
			result := runWeatherTool(params.Arguments)

			weatherMessage := fmt.Sprintf("Weather forecast for latitude %.2f and longitude %.2f for the next %d days:\n", params.Arguments.Latitude, params.Arguments.Longitude, params.Arguments.Days)
			for _, forecast := range result.Results {
				weatherMessage += fmt.Sprintf("- Date: %s, Max Temp: %.1f°C, Min Temp: %.1f°C, Weather Code: %d\n",
					forecast.Date, forecast.MaxTemp, forecast.MinTemp, forecast.WeatherCode)
			}
			return &mcp.CallToolResultFor[ListWeatherToolResult]{
				StructuredContent: result,
				Content: []mcp.Content{
					&mcp.TextContent{
						Text: weatherMessage,
					},
				},
			}, nil
		},
	}
}

// define your input/output schemas here
type WeatherToolParams struct {
	Latitude  float64 `json:"latitude" description:"The latitude of the location to get weather for."`
	Longitude float64 `json:"longitude" description:"The longitude of the location to get weather for."`
	Days	 int     `json:"days" description:"The number of days to get the weather forecast for."`
}

type ListWeatherToolResult struct {
	Results []*WeatherToolResult `json:"results" description:"A list of weather forecasts for the specified location and number of days."`
}

type WeatherToolResult struct {
	Date        string  `json:"date" description:"The date of the weather forecast."`
	MaxTemp     float64 `json:"max_temp" description:"The maximum temperature for the day."`
	MinTemp     float64 `json:"min_temp" description:"The minimum temperature for the day."`
	WeatherCode int     `json:"weather_code" description:"The weather code for the day."`
}

// API response structures
type WeatherAPIResponse struct {
	Latitude  float64    `json:"latitude"`
	Longitude float64    `json:"longitude"`
	Daily     DailyData  `json:"daily"`
}

type DailyData struct {
	Time            []string  `json:"time"`
	WeatherCode     []int     `json:"weather_code"`
	Temperature2mMax []float64 `json:"temperature_2m_max"`
	Temperature2mMin []float64 `json:"temperature_2m_min"`
}

func runWeatherTool(args WeatherToolParams) ListWeatherToolResult {
	emptyResult := ListWeatherToolResult{Results: make([]*WeatherToolResult, 0)}

	// Call API Open Meteo
	apiURL := fmt.Sprintf("https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&daily=temperature_2m_max,temperature_2m_min,weather_code&past_days=0&forecast_days=%d",
		args.Latitude, args.Longitude, args.Days)

	// Make HTTP GET request
	resp, err := http.Get(apiURL)
	if err != nil {
		return emptyResult
	}
	defer resp.Body.Close()

	// Read response body
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return emptyResult
	}

	// Deserialize JSON response
	var apiResp WeatherAPIResponse
	if err := json.Unmarshal(body, &apiResp); err != nil {
		return emptyResult
	}

	// Convert API response to WeatherToolResult slice
	results := make([]*WeatherToolResult, 0, len(apiResp.Daily.Time))
	for i := range apiResp.Daily.Time {
		results = append(results, &WeatherToolResult{
			Date:        apiResp.Daily.Time[i],
			MaxTemp:     apiResp.Daily.Temperature2mMax[i],
			MinTemp:     apiResp.Daily.Temperature2mMin[i],
			WeatherCode: apiResp.Daily.WeatherCode[i],
		})
	}

	return ListWeatherToolResult{Results: results}
}
