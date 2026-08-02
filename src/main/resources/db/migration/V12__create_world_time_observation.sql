CREATE TABLE T_WORLD_TIME_OBSERVATION (
    world_time_observation_id BIGSERIAL PRIMARY KEY,
    observed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    game_day INTEGER NOT NULL,
    game_hour INTEGER NOT NULL,
    game_minute INTEGER NOT NULL,
    source TEXT NOT NULL,
    source_hash VARCHAR(64) NOT NULL UNIQUE,
    raw_response TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_t_world_time_observed_at ON T_WORLD_TIME_OBSERVATION (observed_at);
CREATE INDEX idx_t_world_time_game_day ON T_WORLD_TIME_OBSERVATION (game_day);
