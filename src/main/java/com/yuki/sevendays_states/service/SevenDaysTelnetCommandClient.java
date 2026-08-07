package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.telnet.TelnetClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SevenDaysTelnetCommandClient {
  private final SevenDaysDataProperties properties;

  /** Sends a server-wide chat message after removing command-breaking characters. */
  public boolean broadcast(String message) {
    if (message == null) {
      return false;
    }
    String sanitized = message
        .replaceAll("[\\r\\n\\p{Cntrl}]+", " ")
        .replace("\\", "\\\\")
        .replace("\"", "'")
        .strip();
    return !sanitized.isBlank() && send("say \"" + sanitized + "\"");
  }

  public boolean send(String command) {
    if (command == null || command.isBlank()) {
      return false;
    }
    TelnetClient telnet = new TelnetClient();
    try {
      telnet.setConnectTimeout(properties.telnet().readTimeoutMs());
      telnet.connect(properties.telnet().host(), properties.telnet().port());
      telnet.setSoTimeout(properties.telnet().readTimeoutMs());
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          telnet.getInputStream(), StandardCharsets.UTF_8));
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
          telnet.getOutputStream(), StandardCharsets.UTF_8));
      if (!properties.telnet().password().isBlank()) {
        drain(reader);
        write(writer, properties.telnet().password());
        drain(reader);
      }
      write(writer, command);
      return true;
    } catch (Exception e) {
      log.warn("7DTD telnet command failed.", e);
      return false;
    } finally {
      try {
        telnet.disconnect();
      } catch (Exception ignored) {
        // best effort cleanup
      }
    }
  }

  private void write(BufferedWriter writer, String command) throws java.io.IOException {
    writer.write(command);
    writer.write("\r\n");
    writer.flush();
  }

  private void drain(BufferedReader reader) {
    try {
      long deadline = System.nanoTime() + 300_000_000L;
      while (System.nanoTime() < deadline && reader.ready()) {
        reader.read();
      }
    } catch (Exception ignored) {
      // prompt output is optional
    }
  }
}
