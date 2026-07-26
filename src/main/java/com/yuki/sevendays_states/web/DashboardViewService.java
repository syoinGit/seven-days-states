package com.yuki.sevendays_states.web;

import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardViewService {

  private final JdbcTemplate jdbcTemplate;
  private final PoiNameService poiNameService;
  private final EventMessageFormatter eventMessageFormatter;
  private final DisplayTimeFormatter displayTimeFormatter = new DisplayTimeFormatter();

  public DashboardView dashboard() {
    return new DashboardView(
        playerStatuses(),
        travelEntries(),
        poiStatuses(),
        killLeaders(),
        latestServerState());
  }

  private List<PlayerStatus> playerStatuses() {
    return jdbcTemplate.query("""
        with player_identity as (
          select p.*,
                 case
                   when upper(p.platform) = 'EOS' then 'EOS:' || replace(p.user_id, 'EOS_', '')
                   when p.user_id like 'EOS_%' then 'EOS:' || replace(p.user_id, 'EOS_', '')
                   when upper(coalesce(p.native_platform, '')) = 'EOS' then 'EOS:' || replace(p.native_user_id, 'EOS_', '')
                   when p.native_user_id like 'EOS_%' then 'EOS:' || replace(p.native_user_id, 'EOS_', '')
                 end as eos_key,
                 case
                   when upper(p.platform) = 'STEAM' then 'Steam:' || replace(p.user_id, 'Steam_', '')
                   when p.user_id like 'Steam_%' then 'Steam:' || replace(p.user_id, 'Steam_', '')
                   when upper(coalesce(p.native_platform, '')) = 'STEAM' then 'Steam:' || replace(p.native_user_id, 'Steam_', '')
                   when p.native_user_id like 'Steam_%' then 'Steam:' || replace(p.native_user_id, 'Steam_', '')
                 end as steam_key
          from m_player p
        ),
        deduped_players as (
          select p.*
          from player_identity p
          where not exists (
            select 1
            from player_identity newer
            where newer.id <> p.id
              and (
                (p.eos_key is not null and p.eos_key = newer.eos_key)
                or (p.steam_key is not null and p.steam_key = newer.steam_key)
                or p.player_key = newer.player_key
              )
              and (
                coalesce(newer.last_seen_at, timestamp '0001-01-01 00:00:00')
                  > coalesce(p.last_seen_at, timestamp '0001-01-01 00:00:00')
                or (
                  coalesce(newer.last_seen_at, timestamp '0001-01-01 00:00:00')
                    = coalesce(p.last_seen_at, timestamp '0001-01-01 00:00:00')
                  and newer.id > p.id
                )
              )
          )
        ),
        latest_snapshot as (
          select *
          from (
            select s.*,
                   row_number() over (partition by player_id order by captured_at desc, id desc) as snapshot_rank
            from t_player_state_snapshot s
          ) ranked_snapshot
          where snapshot_rank = 1
        ),
        latest_current_state as (
          select *
          from (
            select c.*,
                   case
                     when c.cross_platform_id is not null and c.cross_platform_id <> '' then 'EOS:' || replace(c.cross_platform_id, 'EOS_', '')
                     when c.platform_id is not null and c.platform_id <> '' then 'Steam:' || replace(c.platform_id, 'Steam_', '')
                     else 'ENTITY:' || c.player_entity_id
                   end as state_player_key,
                   row_number() over (
                     partition by case
                       when c.cross_platform_id is not null and c.cross_platform_id <> '' then 'EOS:' || replace(c.cross_platform_id, 'EOS_', '')
                       when c.platform_id is not null and c.platform_id <> '' then 'Steam:' || replace(c.platform_id, 'Steam_', '')
                       else 'ENTITY:' || c.player_entity_id
                     end
                     order by c.online desc, c.last_updated desc
                   ) as state_rank
            from t_player_current_state c
          ) ranked_state
          where state_rank = 1
        ),
        latest_position as (
          select player_name, occurred_at, position_x, position_y, position_z
          from (
            select player_name, occurred_at, position_x, position_y, position_z,
                   row_number() over (partition by player_name order by occurred_at desc) as position_rank
            from t_player_position_transaction
          ) ranked_position
          where position_rank = 1
        ),
        status_rows as (
          select p.id as player_id,
                 p.player_name,
                 s.world_name,
                 s.game_name,
                 coalesce(c.last_updated, pp.occurred_at, s.last_login) as last_login,
                 coalesce(c.position_x, pp.position_x, s.x) as x,
                 coalesce(c.position_y, pp.position_y, s.y) as y,
                 coalesce(c.position_z, pp.position_z, s.z) as z,
                 c.health,
                 c.deaths,
                 c.level,
                 c.ping,
                 c.online,
                 (
                   select poi.poi_name
                   from m_world_poi poi
                   where coalesce(poi.category, '') <> 'part'
                     and poi.poi_name not like 'part_%'
                   order by ((poi.x - coalesce(c.position_x, pp.position_x, s.x)) * (poi.x - coalesce(c.position_x, pp.position_x, s.x))
                         + (poi.z - coalesce(c.position_z, pp.position_z, s.z)) * (poi.z - coalesce(c.position_z, pp.position_z, s.z)))
                   limit 1
                 ) as poi_name,
                 (
                   select poi.category
                   from m_world_poi poi
                   where coalesce(poi.category, '') <> 'part'
                     and poi.poi_name not like 'part_%'
                   order by ((poi.x - coalesce(c.position_x, pp.position_x, s.x)) * (poi.x - coalesce(c.position_x, pp.position_x, s.x))
                         + (poi.z - coalesce(c.position_z, pp.position_z, s.z)) * (poi.z - coalesce(c.position_z, pp.position_z, s.z)))
                   limit 1
                 ) as poi_category,
                 row_number() over (
                   partition by p.id
                   order by c.online desc nulls last,
                            coalesce(c.last_updated, pp.occurred_at, s.captured_at) desc nulls last
                 ) as card_rank
          from deduped_players p
          left join latest_snapshot s on s.player_id = p.id
          left join latest_current_state c on c.state_player_key in (p.eos_key, p.steam_key, p.player_key)
          left join latest_position pp on pp.player_name = p.player_name
        )
        select player_name, world_name, game_name, last_login, x, y, z,
               health, deaths, level, ping, online, poi_name, poi_category
        from status_rows
        where card_rank = 1
        order by last_login desc nulls last, player_name
        limit 12
        """, (rs, rowNum) -> new PlayerStatus(
        rs.getString("player_name"),
        rs.getString("world_name"),
        rs.getString("game_name"),
        toDisplayTime(rs.getObject("last_login")),
        coordinate(rs.getObject("x"), rs.getObject("y"), rs.getObject("z")),
        displayPoi(rs.getString("poi_name")),
        rs.getString("poi_category"),
        integer(rs, "health"),
        integer(rs, "deaths"),
        integer(rs, "level"),
        integer(rs, "ping"),
        booleanValue(rs, "online")));
  }

  private List<TravelEntry> travelEntries() {
    return jdbcTemplate.query("""
        select *
        from (
          select occurred_at,
                 'JOIN' as kind,
                 player_name,
                 'ログインした' as action_text,
                 null as detail_text,
                 (
                   select poi.poi_name
                   from m_world_poi poi
                   where coalesce(poi.category, '') <> 'part'
                     and poi.poi_name not like 'part_%'
                   order by ((poi.x - j.position_x) * (poi.x - j.position_x)
                         + (poi.z - j.position_z) * (poi.z - j.position_z))
                   limit 1
                 ) as poi_name,
                 position_x as x,
                 position_y as y,
                 position_z as z
          from t_player_join_transaction j
          union all
          select occurred_at,
                 'LEAVE' as kind,
                 player_name,
                 'ログアウトした' as action_text,
                 null as detail_text,
                 null as poi_name,
                 null as x,
                 null as y,
                 null as z
          from t_player_leave_transaction
          union all
          select k.occurred_at,
                 'KILL' as kind,
                 k.player_name,
                 '討伐した' as action_text,
                 coalesce(
                   (select tr.display_text
                    from m_japanese_translation tr
                    where tr.localization_key = k.target_entity_type
                    limit 1),
                   k.target_entity_type
                 ) as detail_text,
	                 (
	                   select poi.poi_name
	                   from m_world_poi poi
	                   where k.player_position_x is not null
	                     and coalesce(poi.category, '') <> 'part'
	                     and poi.poi_name not like 'part_%'
	                   order by ((poi.x - k.player_position_x) * (poi.x - k.player_position_x)
	                         + (poi.z - k.player_position_z) * (poi.z - k.player_position_z))
	                   limit 1
	                 ) as poi_name,
	                 k.player_position_x as x,
	                 k.player_position_y as y,
	                 k.player_position_z as z
          from t_entity_kill_transaction k
          union all
          select occurred_at,
                 transaction_type as kind,
                 player_name,
                 case when transaction_type = 'SLEEPER_SPAWN'
                   then '眠っていた敵を起こした'
                   else '眠っていた敵が再配置された'
                 end as action_text,
                 coalesce(
                   (select tr.display_text
                    from m_japanese_translation tr
                    where tr.localization_key = s.entity_class
                    limit 1),
                   s.entity_class
                 ) as detail_text,
                 (
                   select poi.poi_name
                   from m_world_poi poi
                   where coalesce(poi.category, '') <> 'part'
                     and poi.poi_name not like 'part_%'
                   order by ((poi.x - coalesce(s.player_position_x, s.position_x))
                              * (poi.x - coalesce(s.player_position_x, s.position_x))
                         + (poi.z - coalesce(s.player_position_z, s.position_z))
                              * (poi.z - coalesce(s.player_position_z, s.position_z)))
                   limit 1
                 ) as poi_name,
	                 coalesce(player_position_x, position_x) as x,
	                 coalesce(player_position_y, position_y) as y,
	                 coalesce(player_position_z, position_z) as z
          from t_sleeper_transaction s
          where transaction_type <> 'SLEEPER_RESTORE'
          union all
          select occurred_at,
                 'XP' as kind,
                 player_name,
                 'レベル経験値を獲得した' as action_text,
                 '合計 ' || xp_total as detail_text,
                 null as poi_name,
                 null as x,
                 null as y,
                 null as z
          from t_level_xp_summary_transaction
        ) entries
        order by occurred_at desc
        limit 30
        """, (rs, rowNum) -> new TravelEntry(
        toDisplayTime(rs.getObject("occurred_at")),
        rs.getString("kind"),
        displayPlayer(rs.getString("player_name")),
        rs.getString("action_text"),
        rs.getString("detail_text"),
        eventMessageFormatter.format(
            rs.getString("kind"),
            displayPlayer(rs.getString("player_name")),
            rs.getString("action_text"),
            rs.getString("detail_text"),
            displayEventPoi(rs.getString("poi_name"))),
        displayEventPoi(rs.getString("poi_name")),
        coordinate(rs.getObject("x"), rs.getObject("y"), rs.getObject("z"))));
  }

  private List<PoiStatus> poiStatuses() {
    return jdbcTemplate.query("""
        select poi_name, category, player_name, x, y, z, captured_at, sleeper_events
        from (
          select poi.poi_name,
                 poi.category,
	                 pp.player_name,
	                 pp.position_x as x,
	                 pp.position_y as y,
	                 pp.position_z as z,
	                 pp.occurred_at as captured_at,
                 (
                   select count(*)
                   from t_sleeper_transaction st
                   where ((poi.x - st.position_x) * (poi.x - st.position_x)
                        + (poi.z - st.position_z) * (poi.z - st.position_z)) <= 2500
                 ) as sleeper_events,
                 row_number() over (
	                   partition by pp.player_name
	                   order by ((poi.x - pp.position_x) * (poi.x - pp.position_x)
	                         + (poi.z - pp.position_z) * (poi.z - pp.position_z))
	                 ) as poi_rank
	          from (
	            select player_name, last_updated as occurred_at, position_x, position_y, position_z
	            from t_player_current_state
	            union all
	            select player_name, occurred_at, position_x, position_y, position_z
	            from (
	              select player_name, occurred_at, position_x, position_y, position_z,
	                     row_number() over (partition by player_name order by occurred_at desc) as position_rank
	              from t_player_position_transaction
	            ) ranked_position
	            where position_rank = 1
	              and not exists (
	                select 1
	                from t_player_current_state current_state
	                where current_state.player_name = ranked_position.player_name
	              )
	          ) pp
          join m_world_poi poi on 1 = 1
          where coalesce(poi.category, '') <> 'part'
            and poi.poi_name not like 'part_%'
        ) ranked_poi
        where poi_rank = 1
        order by captured_at desc, player_name
        limit 12
        """, (rs, rowNum) -> new PoiStatus(
        displayPoi(rs.getString("poi_name")),
        rs.getString("category"),
        rs.getString("player_name"),
        coordinate(rs.getObject("x"), rs.getObject("y"), rs.getObject("z")),
        toDisplayTime(rs.getObject("captured_at")),
        rs.getLong("sleeper_events")));
  }

  private List<KillLeader> killLeaders() {
    return jdbcTemplate.query("""
        select player_name, count(*) as kills
        from t_entity_kill_transaction
        group by player_name
        order by kills desc, player_name
        limit 8
        """, (rs, rowNum) -> new KillLeader(
        rs.getString("player_name"),
        rs.getLong("kills")));
  }

  private ServerState latestServerState() {
    List<ServerState> states = jdbcTemplate.query("""
        select occurred_at, fps, player_count, zombie_count, entity_count, rss_mb
        from t_server_metric
        order by occurred_at desc
        limit 1
        """, (rs, rowNum) -> new ServerState(
        toDisplayTime(rs.getObject("occurred_at")),
        rs.getBigDecimal("fps"),
        integer(rs, "player_count"),
        integer(rs, "zombie_count"),
        integer(rs, "entity_count"),
        rs.getBigDecimal("rss_mb")));
    if (states.isEmpty()) {
      return new ServerState("", null, null, null, null, null);
    }
    return states.getFirst();
  }

  private Integer integer(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    int value = rs.getInt(column);
    return rs.wasNull() ? null : value;
  }

  private Boolean booleanValue(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
    boolean value = rs.getBoolean(column);
    return rs.wasNull() ? null : value;
  }

  private String coordinate(Object x, Object y, Object z) {
    if (x == null || y == null || z == null) {
      return "";
    }
    return x + ", " + y + ", " + z;
  }

  private String displayPlayer(String playerName) {
    return playerName == null || playerName.isBlank() ? "誰か" : playerName;
  }

  private String displayPoi(String poiName) {
    return poiNameService.displayName(poiName);
  }

  private String displayEventPoi(String poiName) {
    if (poiName == null || poiName.isBlank()) {
      return "";
    }
    return poiNameService.displayName(poiName);
  }

  private String toDisplayTime(Object value) {
    return displayTimeFormatter.format(value);
  }

  public record DashboardView(
      List<PlayerStatus> playerStatuses,
      List<TravelEntry> travelEntries,
      List<PoiStatus> poiStatuses,
      List<KillLeader> killLeaders,
      ServerState serverState
  ) {
  }

  public record PlayerStatus(
      String playerName,
      String worldName,
      String gameName,
      String lastLogin,
      String coordinate,
      String poiName,
      String poiCategory,
      Integer health,
      Integer deaths,
      Integer level,
      Integer ping,
      Boolean online
  ) {
  }

  public record TravelEntry(
      String occurredAt,
      String kind,
      String actor,
      String actionText,
      String detailText,
      String message,
      String poiName,
      String coordinate
  ) {
  }

  public record PoiStatus(
      String poiName,
      String category,
      String playerName,
      String coordinate,
      String capturedAt,
      long sleeperEvents
  ) {
  }

  public record KillLeader(String playerName, long kills) {
  }

  public record ServerState(
      String occurredAt,
      BigDecimal fps,
      Integer playerCount,
      Integer zombieCount,
      Integer entityCount,
      BigDecimal rssMb
  ) {
  }
}
