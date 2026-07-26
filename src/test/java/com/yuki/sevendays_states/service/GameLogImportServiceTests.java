package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.repository.T_EntityKillTransactionRepository;
import com.yuki.sevendays_states.repository.T_LevelXpSummaryTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerCurrentStateRepository;
import com.yuki.sevendays_states.repository.T_PlayerJoinTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerLeaveTransactionRepository;
import com.yuki.sevendays_states.repository.T_PlayerPositionTransactionRepository;
import com.yuki.sevendays_states.repository.T_ServerMetricRepository;
import com.yuki.sevendays_states.repository.T_SleeperTransactionRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_log;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false",
    "app.sevendays.log.server-metric-interval-minutes=60"
})
class GameLogImportServiceTests {

  @TempDir
  Path tempDir;

  @Autowired
  private GameLogImportService logImportService;

  @Autowired
  private T_PlayerJoinTransactionRepository playerJoinRepository;

  @Autowired
  private T_PlayerCurrentStateRepository playerCurrentStateRepository;

  @Autowired
  private T_PlayerLeaveTransactionRepository playerLeaveRepository;

  @Autowired
  private T_PlayerPositionTransactionRepository playerPositionRepository;

  @Autowired
  private T_EntityKillTransactionRepository entityKillRepository;

  @Autowired
  private T_LevelXpSummaryTransactionRepository levelXpSummaryRepository;

  @Autowired
  private T_SleeperTransactionRepository sleeperRepository;

  @Autowired
  private T_ServerMetricRepository serverMetricRepository;

  @BeforeEach
  void deleteTransactions() {
    playerJoinRepository.deleteAll();
    playerCurrentStateRepository.deleteAll();
    playerLeaveRepository.deleteAll();
    playerPositionRepository.deleteAll();
    entityKillRepository.deleteAll();
    levelXpSummaryRepository.deleteAll();
    sleeperRepository.deleteAll();
    serverMetricRepository.deleteAll();
  }

  @Test
  void importsTargetLogsAndSkipsMalformedLines() throws Exception {
    Path log = writeLog("""
        malformed log
        2026-07-26T08:18:02 1147.256 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: -162, 52, -857): EntityID=331, PltfmId='Steam_76561198123350583', CrossId='EOS_xxx', OwnerID='Steam_xxx', PlayerName='DDD烈火王テムジン', ClientNumber='3'
        2026-07-26T08:41:32 2557.179 INF Player disconnected: EntityID=331, PltfmId='Steam_76561198123350583', CrossId='EOS_xxx', OwnerID='Steam_xxx', PlayerName='DDD烈火王テムジン', ClientNumber='3'
        2026-07-26T08:22:51 1436.863 INF Entity zombieBusinessMan 347 killed by DDD烈火王テムジン 331
        2026-07-26T08:36:07 2233.109 INF MinEventLogMessage: XP gained during the last level:
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromLootThisLevel == 16
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromHarvestingThisLevel == 1014
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromKillThisLevel == 3950
        2026-07-26T08:24:51 1556.661 INF 1544.871 SleeperVolume -546, 55, -577: Spawning -538, 55, -570 (-34, -36), group 'sleeperHordeStageGS2', class zombieBoe, count 5
        2026-07-26T08:21:24 1349.173 INF 1337.678 SleeperVolume -151, 38, -767: Restoring -144, 39, -765 (-9, -48) 'zombieSteveCrawler', count 0
        """);

    GameLogImportResult result = logImportService.importLogFile(log);

    assertThat(result.malformedLines()).isEqualTo(1);
    assertThat(result.playerJoins()).isEqualTo(1);
    assertThat(result.playerLeaves()).isEqualTo(1);
    assertThat(result.entityKills()).isEqualTo(1);
    assertThat(result.levelXpSummaries()).isEqualTo(1);
    assertThat(result.sleeperSpawns()).isEqualTo(1);
    assertThat(result.sleeperRestores()).isEqualTo(1);
    assertThat(playerJoinRepository.count()).isEqualTo(1);
    assertThat(playerLeaveRepository.count()).isEqualTo(1);
    assertThat(playerPositionRepository.count()).isEqualTo(1);
    assertThat(entityKillRepository.count()).isEqualTo(1);
    assertThat(levelXpSummaryRepository.count()).isEqualTo(1);
    assertThat(sleeperRepository.count()).isEqualTo(2);
  }

