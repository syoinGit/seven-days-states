package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SevenDaysTelnetService {

  private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

  private final SevenDaysDataProperties properties;
  private final GameLogImportService logImportService;

  public GameLogImportResult fetchPlayerList() {
    List<String> lines = executeLpCommand();
    if (lines.isEmpty()) {
      log.info("7DTD telnet lp returned no readable lines.");
      return new GameLogImportResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    log.debug("7DTD telnet lp normalized lines. count={}, firstLine={}", lines.size(), lines.getFirst());
    return logImportService.importLogLines("telnet:lp", lines);
  }

  private List<String> executeLpCommand() {
    List<String> lines = new ArrayList<>();
    LocalDateTime commandTime = LocalDateTime.now(ZoneOffset.UTC);
    try (Socket socket = new Socket(properties.telnet().host(), properties.telnet().port())) {
      socket.setSoTimeout(properties.telnet().readTimeoutMs());
      BufferedReader reader = new BufferedReader(
          new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter writer = new BufferedWriter(
          new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

      if (!properties.telnet().password().isBlank()) {
        writer.write(properties.telnet().password());
        writer.newLine();
        writer.flush();
      }
      writer.write("lp");
      writer.newLine();
      writer.flush();

      String line;
      boolean commandOutputStarted = false;
      while ((line = reader.readLine()) != null) {
        if ("lp".equals(line)) {
          continue;
        }
        if (line.contains("Executing command 'lp'")) {
          commandOutputStarted = true;
        }
        if (!commandOutputStarted && (isPlayerLine(line) || isTotalLine(line))) {
          commandOutputStarted = true;
          lines.add(syntheticCommandLine(commandTime));
        }
        if (commandOutputStarted) {
          lines.add(normalizeCommandLine(line, commandTime));
        }
        if (commandOutputStarted && line.startsWith("Total of ") && line.endsWith(" in the game")) {
          break;
        }
      }
    } catch (Exception e) {
      log.warn("7DTD telnet lp command failed. host={}, port={}", properties.telnet().host(), properties.telnet().port(), e);
      return List.of();
    }
    return lines;
  }

  static List<String> normalizeLpOutput(List<String> rawLines, LocalDateTime commandTime) {
    List<String> lines = new ArrayList<>();
    boolean commandOutputStarted = false;
    for (String line : rawLines) {
      if ("lp".equals(line)) {
        continue;
      }
      if (line.contains("Executing command 'lp'")) {
        commandOutputStarted = true;
      }
      if (!commandOutputStarted && (isPlayerLine(line) || isTotalLine(line))) {
        commandOutputStarted = true;
        lines.add(syntheticCommandLine(commandTime));
      }
      if (commandOutputStarted) {
        lines.add(normalizeCommandLine(line, commandTime));
      }
      if (commandOutputStarted && isTotalLine(line)) {
        break;
      }
    }
    return List.copyOf(lines);
  }

  private static String normalizeCommandLine(String line, LocalDateTime commandTime) {
    if (!line.contains("Executing command 'lp'") || looksLikeGameLogLine(line)) {
      return line;
    }
    return syntheticCommandLine(commandTime);
  }

  private static String syntheticCommandLine(LocalDateTime commandTime) {
    return LOG_TIME.format(commandTime) + " 0.000 INF Executing command 'lp' by Telnet from app";
  }

  private static boolean looksLikeGameLogLine(String line) {
    return line.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\s+\\d+(?:\\.\\d+)?\\s+[A-Z]+\\s+.*$");
  }

  private static boolean isPlayerLine(String line) {
    return line.matches("^\\d+\\. id=.*$");
  }

  private static boolean isTotalLine(String line) {
    return line.startsWith("Total of ") && line.endsWith(" in the game");
  }
}
