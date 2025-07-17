package items

type Item struct {
	SellerID    string  `json:"seller_id"`
	Title       string  `json:"title"`
	CategoryID  string  `json:"category_id"`
	Price       float64 `json:"price"`
	DateCreated string  `json:"date_created"`
	LastUpdated string  `json:"last_updated"`
	ID          string  `json:"id"`
}

type Category struct {
	ID                 string        `json:"-"` // populated from map key
	Name               string        `json:"name"`
	PathFromRoot       []CategoryRef `json:"path_from_root"`
	ChildrenCategories []CategoryRef `json:"children_categories"`
}

type CategoryRef struct {
	ID string `json:"id"`
}

type CategoryGroup struct {
	RootCategoryID string   `json:"root_category_id"`
	ItemIDs        []string `json:"item_ids"`
}
