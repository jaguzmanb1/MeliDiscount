package controller

import (
	"items/ports"
	res "items/resources/items" // alias sólo para abreviar
	"net/http"
	"strings"

	"github.com/labstack/echo/v4"
)

// ItemController maneja rutas HTTP relacionadas con ítems y ahora también categorías.
type ItemController struct {
	resource ports.ItemsResourcePort
}

// NewItemController crea un controlador usando el recurso inyectado.
func NewItemController(r ports.ItemsResourcePort) *ItemController {
	return &ItemController{resource: r}
}

// errorResponseMap mapea errores de dominio a códigos y mensajes HTTP.
var errorResponseMap = map[error]struct {
	Code    int
	Message string
}{
	res.ErrItemNotFound: {
		Code:    http.StatusNotFound,
		Message: "No items found for the provided IDs",
	},
	// NUEVO: soporte para categorías
	res.ErrCategoryNotFound: {
		Code:    http.StatusNotFound,
		Message: "No categories found for the provided IDs",
	},
}

// writeError envía una respuesta JSON estandarizada para errores.
func writeError(ctx echo.Context, err error) error {
	if resp, exists := errorResponseMap[err]; exists {
		return ctx.JSON(resp.Code, map[string]string{
			"error": resp.Message,
		})
	}

	return ctx.JSON(http.StatusInternalServerError, map[string]string{
		"error": "Internal server error",
	})
}

// GetItemsHandler maneja GET /items?ids=MLA1,MLA2
func (c *ItemController) GetItemsHandler(ctx echo.Context) error {
	idsParam := ctx.QueryParam("ids")
	if idsParam == "" {
		return ctx.JSON(http.StatusBadRequest, map[string]string{
			"error": "You must provide at least one ID in the 'ids' query parameter",
		})
	}

	ids := strings.Split(idsParam, ",")

	result, err := c.resource.GetItemsByIDs(ids)
	if err != nil {
		return writeError(ctx, err)
	}

	return ctx.JSON(http.StatusOK, result)
}

// GetCategoriesHandler maneja GET /categories?ids=MLA100,MLA200
func (c *ItemController) GetCategoriesHandler(ctx echo.Context) error {
	idsParam := ctx.QueryParam("ids")
	if idsParam == "" {
		return ctx.JSON(http.StatusBadRequest, map[string]string{
			"error": "You must provide at least one ID in the 'ids' query parameter",
		})
	}

	ids := strings.Split(idsParam, ",")

	result, err := c.resource.GroupItemIDsByRootCategory(ids)
	if err != nil {
		return writeError(ctx, err)
	}

	return ctx.JSON(http.StatusOK, result)
}
