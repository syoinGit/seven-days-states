CREATE TABLE T_PLAYER_CURRENT_STATE (
    player_entity_id INTEGER PRIMARY KEY,
    player_name TEXT NOT NULL,
    position_x INTEGER NOT NULL,
    position_y INTEGER,
    position_z INTEGER NOT NULL,
    rotation_x NUMERIC,
    rotation_y NUMERIC,
    rotation_z NUMERIC,
    health INTEGER,
    deaths INTEGER,
    zombies INTEGER,
    players INTEGER,
    score INTEGER,
    level INTEGER,
    platform_id TEXT,
    cross_platform_id TEXT,
    ping INTEGER,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_t_player_current_state_player_name ON T_PLAYER_CURRENT_STATE (player_name);
CREATE INDEX idx_t_player_current_state_position ON T_PLAYER_CURRENT_STATE (position_x, position_z);
CREATE INDEX idx_t_player_current_state_last_updated ON T_PLAYER_CURRENT_STATE (last_updated DESC);

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD COLUMN player_position_x INTEGER;

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD COLUMN player_position_y INTEGER;

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD COLUMN player_position_z INTEGER;

ALTER TABLE T_ENTITY_KILL_TRANSACTION
    ADD COLUMN player_current_state_updated_at TIMESTAMP WITH TIME ZONE;
