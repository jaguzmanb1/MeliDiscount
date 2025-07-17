package items

import (
	"errors"
)

var (
	ErrItemNotFound     = errors.New("item not found")
	ErrCategoryNotFound = errors.New("category not found")
)
