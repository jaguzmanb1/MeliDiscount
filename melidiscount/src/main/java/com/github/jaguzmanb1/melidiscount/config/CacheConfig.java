package com.github.jaguzmanb1.melidiscount.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global cache configuration: Caffeine + custom KeyGenerator.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Primary CacheManager using Caffeine.
     * Tweak size / TTL as needed or externalise with spring.cache.caffeine.spec.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager mgr = new CaffeineCacheManager(
                "discounts",            // ya existente
                "discountsByCategory",  // nuevo  (findMaxNonOverlappingItemsByCategory)
                "rootCategoryGroups"    // nuevo  (groupItemsByRootCategory)
        );

        // ➋  Configuración base: tamaño máx. y TTL
        mgr.setCaffeine(
                Caffeine.newBuilder()
                        .maximumSize(5_000)
                        .expireAfterWrite(Duration.ofMinutes(15))
        );
        return mgr;
    }

    /**
     * KeyGenerator that:
     *  1. Accepts the first parameter (List&lt;String&gt; itemIds).
     *  2. Deduplicates, ordena y concatena (para que el orden en la URL no cause "cache miss").
     *  3. Devuelve una String estable: "MLA1,MLA2,MLA3".
     */
    @Bean("sortedIdsKeyGenerator")
    public KeyGenerator sortedIdsKeyGenerator() {
        return new KeyGenerator() {
            @Override
            @SuppressWarnings("unchecked")
            public Object generate(Object target, Method method, Object... params) {
                if (params.length == 0 || params[0] == null) {
                    return "[]";
                }

                if (params[0] instanceof List<?> raw) {
                    List<String> ids = (List<String>) raw;
                    return ids.stream()
                              .map(Objects::toString)
                              .distinct()
                              .sorted()
                              .collect(Collectors.joining(","));
                }
                // Fallback: just rely on the first param's toString()
                return params[0].toString();
            }
        };
    }
}
