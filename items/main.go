package main

import (
	"log"
	"os"

	"github.com/labstack/echo/v4"
	controller "items/controllers"
	"items/resources/items"
)

const defaultPort = ":8080"
const itemsPath = "./data/items.json"
const categoriesPath = "./data/categories.json"

func main() {
	// Initialize Echo
	e := echo.New()

	// Load item resource
	itemsResource := items.NewResource(itemsPath, categoriesPath)
	log.Printf("✅ Item resource loaded from %s", itemsPath)

	// Initialize controller with the resource
	itemController := controller.NewItemController(itemsResource)

	// Register routes
	e.GET("/items", itemController.GetItemsHandler)

	// Register routes
	e.GET("/categories", itemController.GetCategoriesHandler)

	// Start server
	port := getPort()
	log.Printf("🚀 Starting server on %s", port)
	e.Logger.Fatal(e.Start(port))
}

// getPort allows overriding the default port with an environment variable.
func getPort() string {
	if port := os.Getenv("PORT"); port != "" {
		return ":" + port
	}
	return defaultPort
}
