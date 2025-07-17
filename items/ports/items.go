package ports

import (
	"items/resources/items"
)

type ItemsResourcePort interface {
	GetItemsByIDs(ids []string) ([]items.Item, error)
	GetCategoriesByIDs(ids []string) ([]items.Category, error)
	GroupItemIDsByRootCategory(ids []string) ([]items.CategoryGroup, error)
}
