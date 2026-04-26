-- Habilitar extensión (Debe ejecutarse por un superusuario/admin en Heroku Postgres)
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla para almacenar los embeddings, separada para no recargar la consulta de la tabla 'productos' normal
CREATE TABLE product_embeddings (
    id SERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    embedding vector(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Índice HNSW (Hierarchical Navigable Small World) para búsquedas de alta velocidad (Approximate Nearest Neighbor)
-- Usamos vector_cosine_ops porque queremos similitud del coseno
CREATE INDEX idx_product_embeddings_hnsw ON product_embeddings USING hnsw (embedding vector_cosine_ops);
