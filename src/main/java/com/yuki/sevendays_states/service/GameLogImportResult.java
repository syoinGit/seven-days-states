package com.yuki.sevendays_states.service;

public record GameLogImportResult(
    long filesRead,
    long linesRead,
    long playerJoins,
    long playerLeaves,
    long entityKills,
    long levelXpSummaries,
    long sleeperSpawns,
    long sleeperRestores,
    long serverMetrics,
    long skippedServerMetrics,
    long malformedLines
) {
}
