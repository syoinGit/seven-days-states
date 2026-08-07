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
  private final SevenDaysTelnetCommandClient telnetCommandClient;

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
    WatchpointAiObservationService.AnalysisRequest request = observationService.buildRequest();
    if (!hasActivity(request)) {
      return new PublishResult(PublishStatus.NO_ACTIVITY, null);
    }
    return publish(request);
  }

  public PublishResult publishNow() {
    if (!properties.bedrockEnabled()) {
      return new PublishResult(PublishStatus.DISABLED, null);
    }
    return publish(observationService.buildRequest());
  }

  private PublishResult publish(WatchpointAiObservationService.AnalysisRequest request) {
    BedrockWatchpointClient.GeneratedPost generated = bedrockClient.generate(request);
    AiCommentService.AiCommentEntry saved =
        aiCommentService.publishGenerated(TITLE, generated.body(), SOURCE_TYPE);
    telnetCommandClient.broadcast("WATCHPOINT: " + saved.body());
    return new PublishResult(PublishStatus.PUBLISHED, saved);
  }

  private boolean hasActivity(WatchpointAiObservationService.AnalysisRequest request) {
    var observation = request.observation();
    if (observation == null || observation.currentTotals() == null) {
      return false;
    }
    var totals = observation.currentTotals();
    return !observation.events().isEmpty()
        || totals.joins() > 0 || totals.leaves() > 0 || totals.kills() > 0
        || totals.sleeperEncounters() > 0 || totals.deaths() > 0 || totals.hordeEvents() > 0
        || (totals.onFootDistanceMeters() != null && totals.onFootDistanceMeters().signum() > 0)
        || (totals.vehicleDistanceMeters() != null && totals.vehicleDistanceMeters().signum() > 0);
  }

  public enum PublishStatus {
    PUBLISHED,
    NOT_DUE,
    NO_ACTIVITY,
    DISABLED
  }

  public record PublishResult(PublishStatus status, AiCommentService.AiCommentEntry comment) {
  }
}
