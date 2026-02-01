CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS document_embeddings (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    document_revision_id BIGINT NOT NULL,
    embedding vector(1536),
    chunk_content TEXT NOT NULL,
    token_count INT,
    metadata JSONB
);

CREATE INDEX ON document_embeddings USING hnsw (embedding vector_cosine_ops);
