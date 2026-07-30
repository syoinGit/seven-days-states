package com.yuki.sevendays_states.web;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_dashboard_service;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false"
})
class DashboardViewServiceTests {

  @Autowired
  private DashboardViewService dashboardViewService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetData() {
    jdbcTemplate.update("delete from t_player_position_transaction");
    jdbcTemplate.update("delete from t_player_current_state");
    jdbcTemplate.update("delete from t_player_state_snapshot");
    jdbcTemplate.update("delete from t_entity_kill_transaction");
    jdbcTemplate.update("delete from t_sleeper_transaction");
    jdbcTemplate.update("delete from t_vehicle_position_transaction");
    jdbcTemplate.update("delete from t_vehicle_current_state");
    jdbcTemplate.update("delete from t_world_event_transaction");
    jdbcTemplate.update("delete from m_japanese_translation");
    jdbcTemplate.update("delete from m_player");
  }

  @Test
  void playerStatusesShowOneRowForSameExternalPlayer() {
    jdbcTemplate.update("""
        insert into m_player (id, player_key, platform, user_id, native_platform, native_user_id, player_name, last_seen_at)
        values (1, 'Steam:76561198382915826', 'Steam', '76561198382915826', null, null, 'PlayerOld', timestamp '2026-07-25 10:00:00')
        """);
    jdbcTemplate.update("""
        insert into m_player (id, player_key, platform, user_id, native_platform, native_user_id, player_name, last_seen_at)
        values (2, 'EOS:00024b5c4d2546468b7c6775bd927c32', 'EOS', '00024b5c4d2546468b7c6775bd927c32', 'Steam', '76561198382915826', 'PlayerNew', timestamp '2026-07-26 10:00:00')
        """);
    jdbcTemplate.update("""
        insert into t_player_state_snapshot (player_id, world_name, game_name, captured_at, last_login, x, y, z, source_hash)
        values (2, 'World', 'Game', timestamp '2026-07-26 10:00:00', timestamp '2026-07-26 09:00:00', 10, 20, 30, 'snapshot-1')
        """);
    jdbcTemplate.update("""
        insert into t_player_state_snapshot (player_id, world_name, game_name, captured_at, last_login, x, y, z, source_hash)
        values (2, 'World', 'Game', timestamp '2026-07-26 11:00:00', timestamp '2026-07-26 10:00:00', 11, 21, 31, 'snapshot-2')
        """);
    jdbcTemplate.update("""
        insert into t_player_current_state
        (player_entity_id, player_name, position_x, position_y, position_z, platform_id, cross_platform_id, online, last_updated)
        values (331, 'PlayerNew', 12, 22, 32, 'Steam_76561198382915826', 'EOS_00024b5c4d2546468b7c6775bd927c32', true, timestamp with time zone '2026-07-26 01:30:00+00:00')
        """);
    jdbcTemplate.update("""
        insert into t_player_current_state
        (player_entity_id, player_name, position_x, position_y, position_z, platform_id, cross_platform_id, online, last_updated)
        values (332, 'PlayerNew', 13, 23, 33, 'Steam_76561198382915826', null, true, timestamp with time zone '2026-07-26 01:00:00+00:00')
        """);

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.playerStatuses())
        .extracting(DashboardViewService.PlayerStatus::playerName)
        .containsExactly("PlayerNew");
    assertThat(dashboard.playerStatuses().getFirst().coordinate()).isEqualTo("12, 22, 32");
  }

  @Test
  void playerStatusesUseLatestCurrentStateForHealthAndOnlineDisplay() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    jdbcTemplate.update("""
        insert into m_player (id, player_key, platform, user_id, native_platform, native_user_id, player_name, last_seen_at)
        values (1, 'EOS:eos-a', 'EOS', 'eos-a', 'Steam', 'steam-a', 'PlayerA', ?)
        """, Timestamp.from(now.toInstant()));
    jdbcTemplate.update("""
        insert into t_player_state_snapshot (player_id, world_name, game_name, captured_at, last_login, x, y, z, source_hash)
        values (1, 'World', 'Game', ?, ?, 10, 20, 30, 'snapshot-latest-current-state')
        """, Timestamp.from(now.minusSeconds(60).toInstant()), Timestamp.from(now.minusSeconds(60).toInstant()));
    jdbcTemplate.update("""
        insert into t_player_current_state
        (player_entity_id, player_name, position_x, position_y, position_z, health, level, platform_id, cross_platform_id, online, last_updated)
        values (101, 'PlayerA', 1, 2, 3, 10, 1, 'Steam_steam-a', 'EOS_eos-a', true, ?)
        """, now.minusSeconds(90));
    jdbcTemplate.update("""
        insert into t_player_current_state
        (player_entity_id, player_name, position_x, position_y, position_z, health, level, platform_id, cross_platform_id, online, last_updated)
        values (303, 'PlayerA', 4, 5, 6, 88, 3, 'Steam_steam-a', 'EOS_eos-a', true, ?)
        """, now.minusSeconds(5));

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.playerStatuses()).hasSize(1);
    assertThat(dashboard.playerStatuses().getFirst().health()).isEqualTo(88);
    assertThat(dashboard.playerStatuses().getFirst().level()).isEqualTo(3);
    assertThat(dashboard.playerStatuses().getFirst().coordinate()).isEqualTo("4, 5, 6");
    assertThat(dashboard.playerStatuses().getFirst().online()).isTrue();
  }

  @Test
  void playerStatusesTreatStaleCurrentStateAsOffline() {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    jdbcTemplate.update("""
        insert into m_player (id, player_key, platform, user_id, player_name, last_seen_at)
        values (1, 'EOS:eos-a', 'EOS', 'eos-a', 'PlayerA', ?)
        """, Timestamp.from(now.toInstant()));
    jdbcTemplate.update("""
        insert into t_player_state_snapshot (player_id, world_name, game_name, captured_at, last_login, x, y, z, source_hash)
        values (1, 'World', 'Game', ?, ?, 10, 20, 30, 'snapshot-stale-current-state')
        """, Timestamp.from(now.minusSeconds(300).toInstant()), Timestamp.from(now.minusSeconds(300).toInstant()));
    jdbcTemplate.update("""
        insert into t_player_current_state
        (player_entity_id, player_name, position_x, position_y, position_z, health, platform_id, cross_platform_id, online, last_updated)
        values (101, 'PlayerA', 1, 2, 3, 10, 'Steam_steam-a', 'EOS_eos-a', true, ?)
        """, now.minusSeconds(300));

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.playerStatuses()).hasSize(1);
    assertThat(dashboard.playerStatuses().getFirst().online()).isFalse();
  }

  @Test
  void killMessageContainsKillerAndVictim() {
    jdbcTemplate.update("""
        insert into t_entity_kill_transaction
        (occurred_at, player_name, player_entity_id, target_entity_type, target_entity_id, source_file, source_log_hash)
        values (timestamp with time zone '2026-07-26 01:22:51+00:00', 'DDD烈火王テムジン', 331, 'zombieBusinessMan', 347, 'log', 'kill-1')
        """);
    jdbcTemplate.update("""
        insert into m_japanese_translation (localization_key, display_text)
        values ('zombieBusinessMan', 'ショー')
        """);

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.travelEntries().getFirst().message())
        .isEqualTo("DDD烈火王テムジンがショーを討伐した！");
  }

  @Test
  void displayTimeFormatsUtcOffsetDateTimeAsJstAndKeepsLocalDateTimeLocal() {
    DisplayTimeFormatter formatter = new DisplayTimeFormatter();

    assertThat(formatter.format(OffsetDateTime.of(2026, 7, 26, 15, 8, 38, 0, ZoneOffset.UTC)))
        .isEqualTo("2026-07-27 00:08:38");
    assertThat(formatter.format(OffsetDateTime.of(2026, 7, 26, 1, 8, 38, 0, ZoneOffset.UTC)))
        .isEqualTo("2026-07-26 10:08:38");
    assertThat(formatter.format(Timestamp.valueOf(LocalDateTime.of(2026, 7, 26, 14, 8, 38))))
        .isEqualTo("2026-07-26 14:08:38");
  }

  @Test
  void killMessageWithoutVictimDoesNotThrow() {
    EventMessageFormatter formatter = new EventMessageFormatter();

    assertThat(formatter.format("KILL", "DDD烈火王テムジン", "討伐した", null, null))
        .isEqualTo("DDD烈火王テムジンが討伐した！");
  }

  @Test
  void timelineKeepsKillEventsAndOmitsSleeperRestoreNoise() {
    jdbcTemplate.update("""
        insert into t_entity_kill_transaction
        (occurred_at, player_name, player_entity_id, target_entity_type, target_entity_id, source_file, source_log_hash)
        values (timestamp with time zone '2026-07-26 01:22:51+00:00', 'DDD烈火王テムジン', 331, 'zombieBusinessMan', 347, 'log', 'kill-restore-test')
        """);
    jdbcTemplate.update("""
        insert into t_sleeper_transaction
        (occurred_at, transaction_type, sleeper_volume_x, sleeper_volume_y, sleeper_volume_z,
         position_x, position_y, position_z, entity_class, source_file, source_log_hash)
        values (timestamp with time zone '2026-07-26 01:22:52+00:00', 'SLEEPER_RESTORE', 1, 2, 3,
                4, 5, 6, 'zombieBoe', 'log', 'restore-1')
        """);

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.travelEntries())
        .extracting(DashboardViewService.TravelEntry::kind)
        .contains("KILL")
        .doesNotContain("SLEEPER_RESTORE");
  }

  @Test
  void dashboardShowsWorldAndVehicleEvents() {
    jdbcTemplate.update("""
        insert into m_player (id, player_key, platform, user_id, player_name, last_seen_at)
        values (1, 'EOS:eos-a', 'EOS', 'eos-a', 'PlayerA', timestamp '2026-07-26 10:00:00')
        """);
    jdbcTemplate.update("""
        insert into t_world_event_transaction
        (occurred_at, event_type, actor_player_name, detail_text, position_x, position_y, position_z,
         source_file, source_log_hash, raw_line)
        values (timestamp with time zone '2026-07-29 05:07:38+00:00', 'AIR_DROP', null, '補給物資', 460, 209, 33,
                'log', 'airdrop-1', 'raw')
        """);
    jdbcTemplate.update("""
        insert into t_vehicle_current_state
        (vehicle_entity_id, vehicle_type, vehicle_name, owner_player_id, owner_cross_platform_id,
         position_x, position_y, position_z, total_distance, active, last_updated, source_file, source_log_hash)
        values (2631, 'EntityBicycle', 'vehicleBicycle', 1, 'EOS_eos-a', 452, 38, -605, 20.0, true,
                timestamp with time zone '2026-07-29 05:58:16+00:00', 'log', 'vehicle-current')
        """);
    jdbcTemplate.update("""
        insert into t_vehicle_position_transaction
        (occurred_at, event_type, vehicle_entity_id, vehicle_type, vehicle_name, owner_player_id,
         position_x, position_y, position_z, movement_distance, source_file, source_log_hash, raw_line)
        values (timestamp with time zone '2026-07-29 05:56:11+00:00', 'VEHICLE_LOADED', 2631, 'EntityBicycle',
                'vehicleBicycle', 1, 442, 38, -615, 0, 'log', 'vehicle-loaded', 'raw')
        """);

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.travelEntries())
        .extracting(DashboardViewService.TravelEntry::kind)
        .contains("AIR_DROP", "VEHICLE_LOADED");
    assertThat(dashboard.vehicleStatuses()).hasSize(1);
    assertThat(dashboard.vehicleStatuses().getFirst().ownerName()).isEqualTo("PlayerA");
    assertThat(dashboard.vehicleStatuses().getFirst().totalDistance()).isEqualByComparingTo("20.0");
  }
}
