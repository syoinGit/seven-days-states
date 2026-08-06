package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.AiAnalysisProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchpointSystemPromptProvider {

  private final ResourceLoader resourceLoader;
  private final AiAnalysisProperties properties;
  private volatile String cachedPrompt;

  public String systemPrompt() {
    String current = cachedPrompt;
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (cachedPrompt == null) {
        cachedPrompt = loadPrompt();
      }
      return cachedPrompt;
    }
  }

  private String loadPrompt() {
    try (var input = resourceLoader.getResource(properties.systemPromptResource()).getInputStream()) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      throw new IllegalStateException("WATCHPOINT system prompt cannot be loaded.", e);
    }
  }
}
