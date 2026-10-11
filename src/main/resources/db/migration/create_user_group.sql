CREATE TABLE user_group (
                            id BIGSERIAL PRIMARY KEY,

                            user_id BIGINT NOT NULL,
                            group_id BIGINT NOT NULL,

                            balance NUMERIC(15,2) NOT NULL DEFAULT 0,
                            received BOOLEAN NOT NULL DEFAULT FALSE,
                            member_order INTEGER NOT NULL,

                            CONSTRAINT fk_user_group_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id),

                            CONSTRAINT fk_user_group_group
                                FOREIGN KEY (group_id)
                                    REFERENCES groups(id),

                            CONSTRAINT uk_user_group
                                UNIQUE (user_id, group_id)
);