  @Test
  void skipsServerMetricWithinIntervalAndStoresAfterInterval() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:00:00 1000.000 INF Time: 10.00m FPS: 20.00 Heap: 1000.0MB Max: 1100.0MB Chunks: 10 CGO: 1 Ply: 1 Zom: 0 Ent: 1 (2) Items: 0 CO: 1 RSS: 2000.0MB
        2026-07-26T08:30:00 2800.000 INF Time: 40.00m FPS: 20.00 Heap: 1001.0MB Max: 1100.0MB Chunks: 11 CGO: 1 Ply: 1 Zom: 0 Ent: 1 (2) Items: 0 CO: 1 RSS: 2001.0MB
        2026-07-26T09:00:00 4600.000 INF Time: 70.00m FPS: 20.00 Heap: 1002.0MB Max: 1100.0MB Chunks: 12 CGO: 1 Ply: 1 Zom: 0 Ent: 1 (2) Items: 0 CO: 1 RSS: 2002.0MB
        """);

    GameLogImportResult result = logImportService.importLogFile(log);

    assertThat(result.serverMetrics()).isEqualTo(2);
    assertThat(result.skippedServerMetrics()).isEqualTo(1);
    assertThat(serverMetricRepository.count()).isEqualTo(2);
  }

  @Test
  void doesNotInferPlayerForLevelXpSummary() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:18:02 1147.256 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: -162, 52, -857): EntityID=331, PltfmId='Steam_76561198123350583', CrossId='EOS_xxx', OwnerID='Steam_xxx', PlayerName='DDD烈火王テムジン', ClientNumber='3'
        2026-07-26T08:36:07 2233.109 INF MinEventLogMessage: XP gained during the last level:
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromLootThisLevel == 16
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromHarvestingThisLevel == 1014
        2026-07-26T08:36:07 2233.109 INF CVarLogValue: $xpFromKillThisLevel == 3950
        """);

    logImportService.importLogFile(log);

    assertThat(levelXpSummaryRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.getPlayerEntityId() + ":" + row.getPlayerInferenceMethod() + ":" + row.getXpTotal())
        .containsExactly("null:null:null:4980");
  }

  @Test
  void importingSameLogTwiceDoesNotCreateDuplicates() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:18:02 1147.256 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: -162, 52, -857): EntityID=331, PltfmId='Steam_76561198123350583', CrossId='EOS_xxx', OwnerID='Steam_xxx', PlayerName='DDD烈火王テムジン', ClientNumber='3'
        2026-07-26T08:22:51 1436.863 INF Entity zombieBusinessMan 347 killed by DDD烈火王テムジン 331
        """);

    GameLogImportResult first = logImportService.importLogFile(log);
    GameLogImportResult second = logImportService.importLogFile(log);

    assertThat(first.playerJoins()).isEqualTo(1);
    assertThat(first.entityKills()).isEqualTo(1);
    assertThat(second.playerJoins()).isZero();
    assertThat(second.entityKills()).isZero();
    assertThat(playerJoinRepository.count()).isEqualTo(1);
    assertThat(playerPositionRepository.count()).isEqualTo(1);
    assertThat(entityKillRepository.count()).isEqualTo(1);
  }

  @Test
  void assignsSleeperToNearestActivePlayerWhenMultiplePlayersAreOnline() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:00:00 1000.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 0, 50, 0): EntityID=101, PltfmId='Steam_a', CrossId='EOS_a', OwnerID='Steam_a', PlayerName='PlayerA', ClientNumber='1'
        2026-07-26T08:00:10 1010.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 1000, 50, 1000): EntityID=202, PltfmId='Steam_b', CrossId='EOS_b', OwnerID='Steam_b', PlayerName='PlayerB', ClientNumber='2'
        2026-07-26T08:01:00 1060.000 INF 1060.000 SleeperVolume 110, 50, 105: Spawning 120, 50, 100 (7, 6), group 'sleeperHordeStageGS2', class zombieBoe, count 1
        2026-07-26T08:02:00 1120.000 INF 1120.000 SleeperVolume 900, 50, 890: Spawning 910, 50, 900 (56, 56), group 'sleeperHordeStageGS2', class zombieNurse, count 1
        2026-07-26T08:03:00 1180.000 INF 1180.000 SleeperVolume 180, 50, 170: Spawning 190, 50, 180 (11, 11), group 'sleeperHordeStageGS2', class zombieSteve, count 1
        """);

    logImportService.importLogFile(log);

    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getEntityClass() + ":" + row.getPlayerName() + ":" + row.getPlayerInferenceMethod())
        .containsExactly(
            "zombieBoe:PlayerA:nearest_active_player_latest_position",
            "zombieNurse:PlayerB:nearest_active_player_latest_position",
            "zombieSteve:null:null");
    assertThat(playerPositionRepository.findAll())
        .extracting(row -> row.getPositionSourceType() + ":" + row.getPlayerName() + ":" + row.getPositionX() + ":" + row.getPositionZ())
        .containsExactly(
            "PLAYER_JOIN:PlayerA:0:0",
            "PLAYER_JOIN:PlayerB:1000:1000");
  }

  @Test
  void doesNotAssignSleeperWhenMultipleActivePlayersAreTooCloseToInfer() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:00:00 1000.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 0, 50, 0): EntityID=101, PltfmId='Steam_a', CrossId='EOS_a', OwnerID='Steam_a', PlayerName='PlayerA', ClientNumber='1'
        2026-07-26T08:00:10 1010.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 20, 50, 0): EntityID=202, PltfmId='Steam_b', CrossId='EOS_b', OwnerID='Steam_b', PlayerName='PlayerB', ClientNumber='2'
        2026-07-26T08:01:00 1060.000 INF 1060.000 SleeperVolume 10, 50, 0: Spawning 10, 50, 0 (0, 0), group 'sleeperHordeStageGS2', class zombieBoe, count 1
        """);

    logImportService.importLogFile(log);

    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getEntityClass() + ":" + row.getPlayerName())
        .containsExactly("zombieBoe:null");
    assertThat(playerPositionRepository.findAll())
        .extracting(row -> row.getPositionSourceType() + ":" + row.getPlayerName())
        .containsExactly(
            "PLAYER_JOIN:PlayerA",
            "PLAYER_JOIN:PlayerB");
  }

  @Test
  void recordsSleeperPositionWhenOnlyOnePlayerIsOnline() throws Exception {
    Path log = writeLog("""
        2026-07-26T08:00:00 1000.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 0, 50, 0): EntityID=101, PltfmId='Steam_a', CrossId='EOS_a', OwnerID='Steam_a', PlayerName='PlayerA', ClientNumber='1'
        2026-07-26T08:01:00 1060.000 INF 1060.000 SleeperVolume 110, 50, 105: Spawning 120, 50, 100 (7, 6), group 'sleeperHordeStageGS2', class zombieBoe, count 1
        """);

    logImportService.importLogFile(log);

    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getEntityClass() + ":" + row.getPlayerName() + ":" + row.getPlayerInferenceMethod())
        .containsExactly("zombieBoe:PlayerA:single_active_player_session");
    assertThat(playerPositionRepository.findAll())
        .extracting(row -> row.getPositionSourceType() + ":" + row.getPlayerName() + ":" + row.getPositionX() + ":" + row.getPositionZ())
        .containsExactly(
            "PLAYER_JOIN:PlayerA:0:0",
            "SLEEPER_INFERRED:PlayerA:120:100");
  }

  @Test
  void usesPlayerListCommandPositionAsDirectCurrentPosition() throws Exception {
    Path log = writeLog("""
        2026-07-26T10:50:00 10200.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 0, 50, 0): EntityID=101, PltfmId='Steam_a', CrossId='EOS_a', OwnerID='Steam_a', PlayerName='PlayerA', ClientNumber='1'
        2026-07-26T10:50:01 10201.000 INF PlayerSpawnedInWorld (reason: JoinMultiplayer, position: 0, 50, 0): EntityID=202, PltfmId='Steam_b', CrossId='EOS_b', OwnerID='Steam_b', PlayerName='PlayerB', ClientNumber='2'
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7
        1. id=202, PlayerB, pos=(0.0, 50.0, 0.0), rot=(0.0, 0.0, 0.0), remote=True, health=100, deaths=0, zombies=0, players=0, score=0, level=1, pltfmid=Steam_b, crossid=EOS_b, ip=10.0.0.2, ping=7
        Total of 2 in the game
        2026-07-26T10:54:00 10500.000 INF 10500.000 SleeperVolume -540, 48, -450: Spawning -530, 48, -440 (-33, -27), group 'sleeperHordeStageGS2', class zombieBoe, count 1
        """);

    logImportService.importLogFile(log);

    assertThat(playerPositionRepository.findAll())
        .extracting(row -> row.getPositionSourceType() + ":" + row.getPlayerName() + ":" + row.getPositionX() + ":" + row.getPositionZ())
        .containsExactly(
            "PLAYER_JOIN:PlayerA:0:0",
            "PLAYER_JOIN:PlayerB:0:0",
            "LP_COMMAND:PlayerA:-532:-446",
            "LP_COMMAND:PlayerB:0:0");
    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getEntityClass() + ":" + row.getPlayerName() + ":" + row.getPlayerInferenceMethod())
        .containsExactly("zombieBoe:PlayerA:nearest_current_state_position");
    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.getPlayerPositionX() + ":" + row.getPlayerPositionZ())
        .containsExactly("PlayerA:-532:-446");
    assertThat(playerCurrentStateRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.getPositionX() + ":" + row.getPositionZ() + ":" + row.getHealth() + ":" + row.getLevel())
        .containsExactlyInAnyOrder(
            "PlayerA:-532:-446:101:2",
            "PlayerB:0:0:100:1");
  }

  @Test
  void assignsSleeperToNearestPlayerFromCurrentStateWhenLogContextHasNoPlayers() throws Exception {
    Path lpLog = writeLog("""
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7
        1. id=202, PlayerB, pos=(0.0, 50.0, 0.0), rot=(0.0, 0.0, 0.0), remote=True, health=100, deaths=0, zombies=0, players=0, score=0, level=1, pltfmid=Steam_b, crossid=EOS_b, ip=10.0.0.2, ping=7
        Total of 2 in the game
        """);
    Path sleeperLog = tempDir.resolve("sleeper-log");
    Files.writeString(sleeperLog, """
        2026-07-26T10:54:00 10500.000 INF 10500.000 SleeperVolume -540, 48, -450: Spawning -530, 48, -440 (-33, -27), group 'sleeperHordeStageGS2', class zombieBoe, count 1
        """);

    logImportService.importLogFile(lpLog);
    logImportService.importLogFile(sleeperLog);

    assertThat(sleeperRepository.findAll())
        .extracting(row -> row.getEntityClass() + ":" + row.getPlayerName() + ":" + row.getPlayerInferenceMethod()
            + ":" + row.getPlayerPositionX() + ":" + row.getPlayerPositionZ())
        .containsExactly("zombieBoe:PlayerA:nearest_current_state_position:-532:-446");
  }

  @Test
  void copiesCurrentStatePositionWhenEntityKillIsStored() throws Exception {
    Path log = writeLog("""
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=331, DDD烈火王テムジン, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_76561198123350583, crossid=EOS_xxx, ip=10.0.0.1, ping=7
        Total of 1 in the game
        2026-07-26T10:54:00 10500.000 INF Entity zombieBusinessMan 347 killed by DDD烈火王テムジン 331
        """);

    logImportService.importLogFile(log);

    assertThat(entityKillRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.getPlayerPositionX() + ":" + row.getPlayerPositionY() + ":" + row.getPlayerPositionZ())
        .containsExactly("DDD烈火王テムジン:-532:48:-446");
  }

  @Test
  void marksMissingPlayersOfflineOnlyAfterSuccessfulPlayerListResponse() throws Exception {
    Path firstLpLog = tempDir.resolve("first-lp-log");
    Files.writeString(firstLpLog, """
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7
        1. id=202, PlayerB, pos=(0.0, 50.0, 0.0), rot=(0.0, 0.0, 0.0), remote=True, health=100, deaths=0, zombies=0, players=0, score=0, level=1, pltfmid=Steam_b, crossid=EOS_b, ip=10.0.0.2, ping=7
        Total of 2 in the game
        """);
    Path secondLpLog = tempDir.resolve("second-lp-log");
    Files.writeString(secondLpLog, """
        2026-07-26T10:54:10 10515.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-530.0, 48.0, -440.1), rot=(-4.2, 369.8, 0.0), remote=True, health=99, deaths=0, zombies=9, players=0, score=9, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=9
        Total of 1 in the game
        """);

    logImportService.importLogFile(firstLpLog);
    logImportService.importLogFile(secondLpLog);

    assertThat(playerCurrentStateRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.isOnline() + ":" + row.getPositionX() + ":" + row.getPing())
        .containsExactlyInAnyOrder(
            "PlayerA:true:-530:9",
            "PlayerB:false:0:7");
  }

  @Test
  void marksAllPlayersOfflineAfterSuccessfulEmptyPlayerListResponse() throws Exception {
    Path firstLpLog = tempDir.resolve("first-lp-log");
    Files.writeString(firstLpLog, """
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7
        Total of 1 in the game
        """);
    Path emptyLpLog = tempDir.resolve("empty-lp-log");
    Files.writeString(emptyLpLog, """
        2026-07-26T10:54:10 10515.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        Total of 0 in the game
        """);

    logImportService.importLogFile(firstLpLog);
    logImportService.importLogFile(emptyLpLog);

    assertThat(playerCurrentStateRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.isOnline() + ":" + row.getPositionX() + ":" + row.getHealth())
        .containsExactly("PlayerA:false:-532:101");
  }

  @Test
  void replacesCurrentStateForSameExternalPlayerWhenEntityIdChanges() throws Exception {
    Path firstLpLog = tempDir.resolve("first-lp-log");
    Files.writeString(firstLpLog, """
        2026-07-26T10:53:10 10455.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=101, deaths=0, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7
        Total of 1 in the game
        """);
    Path secondLpLog = tempDir.resolve("second-lp-log");
    Files.writeString(secondLpLog, """
        2026-07-26T11:03:10 11055.738 INF Executing command 'lp' by Telnet from 172.18.0.1:32864
        0. id=303, PlayerA, pos=(-520.0, 49.0, -430.1), rot=(-4.2, 369.8, 0.0), remote=True, health=77, deaths=1, zombies=9, players=0, score=9, level=3, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=11
        Total of 1 in the game
        """);

    logImportService.importLogFile(firstLpLog);
    logImportService.importLogFile(secondLpLog);

    assertThat(playerCurrentStateRepository.findAll())
        .extracting(row -> row.getPlayerEntityId() + ":" + row.getPlayerName() + ":" + row.getHealth() + ":" + row.getLevel()
            + ":" + row.getPositionX() + ":" + row.getPing() + ":" + row.isOnline())
        .containsExactly("303:PlayerA:77:3:-520:11:true");
  }

  @Test
  void importsRawTelnetPlayerListWithoutGameLogTimestamp() {
    List<String> telnetLines = SevenDaysTelnetService.normalizeLpOutput(List.of(
        "lp",
        "Executing command 'lp' by Telnet from 172.18.0.1:32864",
        "0. id=101, PlayerA, pos=(-532.0, 48.0, -446.1), rot=(-4.2, 369.8, 0.0), remote=True, health=76, deaths=1, zombies=8, players=0, score=8, level=2, pltfmid=Steam_a, crossid=EOS_a, ip=10.0.0.1, ping=7",
        "Total of 1 in the game"
    ), LocalDateTime.of(2026, 7, 26, 10, 53, 10));

    GameLogImportResult result = logImportService.importLogLines("telnet:lp", telnetLines);

    assertThat(result.malformedLines()).isZero();
    assertThat(playerCurrentStateRepository.findAll())
        .extracting(row -> row.getPlayerName() + ":" + row.getHealth() + ":" + row.getDeaths() + ":" + row.isOnline())
        .containsExactly("PlayerA:76:1:true");
  }

  private Path writeLog(String content) throws Exception {
    Path file = tempDir.resolve("log");
    Files.writeString(file, content);
    return file;
  }
}
