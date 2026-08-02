package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.AiCommentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiaryMaintenanceService {

  private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Tokyo");

  private final JdbcTemplate jdbcTemplate;
  private final AiCommentService aiCommentService;
  private final PoiNameService poiNameService;

  public List<DiaryDay> days() {
    List<OffsetDateTime> timestamps = jdbcTemplate.query("""
        select occurred_at from (
          select occurred_at from t_player_join_transaction
          union all select occurred_at from t_player_leave_transaction
          union all select occurred_at from t_entity_kill_transaction
          union all select occurred_at from t_sleeper_transaction where transaction_type = 'SLEEPER_SPAWN'
          union all select occurred_at from t_world_event_transaction where event_type <> 'BLOOD_MOON'
          union all select occurred_at from t_vehicle_position_transaction where movement_distance >= 1
          union all select occurred_at from t_level_xp_summary_transaction
          union all select observed_at as occurred_at from t_world_time_observation
        ) diary_events
        order by occurred_at desc
        limit 10000
        """, (rs, rowNum) -> rs.getObject("occurred_at", OffsetDateTime.class));
    LinkedHashSet<LocalDate> dates = new LinkedHashSet<>();
    timestamps.forEach(timestamp -> dates.add(timestamp.atZoneSameInstant(DISPLAY_ZONE).toLocalDate()));
    aiCommentService.history().stream()
        .map(AiCommentService.AiCommentEntry::diaryDate)
        .filter(java.util.Objects::nonNull)
        .forEach(dates::add);
    return dates.stream().sorted(java.util.Comparator.reverseOrder()).limit(30)
        .map(date -> {
          DiaryPacket packet = packet(date);
          return new DiaryDay(
              date, packet.gameDayLabel(), packet.participants().size(), packet.eventCount(),
              aiCommentService.findByDiaryDate(date).isPresent());
        }).toList();
  }

  public DiaryPacket packet(LocalDate date) {
    OffsetDateTime from = date.atStartOfDay(DISPLAY_ZONE).toOffsetDateTime();
    OffsetDateTime to = date.plusDays(1).atStartOfDay(DISPLAY_ZONE).toOffsetDateTime();
    List<WorldClock> clocks = jdbcTemplate.query("""
        select game_day, game_hour, game_minute
        from t_world_time_observation
        where observed_at >= ? and observed_at < ?
        order by observed_at
        """, (rs, rowNum) -> new WorldClock(
        rs.getInt("game_day"), rs.getInt("game_hour"), rs.getInt("game_minute")), from, to);
    String gameDayLabel = clocks.isEmpty() ? "未観測" : gameDayLabel(clocks.getFirst(), clocks.getLast());

    List<String> playerNames = jdbcTemplate.queryForList("""
        select distinct player_name from (
          select player_name from t_player_join_transaction where occurred_at >= ? and occurred_at < ?
          union select player_name from t_player_position_transaction where occurred_at >= ? and occurred_at < ?
          union select player_name from t_entity_kill_transaction where occurred_at >= ? and occurred_at < ?
          union select player_name from t_sleeper_transaction where occurred_at >= ? and occurred_at < ?
        ) players where player_name is not null and player_name <> '' order by player_name
        """, String.class, from, to, from, to, from, to, from, to);
    List<PlayerDay> participants = playerNames.stream()
        .map(name -> playerDay(name, from, to))
        .toList();

    List<EventCount> events = jdbcTemplate.query("""
        select event_type, count(*) as event_count from (
          select event_type from t_world_event_transaction
          where occurred_at >= ? and occurred_at < ? and event_type <> 'BLOOD_MOON'
          union all
          select 'SLEEPER_SPAWN' from t_sleeper_transaction
          where occurred_at >= ? and occurred_at < ? and transaction_type = 'SLEEPER_SPAWN'
        ) events group by event_type order by event_count desc, event_type
        """, (rs, rowNum) -> new EventCount(rs.getString("event_type"), rs.getLong("event_count")),
        from, to, from, to);
    List<EventCount> enemies = jdbcTemplate.query("""
        select coalesce((select display_text from m_japanese_translation tr
                         where tr.localization_key = k.target_entity_type limit 1),
                        k.target_entity_type) as event_type,
               count(*) as event_count
        from t_entity_kill_transaction k
        where occurred_at >= ? and occurred_at < ?
        group by k.target_entity_type
        order by event_count desc
        limit 10
        """, (rs, rowNum) -> new EventCount(rs.getString("event_type"), rs.getLong("event_count")), from, to);
    List<String> pois = jdbcTemplate.queryForList("""
        select distinct poi_name from (
          select (select poi.poi_name from m_world_poi poi
                  where coalesce(poi.category, '') <> 'part' and poi.poi_name not like 'part_%'
                  order by ((poi.x - pos.position_x) * (poi.x - pos.position_x)
                       + (poi.z - pos.position_z) * (poi.z - pos.position_z)) limit 1) as poi_name
          from t_player_position_transaction pos
          where pos.occurred_at >= ? and pos.occurred_at < ?
        ) visited where poi_name is not null limit 30
        """, String.class, from, to).stream().map(poiNameService::displayName).toList();

    Long countedEvents = jdbcTemplate.queryForObject("""
        select count(*) from (
          select occurred_at from t_player_join_transaction where occurred_at >= ? and occurred_at < ?
          union all select occurred_at from t_player_leave_transaction where occurred_at >= ? and occurred_at < ?
          union all select occurred_at from t_entity_kill_transaction where occurred_at >= ? and occurred_at < ?
          union all select occurred_at from t_sleeper_transaction where occurred_at >= ? and occurred_at < ? and transaction_type = 'SLEEPER_SPAWN'
          union all select occurred_at from t_world_event_transaction where occurred_at >= ? and occurred_at < ? and event_type <> 'BLOOD_MOON'
          union all select occurred_at from t_vehicle_position_transaction where occurred_at >= ? and occurred_at < ? and movement_distance >= 1
        ) counted
        """, Long.class, from, to, from, to, from, to, from, to, from, to, from, to);
    long eventCount = countedEvents == null ? 0 : countedEvents;
    Optional<AiCommentService.AiCommentEntry> diary = aiCommentService.findByDiaryDate(date);
    String generationData = generationData(date, gameDayLabel, participants, events, enemies, pois);
    return new DiaryPacket(date, gameDayLabel, clocks.isEmpty() ? "未観測" : clockLabel(clocks.getLast()),
        eventCount, participants, events, enemies, pois, generationData, diary.orElse(null));
  }

  private PlayerDay playerDay(String name, OffsetDateTime from, OffsetDateTime to) {
    long joins = count("t_player_join_transaction", name, from, to, null);
    long leaves = count("t_player_leave_transaction", name, from, to, null);
    long kills = count("t_entity_kill_transaction", name, from, to, null);
    long encounters = count("t_sleeper_transaction", name, from, to, "transaction_type = 'SLEEPER_SPAWN'");
    BigDecimal position = distance("t_player_position_transaction", "player_name", name, from, to);
    BigDecimal vehicle = jdbcTemplate.queryForObject("""
        select coalesce(sum(v.movement_distance), 0)
        from t_vehicle_position_transaction v
        join m_player p on p.id = v.owner_player_id
        where p.player_name = ? and v.occurred_at >= ? and v.occurred_at < ?
        """, BigDecimal.class, name, from, to);
    return new PlayerDay(name, joins, leaves, kills, encounters, position, vehicle == null ? BigDecimal.ZERO : vehicle);
  }

  private long count(String table, String player, OffsetDateTime from, OffsetDateTime to, String extra) {
    String sql = "select count(*) from " + table
        + " where player_name = ? and occurred_at >= ? and occurred_at < ?"
        + (extra == null ? "" : " and " + extra);
    Long count = jdbcTemplate.queryForObject(sql, Long.class, player, from, to);
    return count == null ? 0 : count;
  }

  private BigDecimal distance(
      String table, String playerColumn, String player, OffsetDateTime from, OffsetDateTime to) {
    BigDecimal distance = jdbcTemplate.queryForObject(
        "select coalesce(sum(movement_distance), 0) from " + table
            + " where " + playerColumn + " = ? and occurred_at >= ? and occurred_at < ?",
        BigDecimal.class, player, from, to);
    return distance == null ? BigDecimal.ZERO : distance;
  }

  private String generationData(
      LocalDate date, String gameDay, List<PlayerDay> players, List<EventCount> events,
      List<EventCount> enemies, List<String> pois) {
    List<String> lines = new ArrayList<>();
    lines.add("WATCHPOINT 冒険日記生成データ");
    lines.add("実日付: " + date);
    lines.add("ゲーム内時間: " + gameDay);
    lines.add("参加プレイヤー:");
    players.forEach(player -> lines.add("- " + player.name() + ": 討伐" + player.kills()
        + "、遭遇" + player.encounters() + "、位置移動" + player.positionDistance().setScale(1, java.math.RoundingMode.HALF_UP)
        + "m、乗り物" + player.vehicleDistance().setScale(1, java.math.RoundingMode.HALF_UP)
        + "m、ログイン" + player.joins() + "回"));
    lines.add("注意: 位置移動には乗車中の移動が含まれる可能性があるため、乗り物距離と単純合算しない。");
    lines.add("主要イベント: " + eventSummary(events));
    lines.add("討伐した敵: " + eventSummary(enemies));
    lines.add("訪問POI: " + (pois.isEmpty() ? "未記録" : String.join("、", pois)));
    lines.add("生成指示: 荒廃世界の冒険記録として、事実を変えず、参加者それぞれの活躍が伝わる日本語の日記を作成する。");
    return String.join("\n", lines);
  }

  private String eventSummary(List<EventCount> counts) {
    return counts.isEmpty() ? "なし" : counts.stream()
        .map(count -> count.name() + " " + count.count() + "件")
        .collect(java.util.stream.Collectors.joining("、"));
  }

  private String gameDayLabel(WorldClock first, WorldClock last) {
    return first.day() == last.day()
        ? "DAY " + first.day()
        : "DAY " + first.day() + " → DAY " + last.day();
  }

  private String clockLabel(WorldClock clock) {
    return "DAY %d %02d:%02d".formatted(clock.day(), clock.hour(), clock.minute());
  }

  public record DiaryDay(
      LocalDate date, String gameDayLabel, int participantCount, long eventCount, boolean registered) {
  }

  public record DiaryPacket(
      LocalDate date, String gameDayLabel, String lastWorldTime, long eventCount,
      List<PlayerDay> participants, List<EventCount> events, List<EventCount> enemies,
      List<String> pois, String generationData, AiCommentService.AiCommentEntry diary) {
  }

  public record PlayerDay(
      String name, long joins, long leaves, long kills, long encounters,
      BigDecimal positionDistance, BigDecimal vehicleDistance) {
  }

  public record EventCount(String name, long count) {
  }

  private record WorldClock(int day, int hour, int minute) {
  }
}
