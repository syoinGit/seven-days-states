package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.config.AiAnalysisProperties;
import com.yuki.sevendays_states.web.WatchpointAiObservationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Coordinates observation, generation, validation, and persistence without holding a DB transaction during AWS I/O. */
@Service
@RequiredArgsConstructor
public class WatchpointAiPublishingService {

  public static final String SOURCE_TYPE = "AWS_BEDROCK";
  private static final String TITLE = "WATCHPOINT観測記録";

  private final AiAnalysisProperties properties;
  private final WatchpointAiObservationService observationService;
  private final BedrockWatchpointClient bedrockClient;
  private final AiCommentService aiCommentService;

  public PublishResult publishIfDue() {
    if (!properties.bedrockEnabled()) {
      return new PublishResult(PublishStatus.DISABLED, null);
    }
    OffsetDateTime threshold = OffsetDateTime.now(ZoneOffset.UTC)
        .minusMinutes(properties.scheduleMinutes());
    boolean recentlyPublished = aiCommentService.latestBySourceType(SOURCE_TYPE)
        .map(comment -> !comment.publishedAt().isBefore(threshold))
        .orElse(false);
    if (recentlyPublished) {
      return new PublishResult(PublishStatus.NOT_DUE, null);
    }
    return publishNow();
  }

  public PublishResult publishNow() {
    if (!properties.bedrockEnabled()) {
      return new PublishResult(PublishStatus.DISABLED, null);
    }
    BedrockWatchpointClient.GeneratedPost generated =
        bedrockClient.generate(observationService.buildRequest());
    AiCommentService.AiCommentEntry saved =
        aiCommentService.publishGenerated(TITLE, generated.body(), SOURCE_TYPE);
    return new PublishResult(PublishStatus.PUBLISHED, saved);
  }

  public enum PublishStatus {
    PUBLISHED,
    NOT_DUE,
    DISABLED
  }

  public record PublishResult(PublishStatus status, AiCommentService.AiCommentEntry comment) {
  }
}
