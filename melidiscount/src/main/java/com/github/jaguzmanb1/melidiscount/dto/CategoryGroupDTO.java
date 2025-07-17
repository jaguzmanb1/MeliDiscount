package com.github.jaguzmanb1.melidiscount.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Grupo devuelto por GET /meli_discount/categories */
public record CategoryGroupDTO(
        @JsonProperty("root_category_id") String rootCategoryId,
        @JsonProperty("item_ids")         List<String> itemIds) {}
