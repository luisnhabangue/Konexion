CREATE TABLE history_group (
                               id BIGSERIAL PRIMARY KEY,

                               group_id BIGINT NOT NULL,
                               description VARCHAR(500) NOT NULL,
                               status VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP NOT NULL,
                               created_by VARCHAR(100) NOT NULL,

                               CONSTRAINT fk_history_group_group
                                   FOREIGN KEY (group_id)
                                       REFERENCES groups(id)
);