package com.yuki.sevendays_states.web;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states_diary;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true",
    "app.sevendays.import.startup-enabled=false"
})
class DiaryMaintenanceServiceTests {

  @Autowired
  private DiaryMaintenanceService service;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void resetData() {
    jdbcTemplate.update("delete from t_ai_comment");
    jdbcTemplate.update("delete from t_world_time_observation");
    jdbcTemplate.update("delete from t_entity_kill_transaction");
    jdbcTemplate.update("delete from t_player_position_transaction");
    jdbcTemplate.update("delete from t_player_join_transaction");
  }

  @Test
  void buildsDailyGenerationPacketFromAdventureLogs() {
    LocalDate date = LocalDate.of(2026, 8, 2);
    OffsetDateTime time = OffsetDateTime.of(2026, 8, 2, 3, 0, 0, 0, ZoneOffset.UTC);
    jdbcTemplate.update("""
        insert into t_world_time_observation
        (observed_at, game_day, game_hour, game_minute, source, source_hash, raw_response)
        values (?, 20, 12, 0, 'telnet:gettime', 'diary-time', 'Day 20, 12:00')
        """, time);
    jdbcTemplate.update("""
        insert into t_player_join_transaction
        (occurred_at, player_name, player_entity_id, source_file, source_log_hash)
        values (?, 'PlayerA', 101, 'log', 'diary-join')
        """, time);
    jdbcTemplate.update("""
        insert into t_player_position_transaction
        (occurred_at, player_name, player_entity_id, position_x, position_y, position_z,
         position_source_type, source_event_hash, source_file, movement_distance)
        values (?, 'PlayerA', 101, 10, 40, 20, 'LP_COMMAND', 'diary-pos', 'telnet', 125.5)
        """, time.plusMinutes(1));
    jdbcTemplate.update("""
        insert into t_entity_kill_transaction
        (occurred_at, player_name, player_entity_id, target_entity_type, target_entity_id,
         source_file, source_log_hash)
        values (?, 'PlayerA', 101, 'zombieNurse', 501, 'log', 'diary-kill')
        """, time.plusMinutes(2));

    DiaryMaintenanceService.DiaryPacket packet = service.packet(date);

    assertThat(packet.gameDayLabel()).isEqualTo("DAY 20");
    assertThat(packet.participants()).singleElement().satisfies(player -> {
      assertThat(player.name()).isEqualTo("PlayerA");
      assertThat(player.kills()).isOne();
      assertThat(player.positionDistance()).isEqualByComparingTo("125.5");
    });
    assertThat(packet.generationData()).contains("PlayerA", "討伐1", "位置移動125.5m");
    assertThat(service.days()).extracting(DiaryMaintenanceService.DiaryDay::date).contains(date);
  }
}
