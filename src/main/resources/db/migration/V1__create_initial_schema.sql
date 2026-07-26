CREATE TABLE T_IMPORT_RUN (
    id BIGSERIAL PRIMARY KEY,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    environment_name VARCHAR(80),
    source_root VARCHAR(500) NOT NULL,
    config_dir VARCHAR(500),
    data_dir VARCHAR(500),
    game_dir VARCHAR(500),
    status VARCHAR(40) NOT NULL,
    message TEXT
);

CREATE INDEX idx_t_import_run_started_at ON T_IMPORT_RUN (started_at DESC);
CREATE INDEX idx_t_import_run_status ON T_IMPORT_RUN (status);

CREATE TABLE M_SERVER_CONFIG_SETTING (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    setting_key VARCHAR(180) NOT NULL UNIQUE,
    setting_value TEXT,
    is_sensitive BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE M_GAME_CONFIG_ELEMENT (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    config_name VARCHAR(120) NOT NULL,
    element_name VARCHAR(120) NOT NULL,
    entity_key VARCHAR(260) NOT NULL,
    extends_key VARCHAR(260),
    display_name_key VARCHAR(260),
    category VARCHAR(120),
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    raw_xml TEXT NOT NULL
);

CREATE INDEX idx_m_game_config_element_config ON M_GAME_CONFIG_ELEMENT (config_name);
CREATE INDEX idx_m_game_config_element_entity_key ON M_GAME_CONFIG_ELEMENT (entity_key);

CREATE TABLE M_JAPANESE_TRANSLATION (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    localization_key VARCHAR(260) NOT NULL UNIQUE,
    source VARCHAR(120),
    entry_type VARCHAR(120),
    context TEXT,
    english TEXT,
    japanese TEXT,
    display_text TEXT NOT NULL,
    translated BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_m_japanese_translation_source ON M_JAPANESE_TRANSLATION (source);
CREATE INDEX idx_m_japanese_translation_translated ON M_JAPANESE_TRANSLATION (translated);

CREATE TABLE M_GAME_ENTITY (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    entity_key VARCHAR(180) NOT NULL UNIQUE,
    entity_type VARCHAR(80),
    display_name_key VARCHAR(260),
    category VARCHAR(120),
    tags TEXT,
    raw_xml TEXT NOT NULL
);

CREATE INDEX idx_m_game_entity_type ON M_GAME_ENTITY (entity_type);
CREATE INDEX idx_m_game_entity_category ON M_GAME_ENTITY (category);

CREATE TABLE M_BLOCK (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    block_key VARCHAR(180) NOT NULL UNIQUE,
    display_name_key VARCHAR(260),
    material VARCHAR(120),
    shape VARCHAR(120),
    category VARCHAR(120),
    tags TEXT,
    raw_xml TEXT NOT NULL
);

CREATE INDEX idx_m_block_category ON M_BLOCK (category);
CREATE INDEX idx_m_block_material ON M_BLOCK (material);

CREATE TABLE M_ITEM (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    item_key VARCHAR(180) NOT NULL UNIQUE,
    item_type VARCHAR(80),
    display_name_key VARCHAR(260),
    category VARCHAR(120),
    tags TEXT,
    raw_xml TEXT NOT NULL
);

CREATE INDEX idx_m_item_type ON M_ITEM (item_type);
CREATE INDEX idx_m_item_category ON M_ITEM (category);

CREATE TABLE M_VEHICLE (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    vehicle_key VARCHAR(180) NOT NULL UNIQUE,
    entity_class_key VARCHAR(180),
    display_name_key VARCHAR(260),
    vehicle_type VARCHAR(80),
    raw_xml TEXT NOT NULL
);

CREATE INDEX idx_m_vehicle_entity_class ON M_VEHICLE (entity_class_key);

CREATE TABLE M_WORLD (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    world_name VARCHAR(160) NOT NULL UNIQUE,
    height_map_size INTEGER,
    generation_seed VARCHAR(160),
    raw_xml TEXT
);

CREATE TABLE M_GAME_SAVE (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    world_name VARCHAR(160) NOT NULL,
    game_name VARCHAR(160) NOT NULL,
    save_path VARCHAR(500) NOT NULL,
    last_scanned_at TIMESTAMP,
    source_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE UNIQUE INDEX idx_m_game_save_world_game ON M_GAME_SAVE (world_name, game_name);

CREATE TABLE M_WORLD_POI (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    world_name VARCHAR(160) NOT NULL,
    game_name VARCHAR(160),
    poi_name VARCHAR(180) NOT NULL,
    poi_type VARCHAR(80),
    category VARCHAR(80),
    x INTEGER NOT NULL,
    y INTEGER NOT NULL,
    z INTEGER NOT NULL,
    rotation INTEGER
);

CREATE INDEX idx_m_world_poi_world ON M_WORLD_POI (world_name, game_name);
CREATE INDEX idx_m_world_poi_position ON M_WORLD_POI (x, z);

CREATE TABLE M_WORLD_SPAWN_POINT (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    world_name VARCHAR(160) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    z DOUBLE PRECISION NOT NULL,
    rotation_x DOUBLE PRECISION,
    rotation_y DOUBLE PRECISION,
    rotation_z DOUBLE PRECISION
);

CREATE INDEX idx_m_world_spawn_point_world ON M_WORLD_SPAWN_POINT (world_name);

CREATE TABLE M_PLAYER (
    id BIGSERIAL PRIMARY KEY,
    source_path VARCHAR(500),
    player_key VARCHAR(180) NOT NULL UNIQUE,
    platform VARCHAR(40) NOT NULL,
    user_id VARCHAR(120) NOT NULL,
    native_platform VARCHAR(40),
    native_user_id VARCHAR(120),
    player_name VARCHAR(120) NOT NULL,
    first_seen_at TIMESTAMP,
    last_seen_at TIMESTAMP
);

CREATE INDEX idx_m_player_user ON M_PLAYER (platform, user_id);
CREATE INDEX idx_m_player_name ON M_PLAYER (player_name);

CREATE TABLE T_PLAYER_STATE_SNAPSHOT (
    id BIGSERIAL PRIMARY KEY,
    import_run_id BIGINT REFERENCES T_IMPORT_RUN(id),
    source_path VARCHAR(500),
    player_id BIGINT REFERENCES M_PLAYER(id),
    world_name VARCHAR(160) NOT NULL,
    game_name VARCHAR(160) NOT NULL,
    captured_at TIMESTAMP NOT NULL,
    play_group VARCHAR(80),
    last_login TIMESTAMP,
    x INTEGER,
    y INTEGER,
    z INTEGER,
    source_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE INDEX idx_t_player_state_snapshot_player ON T_PLAYER_STATE_SNAPSHOT (player_id);
CREATE INDEX idx_t_player_state_snapshot_world ON T_PLAYER_STATE_SNAPSHOT (world_name, game_name);
CREATE INDEX idx_t_player_state_snapshot_captured_at ON T_PLAYER_STATE_SNAPSHOT (captured_at DESC);

CREATE TABLE T_PLAYER_MARKER_SNAPSHOT (
    id BIGSERIAL PRIMARY KEY,
    import_run_id BIGINT REFERENCES T_IMPORT_RUN(id),
    source_path VARCHAR(500),
    player_id BIGINT REFERENCES M_PLAYER(id),
    world_name VARCHAR(160) NOT NULL,
    game_name VARCHAR(160) NOT NULL,
    captured_at TIMESTAMP NOT NULL,
    marker_type VARCHAR(40) NOT NULL,
    ref_id VARCHAR(80),
    position_data_type VARCHAR(40),
    target_platform VARCHAR(40),
    target_user_id VARCHAR(120),
    x INTEGER,
    y INTEGER,
    z INTEGER,
    source_hash VARCHAR(64) NOT NULL UNIQUE
);

CREATE INDEX idx_t_player_marker_snapshot_player ON T_PLAYER_MARKER_SNAPSHOT (player_id);
CREATE INDEX idx_t_player_marker_snapshot_world ON T_PLAYER_MARKER_SNAPSHOT (world_name, game_name);
CREATE INDEX idx_t_player_marker_snapshot_type ON T_PLAYER_MARKER_SNAPSHOT (marker_type);
