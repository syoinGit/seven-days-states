package com.yuki.sevendays_states.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai-analysis")
public record AiAnalysisProperties(
    int windowMinutes,
    int maxEvents,
    String systemPromptResource,
    boolean bedrockEnabled,
    String awsRegion,
    String modelId,
    int maxOutputTokens,
    int scheduleMinutes,
    long initialDelayMs
) {

  public AiAnalysisProperties {
    windowMinutes = windowMinutes <= 0 ? 30 : Math.min(windowMinutes, 180);
    maxEvents = maxEvents <= 0 ? 60 : Math.min(maxEvents, 200);
    systemPromptResource = systemPromptResource == null || systemPromptResource.isBlank()
        ? "classpath:prompts/watchpoint-system-prompt.txt"
        : systemPromptResource;
    awsRegion = awsRegion == null || awsRegion.isBlank() ? "ap-northeast-1" : awsRegion;
    modelId = modelId == null || modelId.isBlank()
        ? "jp.anthropic.claude-haiku-4-5-20251001-v1:0"
        : modelId;
    maxOutputTokens = maxOutputTokens <= 0 ? 400 : Math.min(maxOutputTokens, 1000);
    scheduleMinutes = scheduleMinutes <= 0 ? 30 : Math.max(scheduleMinutes, 5);
    initialDelayMs = initialDelayMs < 0 ? 60000 : initialDelayMs;
  }
}
