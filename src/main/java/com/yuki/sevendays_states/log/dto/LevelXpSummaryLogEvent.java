package com.yuki.sevendays_states.log.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record LevelXpSummaryLogEvent(
    OffsetDateTime occurredAt,
    int xpFromLoot,
    int xpFromHarvesting,
    int xpFromKill,
    int xpTotal,
    List<String> rawLines,
    int consumedLineCount
) {
}
