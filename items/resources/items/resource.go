package items

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
)

// Resource provides in‑memory access and indexes for items and categories.
// Besides the raw maps loaded from JSON, it maintains an index for fast
// resolution item_id → root_category_id.
type Resource struct {
	items              map[string]Item
	categories         map[string]Category
	itemToRootCategory map[string]string // itemID → rootCategoryID
}

// NewResource loads items & categories from disk and builds the
// item→rootCategory index so look‑ups are O(1).
func NewResource(itemJSONPath, categoryJSONPath string) *Resource {
	items := loadItems(itemJSONPath)
	categories := loadCategories(categoryJSONPath)

	return &Resource{
		items:              items,
		categories:         categories,
		itemToRootCategory: buildItemToRootCategoryIndex(items, categories),
	}
}

// loadItems reads an items JSON file and unmarshals it into a map.
func loadItems(path string) map[string]Item {
	file, err := os.Open(path)
	if err != nil {
		panic(fmt.Sprintf("failed to open items JSON file: %v", err))
	}
	defer file.Close()

	bytes, err := io.ReadAll(file)
	if err != nil {
		panic(fmt.Sprintf("failed to read items JSON file: %v", err))
	}

	var data map[string]Item
	if err := json.Unmarshal(bytes, &data); err != nil {
		panic(fmt.Sprintf("failed to unmarshal items JSON: %v", err))
	}

	// Copy the map key into the struct so downstream code has the ID field.
	for id, itm := range data {
		itm.ID = id
		data[id] = itm
	}

	return data
}

// loadCategories reads a categories JSON file and unmarshals it into a map.
func loadCategories(path string) map[string]Category {
	file, err := os.Open(path)
	if err != nil {
		panic(fmt.Sprintf("failed to open categories JSON file: %v", err))
	}
	defer file.Close()

	bytes, err := io.ReadAll(file)
	if err != nil {
		panic(fmt.Sprintf("failed to read categories JSON file: %v", err))
	}

	var data map[string]Category
	if err := json.Unmarshal(bytes, &data); err != nil {
		panic(fmt.Sprintf("failed to unmarshal categories JSON: %v", err))
	}

	for id, cat := range data {
		cat.ID = id
		data[id] = cat
	}

	return data
}

// buildItemToRootCategoryIndex precomputes itemID → rootCategoryID so future
// look‑ups are O(1).
func buildItemToRootCategoryIndex(items map[string]Item, categories map[string]Category) map[string]string {
	idx := make(map[string]string, len(items))

	for id, itm := range items {
		cat, ok := categories[itm.CategoryID]
		if !ok {
			// The item references an unknown category; skip (or handle as needed)
			continue
		}

		rootID := cat.ID
		if len(cat.PathFromRoot) > 0 {
			rootID = cat.PathFromRoot[0].ID
		}
		idx[id] = rootID
	}

	return idx
}

// GetItemsByIDs returns the items matching the provided IDs.
// If none are found, ErrItemNotFound is returned.
func (r *Resource) GetItemsByIDs(ids []string) ([]Item, error) {
	result := make([]Item, 0, len(ids))
	for _, id := range ids {
		if itm, ok := r.items[id]; ok {
			result = append(result, itm)
		}
	}
	if len(result) == 0 {
		return nil, ErrItemNotFound
	}
	return result, nil
}

// GetCategoriesByIDs returns the categories matching the provided IDs.
// If none are found, ErrCategoryNotFound is returned.
func (r *Resource) GetCategoriesByIDs(ids []string) ([]Category, error) {
	result := make([]Category, 0, len(ids))
	for _, id := range ids {
		if cat, ok := r.categories[id]; ok {
			result = append(result, cat)
		}
	}
	if len(result) == 0 {
		return nil, ErrCategoryNotFound
	}
	return result, nil
}

// GroupItemIDsByRootCategory groups a slice of item IDs by the root category
// (first element of path_from_root) for fast responses like:
//
//	[{"root_category_id":"MLA1000","item_ids":["MLA1","MLA2"]}, …]
//
// It silently ignores unknown item IDs but returns ErrItemNotFound if *all*
// requested IDs are unknown. The caller can choose how to handle partial misses.
func (r *Resource) GroupItemIDsByRootCategory(ids []string) ([]CategoryGroup, error) {
	if len(ids) == 0 {
		return nil, ErrItemNotFound
	}

	// rootCatID → []itemID
	tmp := make(map[string][]string)

	for _, id := range ids {
		if root, ok := r.itemToRootCategory[id]; ok {
			tmp[root] = append(tmp[root], id)
		}
	}

	if len(tmp) == 0 {
		return nil, ErrItemNotFound
	}

	groups := make([]CategoryGroup, 0, len(tmp))
	for root, items := range tmp {
		groups = append(groups, CategoryGroup{
			RootCategoryID: root,
			ItemIDs:        items,
		})
	}

	return groups, nil
}
