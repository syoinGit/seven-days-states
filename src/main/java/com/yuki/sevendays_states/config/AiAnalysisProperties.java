package com.yuki.sevendays_states.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-analysis")
public record AiAnalysisProperties(
    int windowMinutes,
    int maxEvents,
    String systemPromptResource
) {

  public AiAnalysisProperties {
    windowMinutes = windowMinutes <= 0 ? 30 : Math.min(windowMinutes, 180);
    maxEvents = maxEvents <= 0 ? 60 : Math.min(maxEvents, 200);
    systemPromptResource = systemPromptResource == null || systemPromptResource.isBlank()
        ? "classpath:prompts/watchpoint-system-prompt.txt"
        : systemPromptResource;
  }
}
