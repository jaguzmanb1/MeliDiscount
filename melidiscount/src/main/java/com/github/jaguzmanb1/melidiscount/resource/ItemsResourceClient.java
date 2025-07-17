package com.github.jaguzmanb1.melidiscount.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.jaguzmanb1.melidiscount.dto.CategoryGroupDTO;
import com.github.jaguzmanb1.melidiscount.dto.ItemDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Strongly‑typed client for the Items service.
 * <p>Ahora también consulta <code>/meli_discount/categories</code>.</p>
 */
@Component
public class ItemsResourceClient {

    /* ---------- Jackson Type Tokens ---------- */
    private static final TypeReference<List<ItemDTO>>           ITEM_LIST_REF   = new TypeReference<>() {};
    private static final TypeReference<List<CategoryGroupDTO>>  GROUP_LIST_REF  = new TypeReference<>() {};

    /* ---------- Config ---------- */
    private final String itemsUrl;       // …/items
    private final String categoriesUrl;  // …/meli_discount/categories
    private final HttpClient   httpClient;
    private final ObjectMapper objectMapper;

    public ItemsResourceClient(
            @Value("${external.items-service.base-url:http://localhost:8080/items}")
            String itemsUrl,
            HttpClient httpClient,
            ObjectMapper objectMapper) {

        this.itemsUrl      = Objects.requireNonNull(itemsUrl, "itemsUrl must not be null");
        this.httpClient    = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.objectMapper  = Objects.requireNonNull(objectMapper, "objectMapper must not be null");

        /* Derivamos la URL de categorías a partir de la de items para no pedir más pará‑metros. */
        this.categoriesUrl = deriveCategoriesUrl(itemsUrl);

        this.objectMapper.registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /* ========================  PUBLIC API  ======================== */

    /**
     * Llama a <code>/items?ids=…</code>.
     */
    public List<ItemDTO> fetchItemsByIds(List<String> itemIds) {
        return performGet(
                itemsUrl,
                "ids",
                itemIds,
                ITEM_LIST_REF,
                ItemsClientException::new
        );
    }

    /**
     * Llama a <code>/meli_discount/categories?item_ids=…</code>
     * y devuelve los grupos por categoría raíz.
     */
    public List<CategoryGroupDTO> groupByRootCategory(List<String> itemIds) {
        return performGet(
                categoriesUrl,
                "ids",
                itemIds,
                GROUP_LIST_REF,
                ItemsClientException::new           // Reutilizamos la misma excepción de dominio
        );
    }

    /* ========================  INTERNAL UTILS  ======================== */

    /**
     * Utilidad genérica para GET con lista de IDs y deserialización en tipo genérico.
     */
    private <T> List<T> performGet(
            String base,
            String paramName,
            List<String> ids,
            TypeReference<List<T>> ref,
            ExceptionFactory exFactory) {

        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        try {
            String idsParam = ids.stream()
                    .map(id -> URLEncoder.encode(id, StandardCharsets.UTF_8))
                    .collect(Collectors.joining(","));

            URI uri = URI.create(String.format("%s?%s=%s", base, paramName, idsParam));

            HttpRequest request = HttpRequest.newBuilder().uri(uri).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw exFactory.build("Service responded HTTP " + response.statusCode());
            }

            List<T> result = objectMapper.readValue(response.body(), ref);
            return result == null ? List.of() : List.copyOf(result);

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw exFactory.build("Call interrupted", ie);
        } catch (Exception e) {
            throw exFactory.build("I/O Deserialization error", e);
        }
    }

    private static String deriveCategoriesUrl(String itemsUrl) {
        // Si termina en “…/items”, lo reemplazamos; si no, agregamos la ruta completa.
        return itemsUrl.endsWith("/items")
                ? itemsUrl.replace("/items", "/categories")
                : itemsUrl + "/categories";
    }

    /* ---------- Domain‑specific unchecked exception ---------- */
    public static class ItemsClientException extends RuntimeException {
        public ItemsClientException(String msg) { super(msg); }
        public ItemsClientException(String msg, Throwable c) { super(msg, c); }
    }

    /* Pequeña factory para no duplicar código en performGet */
    @FunctionalInterface
    private interface ExceptionFactory {
        ItemsClientException build(String msg);
        default ItemsClientException build(String msg, Throwable cause) {
            return new ItemsClientException(msg, cause);
        }
    }
}
