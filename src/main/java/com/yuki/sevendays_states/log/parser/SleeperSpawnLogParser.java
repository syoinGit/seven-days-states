package com.yuki.sevendays_states.log.parser;

import com.yuki.sevendays_states.log.dto.ParsedLogLine;
import com.yuki.sevendays_states.log.dto.SleeperLogEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SleeperSpawnLogParser {

  private static final Pattern EVENT = Pattern.compile(
      "^(?:\\d+(?:\\.\\d+)\\s+)?SleeperVolume (?<svx>-?\\d+), (?<svy>-?\\d+), (?<svz>-?\\d+): "
          + "Spawning (?<x>-?\\d+), (?<y>-?\\d+), (?<z>-?\\d+) \\((?<chunkX>-?\\d+), (?<chunkZ>-?\\d+)\\), "
          + "group '(?<group>[^']*)', class (?<entityClass>[^,]+), count (?<count>-?\\d+)$");

  private final GameLogLineParser lineParser;

  public SleeperSpawnLogParser() {
    this(new GameLogLineParser());
  }

  public SleeperSpawnLogParser(GameLogLineParser lineParser) {
    this.lineParser = lineParser;
  }

  public boolean matches(String rawLine) {
    return parse(rawLine).isPresent();
  }

  public Optional<SleeperLogEvent> parse(String rawLine) {
    return lineParser.parse(rawLine).flatMap(this::parse);
  }

  public Optional<SleeperLogEvent> parse(ParsedLogLine line) {
    try {
      Matcher matcher = EVENT.matcher(line.message());
      if (!matcher.matches()) {
        return Optional.empty();
      }
      return Optional.of(new SleeperLogEvent(
          line.occurredAt(),
          "SLEEPER_SPAWN",
          integer(matcher.group("svx")),
          integer(matcher.group("svy")),
          integer(matcher.group("svz")),
          integer(matcher.group("x")),
          integer(matcher.group("y")),
          integer(matcher.group("z")),
          integer(matcher.group("chunkX")),
          integer(matcher.group("chunkZ")),
          matcher.group("group"),
          matcher.group("entityClass"),
          integer(matcher.group("count")),
          line.rawLine()));
    } catch (RuntimeException e) {
      return Optional.empty();
    }
  }

  private int integer(String value) {
    return Integer.parseInt(value);
  }
}
