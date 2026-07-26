package com.yuki.sevendays_states.log.dto;

import java.time.OffsetDateTime;

public record EntityKillLogEvent(
    OffsetDateTime occurredAt,
    String playerName,
    int playerEntityId,
    String targetEntityType,
    int targetEntityId,
    String rawLine
) {
}
