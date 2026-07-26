CREATE TABLE T_PLAYER_JOIN_TRANSACTION (
    player_join_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_name TEXT NOT NULL,
    player_entity_id INTEGER NOT NULL,
    platform_id TEXT,
    cross_platform_id TEXT,
    position_x INTEGER,
    position_y INTEGER,
    position_z INTEGER,
    join_reason VARCHAR(100),
    client_number INTEGER,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_player_join_occurred_at ON T_PLAYER_JOIN_TRANSACTION (occurred_at);
CREATE INDEX idx_t_player_join_player_entity_id ON T_PLAYER_JOIN_TRANSACTION (player_entity_id);
CREATE INDEX idx_t_player_join_player_name ON T_PLAYER_JOIN_TRANSACTION (player_name);

CREATE TABLE T_PLAYER_LEAVE_TRANSACTION (
    player_leave_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_name TEXT NOT NULL,
    player_entity_id INTEGER NOT NULL,
    platform_id TEXT,
    cross_platform_id TEXT,
    client_number INTEGER,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_player_leave_occurred_at ON T_PLAYER_LEAVE_TRANSACTION (occurred_at);
CREATE INDEX idx_t_player_leave_player_entity_id ON T_PLAYER_LEAVE_TRANSACTION (player_entity_id);
CREATE INDEX idx_t_player_leave_player_name ON T_PLAYER_LEAVE_TRANSACTION (player_name);

CREATE TABLE T_ENTITY_KILL_TRANSACTION (
    entity_kill_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_name TEXT NOT NULL,
    player_entity_id INTEGER NOT NULL,
    target_entity_type TEXT NOT NULL,
    target_entity_id INTEGER NOT NULL,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_entity_kill_occurred_at ON T_ENTITY_KILL_TRANSACTION (occurred_at);
CREATE INDEX idx_t_entity_kill_player_entity_id ON T_ENTITY_KILL_TRANSACTION (player_entity_id);
CREATE INDEX idx_t_entity_kill_player_name ON T_ENTITY_KILL_TRANSACTION (player_name);
CREATE INDEX idx_t_entity_kill_target_entity_type ON T_ENTITY_KILL_TRANSACTION (target_entity_type);

CREATE TABLE T_LEVEL_XP_SUMMARY_TRANSACTION (
    level_xp_summary_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    player_name TEXT,
    player_entity_id INTEGER,
    xp_from_loot INTEGER NOT NULL DEFAULT 0,
    xp_from_harvesting INTEGER NOT NULL DEFAULT 0,
    xp_from_kill INTEGER NOT NULL DEFAULT 0,
    xp_total INTEGER NOT NULL,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_level_xp_summary_occurred_at ON T_LEVEL_XP_SUMMARY_TRANSACTION (occurred_at);
CREATE INDEX idx_t_level_xp_summary_player_entity_id ON T_LEVEL_XP_SUMMARY_TRANSACTION (player_entity_id);

CREATE TABLE T_SLEEPER_TRANSACTION (
    sleeper_transaction_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    sleeper_volume_x INTEGER NOT NULL,
    sleeper_volume_y INTEGER NOT NULL,
    sleeper_volume_z INTEGER NOT NULL,
    position_x INTEGER NOT NULL,
    position_y INTEGER NOT NULL,
    position_z INTEGER NOT NULL,
    chunk_x INTEGER,
    chunk_z INTEGER,
    sleeper_group TEXT,
    entity_class TEXT NOT NULL,
    entity_count INTEGER,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_sleeper_occurred_at ON T_SLEEPER_TRANSACTION (occurred_at);
CREATE INDEX idx_t_sleeper_transaction_type ON T_SLEEPER_TRANSACTION (transaction_type);
CREATE INDEX idx_t_sleeper_position ON T_SLEEPER_TRANSACTION (position_x, position_z);
CREATE INDEX idx_t_sleeper_entity_class ON T_SLEEPER_TRANSACTION (entity_class);

CREATE TABLE T_SERVER_METRIC (
    server_metric_id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    uptime_minutes NUMERIC,
    fps NUMERIC,
    heap_mb NUMERIC,
    max_heap_mb NUMERIC,
    chunks INTEGER,
    cgo INTEGER,
    player_count INTEGER,
    zombie_count INTEGER,
    entity_count INTEGER,
    entity_count_detail INTEGER,
    item_count INTEGER,
    co INTEGER,
    rss_mb NUMERIC,
    source_file TEXT NOT NULL,
    source_log_hash VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_server_metric_occurred_at ON T_SERVER_METRIC (occurred_at);
