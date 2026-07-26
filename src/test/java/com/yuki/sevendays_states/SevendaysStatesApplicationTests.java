package com.yuki.sevendays_states;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:sevendays_states;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.flyway.enabled=true"
})
class SevendaysStatesApplicationTests {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Test
  void contextLoads() {
  }

  @Test
  void startupImportPopulatesAllCurrentTables() {
    assertThat(count("T_IMPORT_RUN")).isPositive();
    assertThat(count("M_SERVER_CONFIG_SETTING")).isPositive();
    assertThat(count("M_GAME_CONFIG_ELEMENT")).isPositive();
    assertThat(count("M_JAPANESE_TRANSLATION")).isPositive();
    assertThat(count("M_GAME_ENTITY")).isPositive();
    assertThat(count("M_BLOCK")).isPositive();
    assertThat(count("M_ITEM")).isPositive();
    assertThat(count("M_VEHICLE")).isPositive();
    assertThat(count("M_WORLD")).isPositive();
    assertThat(count("M_GAME_SAVE")).isPositive();
    assertThat(count("M_WORLD_POI")).isPositive();
    assertThat(count("M_WORLD_SPAWN_POINT")).isPositive();
    assertThat(count("M_PLAYER")).isPositive();
    assertThat(count("T_PLAYER_STATE_SNAPSHOT")).isPositive();
    assertThat(count("T_PLAYER_MARKER_SNAPSHOT")).isPositive();
    assertThat(tableExists("M_SOURCE_FILE")).isFalse();
    assertThat(tableExists("M_SERVER_COMMAND_PERMISSION")).isFalse();
    assertThat(tableExists("T_SAVE_FILE_SNAPSHOT")).isFalse();
  }

  private Long count(String tableName) {
    return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
  }

  private boolean tableExists(String tableName) {
    Long count = jdbcTemplate.queryForObject("""
        select count(*)
        from information_schema.tables
        where upper(table_name) = ?
        """, Long.class, tableName);
    return count != null && count > 0;
  }

}
