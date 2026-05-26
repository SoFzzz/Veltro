-- V13: Migrate primary embedding index from IVFFlat to HNSW
--
-- WHY:
--   IVFFlat requires the number of clusters (lists) to be calibrated upfront
--   against the dataset size. A catalog that grows dynamically degrades
--   IVFFlat recall without periodic VACUUM + REINDEX. HNSW builds a navigable
--   small-world graph that is inherently dynamic, delivers O(log N) search
--   and consistently achieves >98% recall without reconfiguration.
--
-- PREREQUISITES (validate before applying):
--   SELECT extversion FROM pg_extension WHERE extname = 'vector';
--   → Must return >= 0.5.0  (HNSW was introduced in pgvector 0.5.0)
--
-- PARAMETERS chosen:
--   m        = 16  → neighbors per node during construction. Default; good
--                    balance for 512-dim embeddings. Increase to 32 only if
--                    recall testing shows gaps (trades memory for accuracy).
--   ef_construction = 128 → candidates explored at build time. 128 is the
--                    standard starting point; higher values build a better
--                    graph but slow down the initial CREATE INDEX execution.
--
-- SCOPE:
--   Only the PRIMARY embedding column (embedding). The secondary column
--   (embedding_secondary, added in V12) is intentionally left without an
--   HNSW index at this stage because:
--     a) it is sparse (many NULLs) and pgvector HNSW does not skip NULLs
--        efficiently during index builds,
--     b) its query patterns have not yet been audited.
--   A dedicated migration for embedding_secondary can be added later.
--
-- ROLLBACK PLAN (if needed):
--   DROP INDEX IF EXISTS idx_products_embedding;
--   CREATE INDEX idx_products_embedding
--     ON products USING ivfflat (embedding vector_cosine_ops)
--     WITH (lists = 100) WHERE active = true;
-- ──────────────────────────────────────────────────────────────────────────────

DROP INDEX IF EXISTS idx_products_embedding;

CREATE INDEX idx_products_embedding
    ON products
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 128)
    WHERE active = true;
