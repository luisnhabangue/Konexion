CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(150) NOT NULL,
                       doc_nr VARCHAR(50) NOT NULL UNIQUE
);