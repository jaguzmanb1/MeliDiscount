package com.github.jaguzmanb1.melidiscount.controller;

import com.github.jaguzmanb1.melidiscount.dto.CategoryGroupDTO;
import com.github.jaguzmanb1.melidiscount.service.DiscountService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Public REST façade for the «Meli Discount» use case.
 *
 * <p><b>Endpoints</b></p>
 * <pre>
 *   GET /meli_discount?item_ids=MLA1,MLA2
 *       → maximum non‑overlapping subset of all given items
 *
 *   GET /meli_discount/categories?item_ids=MLA1,MLA2,MLA3
 *       → maximum non‑overlapping subset calculated <em>per root category</em>
 * </pre>
 *
 * <p>Business logic is delegated to {@link DiscountService}. Any infrastructure‑level
 * or mapping exceptions are handled by global {@code @ControllerAdvice} components.</p>
 */
@RestController
@RequestMapping("/meli_discount")
@Validated
public class MeliDiscountController {

    private final DiscountService discountService;

    public MeliDiscountController(@NonNull DiscountService discountService) {
        this.discountService = Objects.requireNonNull(discountService, "discountService must not be null");
    }

    /**
     * Calculates the maximal subset of items whose active periods do <em>not</em> overlap (global scope).
     *
     * @param itemIds comma‑separated list automatically converted to {@link java.util.List} by Spring
     * @return JSON body <code>{ "item_ids": ["MLA1", "MLA3"] }</code>
     */
    @GetMapping
    public ResponseEntity<IdsResponse> calculateDiscount(
            @RequestParam(name = "item_ids") List<String> itemIds) {

        List<String> selected = discountService.findMaxNonOverlappingItems(itemIds);
        return ResponseEntity.ok(new IdsResponse(selected));
    }

    /**
     * Calculates, <em>for each root category</em>, the maximal subset of items whose active periods do not overlap.
     *
     * @param itemIds IDs to evaluate
     * @return list of {@link CategoryGroupDTO} grouped by root category
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryGroupDTO>> calculateDiscountByCategory(
            @RequestParam(name = "item_ids") List<String> itemIds) {

        List<CategoryGroupDTO> groups = discountService.findMaxNonOverlappingItemsByCategory(itemIds);
        return ResponseEntity.ok(groups);
    }

    /** JSON wrapper used when the response body is a single array of item IDs. */
    private record IdsResponse(@com.fasterxml.jackson.annotation.JsonProperty("item_ids") List<String> itemIds) {}
}
