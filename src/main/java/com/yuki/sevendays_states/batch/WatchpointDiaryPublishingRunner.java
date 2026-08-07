package com.yuki.sevendays_states.batch;

import com.yuki.sevendays_states.service.WatchpointDiaryPublishingService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WatchpointDiaryPublishingRunner {

  private static final ZoneId JAPAN = ZoneId.of("Asia/Tokyo");
  private final WatchpointDiaryPublishingService publishingService;

  @Scheduled(cron = "${app.ai-analysis.diary-cron:0 15 20 * * *}", zone = "Asia/Tokyo")
  public void publishYesterday() {
    LocalDate date = LocalDate.now(JAPAN).minusDays(1);
    try {
      var result = publishingService.publishIfMissing(date);
      if (result.status() == WatchpointDiaryPublishingService.PublishStatus.PUBLISHED) {
        log.info("WATCHPOINT diary published. date={}, commentId={}", date, result.diary().id());
      }
    } catch (RuntimeException exception) {
      log.error("WATCHPOINT diary generation failed. date={}", date, exception);
    }
  }
}
