ALTER TABLE T_PLAYER_CURRENT_STATE
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_PLAYER_POSITION_TRANSACTION
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_PLAYER_JOIN_TRANSACTION
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_PLAYER_LEAVE_TRANSACTION
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_SLEEPER_TRANSACTION
    ADD COLUMN player_id BIGINT;

ALTER TABLE T_LEVEL_XP_SUMMARY_TRANSACTION
    ADD COLUMN player_id BIGINT;

UPDATE T_PLAYER_CURRENT_STATE c
SET player_id = (
    SELECT p.id
    FROM M_PLAYER p
    WHERE (c.cross_platform_id IS NOT NULL AND c.cross_platform_id <> ''
            AND (
                p.player_key = 'EOS:' || REPLACE(c.cross_platform_id, 'EOS_', '')
                OR (UPPER(p.platform) = 'EOS' AND p.user_id = REPLACE(c.cross_platform_id, 'EOS_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'EOS' AND p.native_user_id = REPLACE(c.cross_platform_id, 'EOS_', ''))
            ))
       OR (c.platform_id IS NOT NULL AND c.platform_id <> ''
            AND (
                p.player_key = 'Steam:' || REPLACE(c.platform_id, 'Steam_', '')
                OR (UPPER(p.platform) = 'STEAM' AND p.user_id = REPLACE(c.platform_id, 'Steam_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'STEAM' AND p.native_user_id = REPLACE(c.platform_id, 'Steam_', ''))
            ))
    ORDER BY p.last_seen_at DESC NULLS LAST, p.id DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_PLAYER_POSITION_TRANSACTION t
SET player_id = (
    SELECT c.player_id
    FROM T_PLAYER_CURRENT_STATE c
    WHERE c.player_id IS NOT NULL
      AND (
        c.player_entity_id = t.player_entity_id
        OR (c.player_name = t.player_name AND c.last_updated >= t.occurred_at - INTERVAL '1' DAY)
      )
    ORDER BY c.last_updated DESC, c.player_entity_id DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_PLAYER_JOIN_TRANSACTION t
SET player_id = (
    SELECT p.id
    FROM M_PLAYER p
    WHERE (t.cross_platform_id IS NOT NULL AND t.cross_platform_id <> ''
            AND (
                p.player_key = 'EOS:' || REPLACE(t.cross_platform_id, 'EOS_', '')
                OR (UPPER(p.platform) = 'EOS' AND p.user_id = REPLACE(t.cross_platform_id, 'EOS_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'EOS' AND p.native_user_id = REPLACE(t.cross_platform_id, 'EOS_', ''))
            ))
       OR (t.platform_id IS NOT NULL AND t.platform_id <> ''
            AND (
                p.player_key = 'Steam:' || REPLACE(t.platform_id, 'Steam_', '')
                OR (UPPER(p.platform) = 'STEAM' AND p.user_id = REPLACE(t.platform_id, 'Steam_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'STEAM' AND p.native_user_id = REPLACE(t.platform_id, 'Steam_', ''))
            ))
    ORDER BY p.last_seen_at DESC NULLS LAST, p.id DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_PLAYER_LEAVE_TRANSACTION t
SET player_id = (
    SELECT p.id
    FROM M_PLAYER p
    WHERE (t.cross_platform_id IS NOT NULL AND t.cross_platform_id <> ''
            AND (
                p.player_key = 'EOS:' || REPLACE(t.cross_platform_id, 'EOS_', '')
                OR (UPPER(p.platform) = 'EOS' AND p.user_id = REPLACE(t.cross_platform_id, 'EOS_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'EOS' AND p.native_user_id = REPLACE(t.cross_platform_id, 'EOS_', ''))
            ))
       OR (t.platform_id IS NOT NULL AND t.platform_id <> ''
            AND (
                p.player_key = 'Steam:' || REPLACE(t.platform_id, 'Steam_', '')
                OR (UPPER(p.platform) = 'STEAM' AND p.user_id = REPLACE(t.platform_id, 'Steam_', ''))
                OR (UPPER(COALESCE(p.native_platform, '')) = 'STEAM' AND p.native_user_id = REPLACE(t.platform_id, 'Steam_', ''))
            ))
    ORDER BY p.last_seen_at DESC NULLS LAST, p.id DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_ENTITY_KILL_TRANSACTION t
SET player_id = (
    SELECT c.player_id
    FROM T_PLAYER_CURRENT_STATE c
    WHERE c.player_id IS NOT NULL
      AND c.player_entity_id = t.player_entity_id
    ORDER BY c.last_updated DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_SLEEPER_TRANSACTION t
SET player_id = (
    SELECT c.player_id
    FROM T_PLAYER_CURRENT_STATE c
    WHERE c.player_id IS NOT NULL
      AND c.player_entity_id = t.player_entity_id
    ORDER BY c.last_updated DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

UPDATE T_LEVEL_XP_SUMMARY_TRANSACTION t
SET player_id = (
    SELECT c.player_id
    FROM T_PLAYER_CURRENT_STATE c
    WHERE c.player_id IS NOT NULL
      AND c.player_entity_id = t.player_entity_id
    ORDER BY c.last_updated DESC
    FETCH FIRST 1 ROW ONLY
)
WHERE player_id IS NULL;

DELETE FROM T_PLAYER_CURRENT_STATE
WHERE player_entity_id IN (
    SELECT player_entity_id
    FROM (
        SELECT player_entity_id,
               ROW_NUMBER() OVER (
                   PARTITION BY player_id
                   ORDER BY online DESC, last_updated DESC, player_entity_id DESC
               ) AS duplicate_rank
        FROM T_PLAYER_CURRENT_STATE
        WHERE player_id IS NOT NULL
    ) ranked_current_state
    WHERE duplicate_rank > 1
);

ALTER TABLE T_PLAYER_CURRENT_STATE
    ADD CONSTRAINT fk_t_player_current_state_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_PLAYER_POSITION_TRANSACTION
    ADD CONSTRAINT fk_t_player_position_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_PLAYER_JOIN_TRANSACTION
    ADD CONSTRAINT fk_t_player_join_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_PLAYER_LEAVE_TRANSACTION
    ADD CONSTRAINT fk_t_player_leave_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD CONSTRAINT fk_t_entity_kill_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_SLEEPER_TRANSACTION
    ADD CONSTRAINT fk_t_sleeper_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

ALTER TABLE T_LEVEL_XP_SUMMARY_TRANSACTION
    ADD CONSTRAINT fk_t_level_xp_summary_player
    FOREIGN KEY (player_id) REFERENCES M_PLAYER(id);

CREATE UNIQUE INDEX idx_t_player_current_state_player_unique
    ON T_PLAYER_CURRENT_STATE (player_id);

CREATE INDEX idx_t_player_position_player_id ON T_PLAYER_POSITION_TRANSACTION (player_id);
CREATE INDEX idx_t_player_join_player_id ON T_PLAYER_JOIN_TRANSACTION (player_id);
CREATE INDEX idx_t_player_leave_player_id ON T_PLAYER_LEAVE_TRANSACTION (player_id);
CREATE INDEX idx_t_entity_kill_player_id ON T_ENTITY_KILL_TRANSACTION (player_id);
CREATE INDEX idx_t_sleeper_player_id ON T_SLEEPER_TRANSACTION (player_id);
CREATE INDEX idx_t_level_xp_summary_player_id ON T_LEVEL_XP_SUMMARY_TRANSACTION (player_id);
