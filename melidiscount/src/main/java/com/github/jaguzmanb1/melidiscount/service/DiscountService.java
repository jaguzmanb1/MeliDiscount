package com.github.jaguzmanb1.melidiscount.service;

import com.github.jaguzmanb1.melidiscount.dto.CategoryGroupDTO;
import com.github.jaguzmanb1.melidiscount.dto.ItemDTO;
import com.github.jaguzmanb1.melidiscount.resource.ItemsResourceClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service responsible for applying the «Meli Discount» rule set.
 *
 * <p><b>Primary responsibility:</b> Retrieve item metadata and compute the maximum
 * subset of items whose active periods do not overlap (<em>interval‑scheduling greedy algorithm</em>).</p>
 *
 * <p><b>Design principles</b></p>
 * <ul>
 *   <li><b>S – Single Responsibility:</b> Business logic only. HTTP concerns are delegated to {@link ItemsResourceClient}.</li>
 *   <li><b>D – Dependency Inversion:</b> Depends directly on the abstraction exposed by {@code ItemsResourceClient}.</li>
 *   <li>Minimal framework annotations keep the class easily unit‑testable.</li>
 * </ul>
 */
@Service
public class DiscountService {

    private final ItemsResourceClient itemsClient;

    public DiscountService(@NonNull ItemsResourceClient itemsClient) {
        this.itemsClient = Objects.requireNonNull(itemsClient, "itemsClient must not be null");
    }

    /**
     * Returns the maximum set of item IDs whose active periods are pairwise non‑overlapping.
     *
     * @param itemIds raw item IDs (an empty or {@code null} collection yields an empty result)
     * @return an immutable list of selected IDs, never {@code null}
     */
    @Cacheable(value = "discounts", keyGenerator = "sortedIdsKeyGenerator")
    public List<String> findMaxNonOverlappingItems(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        List<ItemDTO> items = itemsClient.fetchItemsByIds(itemIds);
        if (items.isEmpty()) {
            return List.of();
        }

        var sortedIntervals = items.stream()
                .map(ItemInterval::of)
                .sorted(Comparator.comparing(ItemInterval::end))
                .toList();

        List<String> selectedIds = new ArrayList<>();
        OffsetDateTime lastEnd = null;

        for (ItemInterval interval : sortedIntervals) {
            if (lastEnd == null || !lastEnd.isAfter(interval.start())) {
                selectedIds.add(interval.itemId());
                lastEnd = interval.end();
            }
        }
        return List.copyOf(selectedIds);
    }

    /**
     * For each root category, returns the maximum subset of items whose active periods do not overlap.
     *
     * @param itemIds raw item IDs (an empty or {@code null} collection yields an empty result)
     * @return a list of {@link CategoryGroupDTO} as required by the public contract, never {@code null}
     */
    @Cacheable(value = "discountsByCategory", keyGenerator = "sortedIdsKeyGenerator")
    public List<CategoryGroupDTO> findMaxNonOverlappingItemsByCategory(List<String> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return List.of();
        }

        /* 1) Group IDs by root category. */
        List<CategoryGroupDTO> rawGroups = itemsClient.groupByRootCategory(itemIds);
        if (rawGroups.isEmpty()) {
            return List.of();
        }

        /* 2) Batch‑fetch metadata for all involved IDs. */
        List<String> allIds = rawGroups.stream()
                .flatMap(g -> g.itemIds().stream())
                .distinct()
                .toList();

        Map<String, ItemDTO> itemMap = itemsClient.fetchItemsByIds(allIds).stream()
                .collect(Collectors.toMap(ItemDTO::getId, it -> it));

        /* 3) Apply the greedy algorithm per category. */
        List<CategoryGroupDTO> result = new ArrayList<>();

        for (CategoryGroupDTO group : rawGroups) {
            var intervals = group.itemIds().stream()
                    .map(itemMap::get)
                    .filter(Objects::nonNull)
                    .map(ItemInterval::of)
                    .sorted(Comparator.comparing(ItemInterval::end))
                    .toList();

            List<String> selected = new ArrayList<>();
            OffsetDateTime lastEnd = null;

            for (ItemInterval interval : intervals) {
                if (lastEnd == null || !lastEnd.isAfter(interval.start())) {
                    selected.add(interval.itemId());
                    lastEnd = interval.end();
                }
            }

            if (!selected.isEmpty()) {
                result.add(new CategoryGroupDTO(group.rootCategoryId(), List.copyOf(selected)));
            }
        }
        return List.copyOf(result);
    }

    /* ───────────────────────────────────────────────────────────────────────────── */

    private record ItemInterval(String itemId, OffsetDateTime start, OffsetDateTime end) {
        static ItemInterval of(ItemDTO dto) {
            Objects.requireNonNull(dto.getDateCreated(), () -> "dateCreated is null for " + dto.getId());
            Objects.requireNonNull(dto.getLastUpdated(), () -> "lastUpdated is null for " + dto.getId());
            return new ItemInterval(dto.getId(), dto.getDateCreated(), dto.getLastUpdated());
        }
    }
}
