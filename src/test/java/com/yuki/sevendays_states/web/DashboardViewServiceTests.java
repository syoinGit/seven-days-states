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

    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();

    assertThat(dashboard.playerStatuses())
        .extracting(DashboardViewService.PlayerStatus::playerName)
        .containsExactly("PlayerNew");
    assertThat(dashboard.playerStatuses().getFirst().coordinate()).isEqualTo("12, 22, 32");
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
}
