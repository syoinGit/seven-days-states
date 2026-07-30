CREATE TABLE T_WORLD_EVENT_TRANSACTION (
    world_event_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    actor_player_name TEXT,
    actor_player_entity_id INTEGER,
    player_id BIGINT,
    detail_text TEXT,
    position_x INTEGER,
    position_y INTEGER,
    position_z INTEGER,
    target_position_x INTEGER,
    target_position_y INTEGER,
    target_position_z INTEGER,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    raw_line TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_t_world_event_player
        FOREIGN KEY (player_id) REFERENCES M_PLAYER(id)
);

CREATE INDEX idx_t_world_event_occurred_at ON T_WORLD_EVENT_TRANSACTION (occurred_at);
CREATE INDEX idx_t_world_event_type ON T_WORLD_EVENT_TRANSACTION (event_type);
CREATE INDEX idx_t_world_event_player_id ON T_WORLD_EVENT_TRANSACTION (player_id);
CREATE INDEX idx_t_world_event_position ON T_WORLD_EVENT_TRANSACTION (position_x, position_z);

CREATE TABLE T_VEHICLE_CURRENT_STATE (
    vehicle_entity_id INTEGER PRIMARY KEY,
    vehicle_type TEXT NOT NULL,
    vehicle_name TEXT,
    owner_player_id BIGINT,
    owner_cross_platform_id TEXT,
    position_x INTEGER,
    position_y INTEGER,
    position_z INTEGER,
    total_distance NUMERIC NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    destroyed_at TIMESTAMP WITH TIME ZONE,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_t_vehicle_current_owner_player
        FOREIGN KEY (owner_player_id) REFERENCES M_PLAYER(id)
);

CREATE INDEX idx_t_vehicle_current_owner_player_id ON T_VEHICLE_CURRENT_STATE (owner_player_id);
CREATE INDEX idx_t_vehicle_current_owner_cross_platform_id ON T_VEHICLE_CURRENT_STATE (owner_cross_platform_id);
CREATE INDEX idx_t_vehicle_current_active ON T_VEHICLE_CURRENT_STATE (active);
CREATE INDEX idx_t_vehicle_current_position ON T_VEHICLE_CURRENT_STATE (position_x, position_z);

CREATE TABLE T_VEHICLE_POSITION_TRANSACTION (
    vehicle_position_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    vehicle_entity_id INTEGER NOT NULL,
    vehicle_type TEXT NOT NULL,
    vehicle_name TEXT,
    owner_player_id BIGINT,
    owner_cross_platform_id TEXT,
    position_x INTEGER,
    position_y INTEGER,
    position_z INTEGER,
    movement_distance NUMERIC NOT NULL DEFAULT 0,
    removal_reason TEXT,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    raw_line TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_t_vehicle_position_owner_player
        FOREIGN KEY (owner_player_id) REFERENCES M_PLAYER(id)
);

CREATE INDEX idx_t_vehicle_position_occurred_at ON T_VEHICLE_POSITION_TRANSACTION (occurred_at);
CREATE INDEX idx_t_vehicle_position_vehicle_entity_id ON T_VEHICLE_POSITION_TRANSACTION (vehicle_entity_id);
CREATE INDEX idx_t_vehicle_position_owner_player_id ON T_VEHICLE_POSITION_TRANSACTION (owner_player_id);
