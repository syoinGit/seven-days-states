package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import com.yuki.sevendays_states.entity.T_EntityKillTransaction;
import com.yuki.sevendays_states.entity.T_LevelXpSummaryTransaction;
import com.yuki.sevendays_states.entity.T_PlayerCurrentState;
import com.yuki.sevendays_states.entity.T_PlayerJoinTransaction;
import com.yuki.sevendays_states.entity.T_PlayerLeaveTransaction;
import com.yuki.sevendays_states.entity.T_PlayerPositionTransaction;
import com.yuki.sevendays_states.entity.T_ServerMetric;
import com.yuki.sevendays_states.entity.T_SleeperTransaction;
import com.yuki.sevendays_states.log.dto.EntityKillLogEvent;
import com.yuki.sevendays_states.log.dto.LevelXpSummaryLogEvent;
import com.yuki.sevendays_states.log.dto.ParsedLogLine;
import com.yuki.sevendays_states.log.dto.PlayerJoinLogEvent;
import com.yuki.sevendays_states.log.dto.PlayerLeaveLogEvent;
import com.yuki.sevendays_states.log.dto.PlayerListPositionLogEvent;
import com.yuki.sevendays_states.log.dto.ServerMetricLogEvent;
import com.yuki.sevendays_states.log.dto.SleeperLogEvent;
import com.yuki.sevendays_states.log.parser.EntityKillLogParser;
import com.yuki.sevendays_states.log.parser.GameLogLineParser;
import com.yuki.sevendays_states.log.parser.LevelXpSummaryLogParser;
import com.yuki.sevendays_states.log.parser.PlayerJoinLogParser;
import com.yuki.sevendays_states.log.parser.PlayerLeaveLogParser;
import com.yuki.sevendays_states.log.parser.PlayerListPositionLogParser;
import com.yuki.sevendays_states.log.parser.ServerMetricLogParser;
import com.yuki.sevendays_states.log.parser.SleeperRestoreLogParser;
import com.yuki.sevendays_states.log.parser.SleeperSpawnLogParser;
import com.yuki.sevendays_states.repository.T_EntityKillTransactionRepository;
import com.yuki.sevendays_states.repository.T_LevelXpSummaryTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerCurrentStateRepository;
import com.yuki.sevendays_states.repository.T_PlayerJoinTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerLeaveTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerPositionTransactionRepository;
import com.yuki.sevendays_states.repository.T_ServerMetricRepository;
import com.yuki.sevendays_states.repository.T_SleeperTransactionRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameLogImportService {

  private static final int MAX_PLAYER_POSITION_INFERENCE_DISTANCE = 250;
  private static final int MIN_PLAYER_POSITION_INFERENCE_DISTANCE_ADVANTAGE = 50;

  private final SevenDaysDataProperties properties;
  private final T_PlayerCurrentStateRepository playerCurrentStateRepository;
  private final T_PlayerJoinTransactionRepository playerJoinRepository;
  private final T_PlayerLeaveTransactionRepository playerLeaveRepository;
  private final T_PlayerPositionTransactionRepository playerPositionRepository;
  private final T_EntityKillTransactionRepository entityKillRepository;
  private final T_LevelXpSummaryTransactionRepository levelXpSummaryRepository;
  private final T_SleeperTransactionRepository sleeperRepository;
  private final T_ServerMetricRepository serverMetricRepository;
  private final AtomicBoolean running = new AtomicBoolean(false);

  private final GameLogLineParser lineParser = new GameLogLineParser();
  private final PlayerJoinLogParser playerJoinParser = new PlayerJoinLogParser(lineParser);
  private final PlayerLeaveLogParser playerLeaveParser = new PlayerLeaveLogParser(lineParser);
  private final EntityKillLogParser entityKillParser = new EntityKillLogParser(lineParser);
  private final LevelXpSummaryLogParser levelXpSummaryParser = new LevelXpSummaryLogParser(lineParser);
  private final PlayerListPositionLogParser playerListPositionParser = new PlayerListPositionLogParser(lineParser);
  private final SleeperSpawnLogParser sleeperSpawnParser = new SleeperSpawnLogParser(lineParser);
  private final SleeperRestoreLogParser sleeperRestoreParser = new SleeperRestoreLogParser(lineParser);
  private final ServerMetricLogParser serverMetricParser = new ServerMetricLogParser(lineParser);

  @Transactional
  public GameLogImportResult importLogs() {
    if (!running.compareAndSet(false, true)) {
      log.info("7DTD log import skipped because another import is running.");
      return emptyResult();
    }
    try {
      Path logPath = properties.logPath();
      if (Files.isRegularFile(logPath)) {
        return importLogFile(logPath);
      }
      if (!Files.isDirectory(logPath)) {
        return emptyResult();
      }
      Counter total = new Counter();
      try (Stream<Path> files = Files.list(logPath)) {
        files.filter(Files::isRegularFile)
            .sorted()
            .forEach(path -> total.add(importLogFile(path)));
      }
      GameLogImportResult result = total.toResult();
      log.info("7DTD log import completed. {}", result);
      return result;
    } catch (Exception e) {
      throw new IllegalStateException("7DTD logs cannot be imported: " + properties.logPath(), e);
    } finally {
      running.set(false);
    }
  }

  public GameLogImportResult importLogFile(Path sourceFile) {
    Counter counter = new Counter();
    counter.filesRead++;
    List<String> lines;
    try {
      lines = Files.readAllLines(sourceFile, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("log file cannot be read: " + sourceFile, e);
    }
    String sourceFileName = sourceFileName(sourceFile);
    LogImportContext context = new LogImportContext();
    importLines(sourceFileName, lines, context, counter);
    return counter.toResult();
  }

  @Transactional
  public GameLogImportResult importLogLines(String sourceFileName, List<String> lines) {
    Counter counter = new Counter();
    LogImportContext context = new LogImportContext();
    importLines(sourceFileName, lines, context, counter);
    return counter.toResult();
  }

  public StreamSession openStreamSession(String sourceFileName) {
    return new StreamSession(sourceFileName);
  }

  private void importLines(
      String sourceFileName,
      List<String> lines,
      LogImportContext context,
      Counter counter) {
    for (int i = 0; i < lines.size(); i++) {
      String rawLine = lines.get(i);
      counter.linesRead++;
      try {
        Optional<LevelXpSummaryLogEvent> xp = levelXpSummaryParser.parse(lines, i);
        if (xp.isPresent()) {
          saveLevelXpSummary(sourceFileName, xp.get(), context, counter);
          i += xp.get().consumedLineCount() - 1;
          counter.linesRead += xp.get().consumedLineCount() - 1L;
          continue;
        }
        Optional<PlayerListPositionLogEvent> playerPositions = playerListPositionParser.parse(lines, i);
        if (playerPositions.isPresent()) {
          savePlayerListPositions(sourceFileName, playerPositions.get(), context, counter);
          i += playerPositions.get().consumedLineCount() - 1;
          counter.linesRead += playerPositions.get().consumedLineCount() - 1L;
          continue;
        }
        Optional<ParsedLogLine> parsedLine = lineParser.parse(rawLine);
        if (parsedLine.isEmpty()) {
          counter.malformedLines++;
          continue;
        }
        parseSingleLine(sourceFileName, parsedLine.get(), context, counter);
      } catch (RuntimeException e) {
        counter.malformedLines++;
        log.debug("Malformed or unsupported 7DTD log line skipped: {}", rawLine, e);
      }
    }
  }

  private void parseSingleLine(String sourceFile, ParsedLogLine line, LogImportContext context, Counter counter) {
    Optional<PlayerJoinLogEvent> join = playerJoinParser.parse(line);
    if (join.isPresent()) {
      savePlayerJoin(sourceFile, join.get(), counter);
      context.playerJoined(join.get());
      return;
    }
    Optional<PlayerLeaveLogEvent> leave = playerLeaveParser.parse(line);
    if (leave.isPresent()) {
      savePlayerLeave(sourceFile, leave.get(), counter);
      context.playerLeft(leave.get());
      return;
    }
    Optional<EntityKillLogEvent> kill = entityKillParser.parse(line);
    if (kill.isPresent()) {
      saveEntityKill(sourceFile, kill.get(), counter);
      return;
    }
    Optional<SleeperLogEvent> sleeperSpawn = sleeperSpawnParser.parse(line);
    if (sleeperSpawn.isPresent()) {
      saveSleeper(sourceFile, sleeperSpawn.get(), context, counter);
      return;
    }
    Optional<SleeperLogEvent> sleeperRestore = sleeperRestoreParser.parse(line);
    if (sleeperRestore.isPresent()) {
      saveSleeper(sourceFile, sleeperRestore.get(), context, counter);
      return;
    }
    serverMetricParser.parse(line).ifPresent(metric -> saveServerMetric(sourceFile, metric, counter));
  }

  private void savePlayerJoin(String sourceFile, PlayerJoinLogEvent event, Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), event.rawLine());
    savePlayerPosition(
        sourceFile,
        hash,
        event.occurredAt(),
        event.playerName(),
        event.playerEntityId(),
        event.positionX(),
        event.positionY(),
        event.positionZ(),
        "PLAYER_JOIN",
        "direct_log_position");
    if (playerJoinRepository.existsBySourceLogHash(hash)) {
      return;
    }
    T_PlayerJoinTransaction row = new T_PlayerJoinTransaction();
    row.setOccurredAt(event.occurredAt());
    row.setPlayerName(event.playerName());
    row.setPlayerEntityId(event.playerEntityId());
    row.setPlatformId(event.platformId());
    row.setCrossPlatformId(event.crossPlatformId());
    row.setPositionX(event.positionX());
    row.setPositionY(event.positionY());
    row.setPositionZ(event.positionZ());
    row.setJoinReason(event.reason());
    row.setClientNumber(event.clientNumber());
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    playerJoinRepository.save(row);
    counter.playerJoins++;
  }

  private void savePlayerLeave(String sourceFile, PlayerLeaveLogEvent event, Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), event.rawLine());
    if (playerLeaveRepository.existsBySourceLogHash(hash)) {
      return;
    }
    T_PlayerLeaveTransaction row = new T_PlayerLeaveTransaction();
    row.setOccurredAt(event.occurredAt());
    row.setPlayerName(event.playerName());
    row.setPlayerEntityId(event.playerEntityId());
    row.setPlatformId(event.platformId());
    row.setCrossPlatformId(event.crossPlatformId());
    row.setClientNumber(event.clientNumber());
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    playerLeaveRepository.save(row);
    counter.playerLeaves++;
  }

  private void saveEntityKill(String sourceFile, EntityKillLogEvent event, Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), event.rawLine());
    if (entityKillRepository.existsBySourceLogHash(hash)) {
      return;
    }
    T_EntityKillTransaction row = new T_EntityKillTransaction();
    row.setOccurredAt(event.occurredAt());
    row.setPlayerName(event.playerName());
    row.setPlayerEntityId(event.playerEntityId());
    row.setTargetEntityType(event.targetEntityType());
    row.setTargetEntityId(event.targetEntityId());
    playerCurrentStateRepository.findById(event.playerEntityId()).filter(currentState ->
        isFreshCurrentState(currentState, event.occurredAt())).ifPresent(currentState -> {
      row.setPlayerPositionX(currentState.getPositionX());
      row.setPlayerPositionY(currentState.getPositionY());
      row.setPlayerPositionZ(currentState.getPositionZ());
      row.setPlayerCurrentStateUpdatedAt(currentState.getLastUpdated());
    });
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    entityKillRepository.save(row);
    counter.entityKills++;
  }

  private void saveLevelXpSummary(
      String sourceFile,
      LevelXpSummaryLogEvent event,
      LogImportContext context,
      Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), String.join("\n", event.rawLines()));
    if (levelXpSummaryRepository.existsBySourceLogHash(hash)) {
      return;
    }
    T_LevelXpSummaryTransaction row = new T_LevelXpSummaryTransaction();
    row.setOccurredAt(event.occurredAt());
    row.setXpFromLoot(event.xpFromLoot());
    row.setXpFromHarvesting(event.xpFromHarvesting());
    row.setXpFromKill(event.xpFromKill());
    row.setXpTotal(event.xpTotal());
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    levelXpSummaryRepository.save(row);
    counter.levelXpSummaries++;
  }

  private void saveSleeper(
      String sourceFile,
      SleeperLogEvent event,
      LogImportContext context,
      Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), event.rawLine());
    Optional<ActivePlayer> inferredPlayer = inferNearestCurrentStatePlayer(
        event.positionX(), event.positionZ(), event.occurredAt())
        .or(() -> context.inferNearestActivePlayer(event.positionX(), event.positionZ()));
    Optional<T_SleeperTransaction> existing = sleeperRepository.findBySourceLogHash(hash);
    if (existing.isPresent()) {
      updateSleeperInference(existing.get(), inferredPlayer);
      inferredPlayer.ifPresent(player -> {
        if (player.trustedForPositionUpdate()) {
          savePlayerPosition(
              sourceFile,
              hash,
              event.occurredAt(),
              player.playerName(),
              player.playerEntityId(),
              event.positionX(),
              event.positionY(),
              event.positionZ(),
              "SLEEPER_INFERRED",
              player.inferenceMethod());
          context.updatePlayerPosition(player, event.positionX(), event.positionY(), event.positionZ());
        }
      });
      return;
    }
    T_SleeperTransaction row = new T_SleeperTransaction();
    row.setOccurredAt(event.occurredAt());
    row.setTransactionType(event.transactionType());
    row.setSleeperVolumeX(event.sleeperVolumeX());
    row.setSleeperVolumeY(event.sleeperVolumeY());
    row.setSleeperVolumeZ(event.sleeperVolumeZ());
    row.setPositionX(event.positionX());
    row.setPositionY(event.positionY());
    row.setPositionZ(event.positionZ());
    row.setChunkX(event.chunkX());
    row.setChunkZ(event.chunkZ());
    row.setSleeperGroup(event.sleeperGroup());
    row.setEntityClass(event.entityClass());
    row.setEntityCount(event.entityCount());
    inferredPlayer.ifPresent(player -> {
      row.setPlayerName(player.playerName());
      row.setPlayerEntityId(player.playerEntityId());
      row.setPlayerInferenceMethod(player.inferenceMethod());
      row.setPlayerPositionX(player.x());
      row.setPlayerPositionY(player.y());
      row.setPlayerPositionZ(player.z());
      row.setPlayerCurrentStateUpdatedAt(player.positionUpdatedAt());
    });
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    sleeperRepository.save(row);
    inferredPlayer.ifPresent(player -> {
      if (player.trustedForPositionUpdate()) {
        savePlayerPosition(
            sourceFile,
            hash,
            event.occurredAt(),
            player.playerName(),
            player.playerEntityId(),
            event.positionX(),
            event.positionY(),
            event.positionZ(),
            "SLEEPER_INFERRED",
            player.inferenceMethod());
        context.updatePlayerPosition(player, event.positionX(), event.positionY(), event.positionZ());
      }
    });
    if ("SLEEPER_RESTORE".equals(event.transactionType())) {
      counter.sleeperRestores++;
    } else {
      counter.sleeperSpawns++;
    }
  }

  private void updateSleeperInference(
      T_SleeperTransaction row,
      Optional<ActivePlayer> inferredPlayer) {
    if (row.getPlayerName() != null || inferredPlayer.isEmpty()) {
      return;
    }
    ActivePlayer player = inferredPlayer.get();
    row.setPlayerName(player.playerName());
    row.setPlayerEntityId(player.playerEntityId());
    row.setPlayerInferenceMethod(player.inferenceMethod());
    row.setPlayerPositionX(player.x());
    row.setPlayerPositionY(player.y());
    row.setPlayerPositionZ(player.z());
    row.setPlayerCurrentStateUpdatedAt(player.positionUpdatedAt());
    sleeperRepository.save(row);
  }

  private Optional<ActivePlayer> inferNearestCurrentStatePlayer(int x, int z, OffsetDateTime occurredAt) {
    List<ActivePlayerDistance> distances = playerCurrentStateRepository.findByOnlineTrue().stream()
        .filter(player -> player.getPositionX() != null && player.getPositionZ() != null)
        .filter(player -> isFreshCurrentState(player, occurredAt))
        .map(player -> new ActivePlayerDistance(new ActivePlayer(
            player.getPlayerName(),
            player.getPlayerEntityId(),
            player.getLastUpdated(),
            player.getPositionX(),
            player.getPositionY(),
            player.getPositionZ(),
            "nearest_current_state_position",
            false,
            player.getLastUpdated()), distance(player.getPositionX(), player.getPositionZ(), x, z)))
        .sorted(Comparator.comparingDouble(ActivePlayerDistance::distance))
        .toList();
    if (distances.isEmpty()) {
      return Optional.empty();
    }
    ActivePlayerDistance nearest = distances.getFirst();
    if (nearest.distance() > MAX_PLAYER_POSITION_INFERENCE_DISTANCE) {
      return Optional.empty();
    }
    if (distances.size() > 1
        && distances.get(1).distance() - nearest.distance() < MIN_PLAYER_POSITION_INFERENCE_DISTANCE_ADVANTAGE) {
      return Optional.empty();
    }
    return Optional.of(nearest.player());
  }

  private boolean isFreshCurrentState(T_PlayerCurrentState player, OffsetDateTime referenceTime) {
    if (player.getLastUpdated() == null) {
      return false;
    }
    Duration maxAge = Duration.ofSeconds(properties.transaction().currentStateMaxAgeSeconds());
    return !player.getLastUpdated().isBefore(referenceTime.minus(maxAge));
  }

  private void savePlayerListPositions(
      String sourceFile,
      PlayerListPositionLogEvent event,
      LogImportContext context,
      Counter counter) {
    for (PlayerListPositionLogEvent.PlayerPosition player : event.players()) {
      String hash = lineHash(sourceFile, event.occurredAt() + "|LP|" + player.playerEntityId(), player.rawLine());
      savePlayerPosition(
          sourceFile,
          hash,
          event.occurredAt(),
          player.playerName(),
          player.playerEntityId(),
          player.positionX(),
          player.positionY(),
          player.positionZ(),
          "LP_COMMAND",
          "direct_telnet_lp");
      context.playerPositionObserved(player, event.occurredAt());
      upsertPlayerCurrentState(event.occurredAt(), player);
    }
    counter.playerListPositions += event.players().size();
    markMissingCurrentStatePlayersOffline(event.occurredAt(), event.players());
  }

  private void upsertPlayerCurrentState(
      OffsetDateTime occurredAt,
      PlayerListPositionLogEvent.PlayerPosition player) {
    T_PlayerCurrentState row = findCurrentStateByEntityOrExternalId(player)
        .orElseGet(T_PlayerCurrentState::new);
    if (row.getPlayerEntityId() != null && !row.getPlayerEntityId().equals(player.playerEntityId())) {
      playerCurrentStateRepository.delete(row);
      playerCurrentStateRepository.flush();
      row = new T_PlayerCurrentState();
    }
    row.setPlayerEntityId(player.playerEntityId());
    row.setPlayerName(player.playerName());
    row.setPositionX(player.positionX());
    row.setPositionY(player.positionY());
    row.setPositionZ(player.positionZ());
    row.setRotationX(player.rotationX());
    row.setRotationY(player.rotationY());
    row.setRotationZ(player.rotationZ());
    row.setHealth(player.health());
    row.setDeaths(player.deaths());
    row.setZombies(player.zombies());
    row.setPlayers(player.players());
    row.setScore(player.score());
    row.setLevel(player.level());
    row.setPlatformId(player.platformId());
    row.setCrossPlatformId(player.crossPlatformId());
    row.setPing(player.ping());
    row.setOnline(true);
    row.setLastUpdated(occurredAt);
    playerCurrentStateRepository.save(row);
  }

  private Optional<T_PlayerCurrentState> findCurrentStateByEntityOrExternalId(
      PlayerListPositionLogEvent.PlayerPosition player) {
    Optional<T_PlayerCurrentState> byEntity = playerCurrentStateRepository.findById(player.playerEntityId());
    if (byEntity.isPresent()) {
      return byEntity;
    }
    Set<String> crossPlatformIds = externalIdVariants(player.crossPlatformId(), "EOS");
    if (!crossPlatformIds.isEmpty()) {
      Optional<T_PlayerCurrentState> byCrossPlatformId = newestCurrentState(
          playerCurrentStateRepository.findByCrossPlatformIdIn(crossPlatformIds));
      if (byCrossPlatformId.isPresent()) {
        return byCrossPlatformId;
      }
    }
    Set<String> platformIds = externalIdVariants(player.platformId(), "Steam");
    if (!platformIds.isEmpty()) {
      return newestCurrentState(playerCurrentStateRepository.findByPlatformIdIn(platformIds));
    }
    return Optional.empty();
  }

  private Optional<T_PlayerCurrentState> newestCurrentState(List<T_PlayerCurrentState> states) {
    return states.stream()
        .max(Comparator.comparing(T_PlayerCurrentState::getLastUpdated, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(T_PlayerCurrentState::getPlayerEntityId, Comparator.nullsFirst(Comparator.naturalOrder())));
  }

  private Set<String> externalIdVariants(String rawValue, String prefix) {
    if (rawValue == null || rawValue.isBlank()) {
      return Set.of();
    }
    String trimmed = rawValue.trim();
    String bare = trimmed.startsWith(prefix + "_") ? trimmed.substring(prefix.length() + 1) : trimmed;
    Set<String> values = new LinkedHashSet<>();
    values.add(trimmed);
    values.add(prefix + "_" + bare);
    values.add(bare);
    return values;
  }

  private void markMissingCurrentStatePlayersOffline(
      OffsetDateTime occurredAt,
      List<PlayerListPositionLogEvent.PlayerPosition> observedPlayers) {
    Set<Integer> onlinePlayerIds = new HashSet<>();
    for (PlayerListPositionLogEvent.PlayerPosition player : observedPlayers) {
      onlinePlayerIds.add(player.playerEntityId());
    }
    List<T_PlayerCurrentState> missingPlayers = onlinePlayerIds.isEmpty()
        ? playerCurrentStateRepository.findByOnlineTrue()
        : playerCurrentStateRepository.findByOnlineTrueAndPlayerEntityIdNotIn(onlinePlayerIds);
    for (T_PlayerCurrentState player : missingPlayers) {
      player.setOnline(false);
      player.setLastUpdated(occurredAt);
    }
    playerCurrentStateRepository.saveAll(missingPlayers);
  }

  private void savePlayerPosition(
      String sourceFile,
      String sourceLogHash,
      OffsetDateTime occurredAt,
      String playerName,
      int playerEntityId,
      int positionX,
      Integer positionY,
      int positionZ,
      String positionSourceType,
      String inferenceMethod) {
    String sourceEventHash = lineHash(sourceFile, occurredAt + "|" + positionSourceType, sourceLogHash);
    if (playerPositionRepository.existsBySourceEventHash(sourceEventHash)) {
      return;
    }
    T_PlayerPositionTransaction row = new T_PlayerPositionTransaction();
    row.setOccurredAt(occurredAt);
    row.setPlayerName(playerName);
    row.setPlayerEntityId(playerEntityId);
    row.setPositionX(positionX);
    row.setPositionY(positionY);
    row.setPositionZ(positionZ);
    row.setPositionSourceType(positionSourceType);
    row.setInferenceMethod(inferenceMethod);
    row.setSourceEventHash(sourceEventHash);
    row.setSourceFile(sourceFile);
    playerPositionRepository.save(row);
  }

  private void saveServerMetric(String sourceFile, ServerMetricLogEvent event, Counter counter) {
    String hash = lineHash(sourceFile, event.occurredAt().toString(), event.rawLine());
    if (serverMetricRepository.existsBySourceLogHash(hash)) {
      return;
    }
    if (!shouldStoreServerMetric(event)) {
      counter.skippedServerMetrics++;
      return;
    }
    T_ServerMetric row = new T_ServerMetric();
    row.setOccurredAt(event.occurredAt());
    row.setUptimeMinutes(event.uptimeMinutes());
    row.setFps(event.fps());
    row.setHeapMb(event.heapMb());
    row.setMaxHeapMb(event.maxHeapMb());
    row.setChunks(event.chunks());
    row.setCgo(event.cgo());
    row.setPlayerCount(event.playerCount());
    row.setZombieCount(event.zombieCount());
    row.setEntityCount(event.entityCount());
    row.setEntityCountDetail(event.entityCountDetail());
    row.setItemCount(event.itemCount());
    row.setCo(event.co());
    row.setRssMb(event.rssMb());
    row.setSourceFile(sourceFile);
    row.setSourceLogHash(hash);
    serverMetricRepository.save(row);
    counter.serverMetrics++;
  }

  private boolean shouldStoreServerMetric(ServerMetricLogEvent event) {
    return serverMetricRepository.findTopByOrderByOccurredAtDesc()
        .map(last -> {
          Duration interval = Duration.ofMinutes(properties.log().serverMetricIntervalMinutes());
          return !event.occurredAt().isBefore(last.getOccurredAt().plus(interval));
        })
        .orElse(true);
  }

  private String sourceFileName(Path sourceFile) {
    Path logRoot = properties.logPath().toAbsolutePath().normalize();
    Path absolute = sourceFile.toAbsolutePath().normalize();
    if (absolute.startsWith(logRoot)) {
      return logRoot.relativize(absolute).toString();
    }
    return absolute.toString();
  }

  private String lineHash(String sourceFile, String occurredAt, String content) {
    return sha256(sourceFile + "|" + occurredAt + "|" + content);
  }

  private String sha256(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 cannot be calculated", e);
    }
  }

  private GameLogImportResult emptyResult() {
    return new Counter().toResult();
  }

  public class StreamSession {
    private final String sourceFileName;
    private final LogImportContext context = new LogImportContext();
    private final List<String> pendingLines = new ArrayList<>();

    private StreamSession(String sourceFileName) {
      this.sourceFileName = sourceFileName;
    }

    public synchronized GameLogImportResult acceptLine(String rawLine) {
      Counter counter = new Counter();
      if (pendingLines.isEmpty()) {
        pendingLines.add(rawLine);
        return counter.toResult();
      }
      if (startsNextEvent(rawLine)) {
        flushInto(counter);
      }
      pendingLines.add(rawLine);
      if (rawLine.startsWith("Total of ") && rawLine.endsWith(" in the game")) {
        flushInto(counter);
      }
      return counter.toResult();
    }

    public synchronized GameLogImportResult flush() {
      Counter counter = new Counter();
      flushInto(counter);
      return counter.toResult();
    }

    private boolean startsNextEvent(String rawLine) {
      if (lineParser.parse(rawLine).isEmpty()) {
        return false;
      }
      if (pendingLines.isEmpty()) {
        return false;
      }
      String first = pendingLines.getFirst();
      if (levelXpSummaryParser.matches(first) && rawLine.contains("CVarLogValue: $xpFrom")) {
        return false;
      }
      return true;
    }

    private void flushInto(Counter counter) {
      if (pendingLines.isEmpty()) {
        return;
      }
      importLines(sourceFileName, List.copyOf(pendingLines), context, counter);
      pendingLines.clear();
    }
  }

  private static class Counter {
    private long filesRead;
    private long linesRead;
    private long playerJoins;
    private long playerLeaves;
    private long playerListPositions;
    private long entityKills;
    private long levelXpSummaries;
    private long sleeperSpawns;
    private long sleeperRestores;
    private long serverMetrics;
    private long skippedServerMetrics;
    private long malformedLines;

    private void add(GameLogImportResult result) {
      filesRead += result.filesRead();
      linesRead += result.linesRead();
      playerJoins += result.playerJoins();
      playerLeaves += result.playerLeaves();
      playerListPositions += result.playerListPositions();
      entityKills += result.entityKills();
      levelXpSummaries += result.levelXpSummaries();
      sleeperSpawns += result.sleeperSpawns();
      sleeperRestores += result.sleeperRestores();
      serverMetrics += result.serverMetrics();
      skippedServerMetrics += result.skippedServerMetrics();
      malformedLines += result.malformedLines();
    }

    private GameLogImportResult toResult() {
      return new GameLogImportResult(
          filesRead,
          linesRead,
          playerJoins,
          playerLeaves,
          playerListPositions,
          entityKills,
          levelXpSummaries,
          sleeperSpawns,
          sleeperRestores,
          serverMetrics,
          skippedServerMetrics,
          malformedLines);
    }
  }

  private static class LogImportContext {
    private final Map<Integer, ActivePlayer> activePlayers = new HashMap<>();

    private void playerJoined(PlayerJoinLogEvent event) {
      activePlayers.put(event.playerEntityId(), new ActivePlayer(
          event.playerName(),
          event.playerEntityId(),
          event.occurredAt(),
          event.positionX(),
          event.positionY(),
          event.positionZ(),
          "join_position",
          true,
          event.occurredAt()));
    }

    private void playerLeft(PlayerLeaveLogEvent event) {
      activePlayers.remove(event.playerEntityId());
    }

    private void playerPositionObserved(
        PlayerListPositionLogEvent.PlayerPosition event,
        OffsetDateTime occurredAt) {
      activePlayers.put(event.playerEntityId(), new ActivePlayer(
          event.playerName(),
          event.playerEntityId(),
          occurredAt,
          event.positionX(),
          event.positionY(),
          event.positionZ(),
          "direct_telnet_lp",
          true,
          occurredAt));
    }

    private Optional<ActivePlayer> inferSingleActivePlayer() {
      if (activePlayers.size() != 1) {
        return Optional.empty();
      }
      return activePlayers.values().stream().findFirst();
    }

    private Optional<ActivePlayer> inferNearestActivePlayer(int x, int z) {
      if (activePlayers.isEmpty()) {
        return Optional.empty();
      }
      if (activePlayers.size() == 1) {
        return inferSingleActivePlayer()
            .map(player -> player.withInferenceMethod("single_active_player_session", true));
      }
      List<ActivePlayerDistance> distances = activePlayers.values().stream()
          .filter(player -> player.x() != null && player.z() != null)
          .map(player -> new ActivePlayerDistance(player, distance(player, x, z)))
          .sorted(Comparator.comparingDouble(ActivePlayerDistance::distance))
          .toList();
      if (distances.isEmpty()) {
        return Optional.empty();
      }
      ActivePlayerDistance nearest = distances.getFirst();
      if (nearest.distance() > MAX_PLAYER_POSITION_INFERENCE_DISTANCE) {
        return Optional.empty();
      }
      if (distances.size() > 1
          && distances.get(1).distance() - nearest.distance() < MIN_PLAYER_POSITION_INFERENCE_DISTANCE_ADVANTAGE) {
        return Optional.empty();
      }
      return Optional.of(nearest.player()
          .withInferenceMethod("nearest_active_player_latest_position", false));
    }

    private void updatePlayerPosition(ActivePlayer player, int x, Integer y, int z) {
      activePlayers.computeIfPresent(player.playerEntityId(), (key, current) -> current.withPosition(x, y, z));
    }

    private static double distance(ActivePlayer player, int x, int z) {
      return GameLogImportService.distance(player.x(), player.z(), x, z);
    }
  }

  private static double distance(int fromX, int fromZ, int toX, int toZ) {
    long dx = (long) fromX - toX;
    long dz = (long) fromZ - toZ;
    return Math.sqrt(dx * dx + dz * dz);
  }

  private record ActivePlayer(
      String playerName,
      int playerEntityId,
      OffsetDateTime joinedAt,
      Integer x,
      Integer y,
      Integer z,
      String inferenceMethod,
      boolean trustedForPositionUpdate,
      OffsetDateTime positionUpdatedAt) {

    private ActivePlayer withInferenceMethod(String method, boolean trusted) {
      return new ActivePlayer(playerName, playerEntityId, joinedAt, x, y, z, method, trusted, positionUpdatedAt);
    }

    private ActivePlayer withPosition(Integer newX, Integer newY, Integer newZ) {
      return new ActivePlayer(
          playerName,
          playerEntityId,
          joinedAt,
          newX,
          newY,
          newZ,
          inferenceMethod,
          trustedForPositionUpdate,
          OffsetDateTime.now());
    }
  }

  private record ActivePlayerDistance(ActivePlayer player, double distance) {
  }
}
