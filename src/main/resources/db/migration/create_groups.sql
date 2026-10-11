CREATE TABLE groups (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(150) NOT NULL,
                        description VARCHAR(500),
                        cycle INTEGER NOT NULL DEFAULT 0,
                        amount NUMERIC(15,2) NOT NULL,
                        schedule INTEGER NOT NULL,
                        created_by BIGINT NOT NULL,
                        created_at TIMESTAMP NOT NULL,
                        start_at DATE NOT NULL,
                        month INTEGER NOT NULL,

                        CONSTRAINT fk_groups_created_by
                            FOREIGN KEY (created_by)
                                REFERENCES users(id)
);