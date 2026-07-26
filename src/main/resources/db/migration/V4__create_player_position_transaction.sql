CREATE TABLE T_PLAYER_POSITION_TRANSACTION (
    player_position_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_name TEXT NOT NULL,
    player_entity_id INTEGER NOT NULL,
    position_x INTEGER NOT NULL,
    position_y INTEGER,
    position_z INTEGER NOT NULL,
    position_source_type VARCHAR(80) NOT NULL,
    inference_method VARCHAR(80),
    source_event_hash VARCHAR(64) NOT NULL UNIQUE,
    source_file TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_player_position_occurred_at ON T_PLAYER_POSITION_TRANSACTION (occurred_at);
CREATE INDEX idx_t_player_position_player_entity_id ON T_PLAYER_POSITION_TRANSACTION (player_entity_id);
CREATE INDEX idx_t_player_position_player_name ON T_PLAYER_POSITION_TRANSACTION (player_name);
CREATE INDEX idx_t_player_position_position ON T_PLAYER_POSITION_TRANSACTION (position_x, position_z);
