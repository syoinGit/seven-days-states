CREATE INDEX IF NOT EXISTS idx_m_player_platform_user_lookup
    ON M_PLAYER (platform, user_id);

CREATE INDEX IF NOT EXISTS idx_m_player_native_user_lookup
    ON M_PLAYER (native_platform, native_user_id);

CREATE INDEX IF NOT EXISTS idx_t_player_current_state_identity_lookup
    ON T_PLAYER_CURRENT_STATE (cross_platform_id, platform_id, last_updated DESC);

CREATE INDEX IF NOT EXISTS idx_t_player_position_latest_name_lookup
    ON T_PLAYER_POSITION_TRANSACTION (player_name, occurred_at DESC);
