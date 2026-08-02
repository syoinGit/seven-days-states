-- V15 was changed after it had already reached production. Re-declare its idempotent
-- indexes so databases that received the earlier V15 definition converge safely.
CREATE INDEX IF NOT EXISTS idx_t_player_join_player_occurred
    ON T_PLAYER_JOIN_TRANSACTION (player_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_player_leave_player_occurred
    ON T_PLAYER_LEAVE_TRANSACTION (player_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_player_position_player_occurred
    ON T_PLAYER_POSITION_TRANSACTION (player_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_entity_kill_player_occurred
    ON T_ENTITY_KILL_TRANSACTION (player_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_sleeper_player_type_occurred
    ON T_SLEEPER_TRANSACTION (player_id, transaction_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_world_event_player_occurred
    ON T_WORLD_EVENT_TRANSACTION (player_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_world_event_type_occurred
    ON T_WORLD_EVENT_TRANSACTION (event_type, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_t_server_metric_occurred_desc
    ON T_SERVER_METRIC (occurred_at DESC);
