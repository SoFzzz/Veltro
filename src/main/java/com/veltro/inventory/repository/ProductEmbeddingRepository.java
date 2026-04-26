package com.veltro.inventory.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.util.Optional;
import java.util.Arrays;

@Repository
public class ProductEmbeddingRepository {

    private final JdbcTemplate jdbcTemplate;

    public ProductEmbeddingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String SEARCH_QUERY =
            "SELECT p.id, p.name, (1 - (pe.embedding <=> ?::vector)) as confidence " +
            "FROM product_embeddings pe " +
            "JOIN products p ON pe.product_id = p.id " +
            "ORDER BY pe.embedding <=> ?::vector " +
            "LIMIT 1";

    public Optional<SemanticSearchResult> findMostSimilarProduct(float[] embedding) {
        // Convert float[] to string format expected by pgvector: "[0.1, 0.2, ...]"
        String embeddingString = Arrays.toString(embedding);
        
        return jdbcTemplate.query(
            SEARCH_QUERY,
            (ResultSet rs) -> {
                if (rs.next()) {
                    return Optional.of(new SemanticSearchResult(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getDouble("confidence")
                    ));
                }
                return Optional.<SemanticSearchResult>empty();
            },
            embeddingString, embeddingString
        );
    }

    public void insertEmbedding(Long productId, float[] embedding) {
        String embeddingString = Arrays.toString(embedding);
        
        jdbcTemplate.update(
            "INSERT INTO product_embeddings (product_id, embedding) VALUES (?, ?::vector)",
            productId, embeddingString
        );
    }

    public record SemanticSearchResult(Long productId, String name, double confidence) {}
}
