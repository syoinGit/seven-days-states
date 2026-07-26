ALTER TABLE T_PLAYER_CURRENT_STATE
    ADD COLUMN online BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_t_player_current_state_online ON T_PLAYER_CURRENT_STATE (online);
CREATE INDEX idx_t_player_current_state_platform_id ON T_PLAYER_CURRENT_STATE (platform_id);
CREATE INDEX idx_t_player_current_state_cross_platform_id ON T_PLAYER_CURRENT_STATE (cross_platform_id);
