package com.yuki.sevendays_states.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuki.sevendays_states.config.AiAnalysisProperties;
import com.yuki.sevendays_states.web.WatchpointAiObservationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WatchpointAiPublishingServiceTests {

  @Test
  void generatesThenPersistsValidatedObservation() {
    AiAnalysisProperties properties = properties(true);
    WatchpointAiObservationService observations = mock(WatchpointAiObservationService.class);
    BedrockWatchpointClient bedrock = mock(BedrockWatchpointClient.class);
    AiCommentService comments = mock(AiCommentService.class);
    WatchpointAiObservationService.AnalysisRequest request = mock(
        WatchpointAiObservationService.AnalysisRequest.class);
    AiCommentService.AiCommentEntry saved = new AiCommentService.AiCommentEntry(
        10L, null, "WATCHPOINT観測記録", "観測本文",
        OffsetDateTime.now(ZoneOffset.UTC), "AWS_BEDROCK");
    when(observations.buildRequest()).thenReturn(request);
    when(bedrock.generate(request)).thenReturn(
        new BedrockWatchpointClient.GeneratedPost("観測本文", List.of("current-totals")));
    when(comments.publishGenerated("WATCHPOINT観測記録", "観測本文", "AWS_BEDROCK"))
        .thenReturn(saved);

    WatchpointAiPublishingService.PublishResult result =
        new WatchpointAiPublishingService(properties, observations, bedrock, comments).publishNow();

    assertThat(result.status()).isEqualTo(WatchpointAiPublishingService.PublishStatus.PUBLISHED);
    assertThat(result.comment()).isEqualTo(saved);
    verify(comments).publishGenerated("WATCHPOINT観測記録", "観測本文", "AWS_BEDROCK");
  }

  @Test
  void doesNothingWhenIntegrationIsDisabled() {
    WatchpointAiObservationService observations = mock(WatchpointAiObservationService.class);
    BedrockWatchpointClient bedrock = mock(BedrockWatchpointClient.class);
    AiCommentService comments = mock(AiCommentService.class);

    WatchpointAiPublishingService.PublishResult result =
        new WatchpointAiPublishingService(properties(false), observations, bedrock, comments)
            .publishIfDue();

    assertThat(result.status()).isEqualTo(WatchpointAiPublishingService.PublishStatus.DISABLED);
    verify(observations, never()).buildRequest();
    verify(bedrock, never()).generate(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void skipsGenerationWhenRecentlyPublished() {
    WatchpointAiObservationService observations = mock(WatchpointAiObservationService.class);
    BedrockWatchpointClient bedrock = mock(BedrockWatchpointClient.class);
    AiCommentService comments = mock(AiCommentService.class);
    when(comments.latestBySourceType("AWS_BEDROCK")).thenReturn(Optional.of(
        new AiCommentService.AiCommentEntry(10L, null, "WATCHPOINT観測記録", "本文",
            OffsetDateTime.now(ZoneOffset.UTC), "AWS_BEDROCK")));

    WatchpointAiPublishingService.PublishResult result =
        new WatchpointAiPublishingService(properties(true), observations, bedrock, comments)
            .publishIfDue();

    assertThat(result.status()).isEqualTo(WatchpointAiPublishingService.PublishStatus.NOT_DUE);
    verify(bedrock, never()).generate(org.mockito.ArgumentMatchers.any());
  }

  private AiAnalysisProperties properties(boolean enabled) {
    return new AiAnalysisProperties(
        30, 60, "classpath:prompts/watchpoint-system-prompt.txt", enabled,
        "ap-northeast-1", "jp.anthropic.claude-haiku-4-5-20251001-v1:0", 400, 30, 60000);
  }
}
