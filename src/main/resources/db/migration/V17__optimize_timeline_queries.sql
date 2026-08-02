CREATE INDEX IF NOT EXISTS idx_t_player_join_occurred_desc
    ON T_PLAYER_JOIN_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_player_leave_occurred_desc
    ON T_PLAYER_LEAVE_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_entity_kill_occurred_desc
    ON T_ENTITY_KILL_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_sleeper_occurred_desc
    ON T_SLEEPER_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_player_position_occurred_desc
    ON T_PLAYER_POSITION_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_vehicle_position_occurred_desc
    ON T_VEHICLE_POSITION_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_level_xp_summary_occurred_desc
    ON T_LEVEL_XP_SUMMARY_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_world_event_occurred_desc
    ON T_WORLD_EVENT_TRANSACTION (occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_world_time_day_observed
    ON T_WORLD_TIME_OBSERVATION (game_day, observed_at);
