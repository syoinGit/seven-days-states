UPDATE T_LEVEL_XP_SUMMARY_TRANSACTION xp
SET player_name = (
        SELECT kill.player_name
        FROM T_ENTITY_KILL_TRANSACTION kill
        WHERE kill.occurred_at <= xp.occurred_at
          AND kill.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
        ORDER BY kill.occurred_at DESC, kill.entity_kill_transaction_id DESC
        FETCH FIRST 1 ROW ONLY
    ),
    player_entity_id = (
        SELECT kill.player_entity_id
        FROM T_ENTITY_KILL_TRANSACTION kill
        WHERE kill.occurred_at <= xp.occurred_at
          AND kill.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
        ORDER BY kill.occurred_at DESC, kill.entity_kill_transaction_id DESC
        FETCH FIRST 1 ROW ONLY
    ),
    player_id = (
        SELECT kill.player_id
        FROM T_ENTITY_KILL_TRANSACTION kill
        WHERE kill.occurred_at <= xp.occurred_at
          AND kill.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
        ORDER BY kill.occurred_at DESC, kill.entity_kill_transaction_id DESC
        FETCH FIRST 1 ROW ONLY
    ),
    player_inference_method = 'recent_kill_before_xp_backfill'
WHERE xp.player_name IS NULL
  AND (
      SELECT COUNT(*)
      FROM T_ENTITY_KILL_TRANSACTION kill
      WHERE kill.occurred_at <= xp.occurred_at
        AND kill.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
  ) = 1
  AND (
      SELECT COUNT(*)
      FROM T_LEVEL_XP_SUMMARY_TRANSACTION nearby_xp
      WHERE nearby_xp.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
        AND nearby_xp.occurred_at <= xp.occurred_at + INTERVAL '5' SECOND
  ) = 1
  AND EXISTS (
      SELECT 1
      FROM T_ENTITY_KILL_TRANSACTION kill
      WHERE kill.occurred_at <= xp.occurred_at
        AND kill.occurred_at >= xp.occurred_at - INTERVAL '5' SECOND
  );
