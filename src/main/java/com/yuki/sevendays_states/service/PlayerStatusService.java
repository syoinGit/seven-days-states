package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import com.yuki.sevendays_states.entity.M_Player;
import com.yuki.sevendays_states.entity.T_PlayerStatus;
import com.yuki.sevendays_states.repository.M_PlayerRepository;
import com.yuki.sevendays_states.repository.T_PlayerStatusRepository;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerStatusService {
  private static final Map<String, String> LABELS = Map.of(
      "ACTIVE", "活動中", "EATING", "ごはん中", "AFK", "AFK", "OUT", "外出", "SLEEPING", "就寝中");

  private final M_PlayerRepository playerRepository;
  private final T_PlayerStatusRepository statusRepository;
  private final SevenDaysTelnetCommandClient telnet;
  private final SevenDaysDataProperties properties;

  @Transactional
  public Optional<StatusChange> updateByName(String playerName, String requestedStatus, String source) {
    if (playerName == null || requestedStatus == null) {
      return Optional.empty();
    }
    String status = normalize(requestedStatus);
    if (status == null) {
      return Optional.empty();
    }
    Optional<M_Player> player = playerRepository.findFirstByPlayerNameOrderByLastSeenAtDesc(playerName);
    if (player.isEmpty()) {
      return Optional.empty();
    }
    T_PlayerStatus row = statusRepository.findById(player.get().getId()).orElseGet(T_PlayerStatus::new);
    String previous = row.getStatus();
    row.setPlayerId(player.get().getId());
    row.setStatus(status);
    row.setSource(source == null || source.isBlank() ? "WEB" : source);
    row.setUpdatedAt(OffsetDateTime.now());
    statusRepository.save(row);
    if (!status.equals(previous)) {
      String message = player.get().getPlayerName() + " は " + LABELS.get(status) + " です";
      telnet.send("say \"" + escape(message) + "\"");
      return Optional.of(new StatusChange(player.get(), status, LABELS.get(status)));
    }
    return Optional.empty();
  }

  public Optional<StatusChange> updateFromChat(String playerName, String command) {
    String status = switch (command.strip().toLowerCase(Locale.ROOT)) {
      case "!飯", "!めし", "!ごはん" -> "EATING";
      case "!afk" -> "AFK";
      case "!戻り", "!もどり", "!back" -> "ACTIVE";
      case "!寝る", "!ねる" -> "SLEEPING";
      case "!外出", "!そと" -> "OUT";
      default -> null;
    };
    return updateByName(playerName, status, "CHAT");
  }

  private String normalize(String raw) {
    String status = raw.strip().toUpperCase(Locale.ROOT);
    return LABELS.containsKey(status) ? status : null;
  }

  private String escape(String value) {
    return value.replace("\\", "\\\\").replace("\"", "'");
  }

  public record StatusChange(M_Player player, String status, String label) {
  }
}
