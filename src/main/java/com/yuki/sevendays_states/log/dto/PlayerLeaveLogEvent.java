package com.yuki.sevendays_states.log.dto;

import java.time.OffsetDateTime;

public record PlayerLeaveLogEvent(
    OffsetDateTime occurredAt,
    String playerName,
    int playerEntityId,
    String platformId,
    String crossPlatformId,
    Integer clientNumber,
    String rawLine
) {
}
