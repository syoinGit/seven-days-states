package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SevenDaysTelnetService {

  private final SevenDaysDataProperties properties;
  private final GameLogImportService logImportService;

  public GameLogImportResult fetchPlayerList() {
    List<String> lines = executeLpCommand();
    if (lines.isEmpty()) {
      return new GameLogImportResult(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
    return logImportService.importLogLines("telnet:lp", lines);
  }

  private List<String> executeLpCommand() {
    List<String> lines = new ArrayList<>();
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
        if (commandOutputStarted) {
          lines.add(line);
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
}
