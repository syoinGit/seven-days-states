package com.yuki.sevendays_states.batch;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import com.yuki.sevendays_states.service.GameLogImportResult;
import com.yuki.sevendays_states.service.SevenDaysTelnetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SevenDaysTelnetPlayerStateRunner {

  private final SevenDaysDataProperties properties;
  private final SevenDaysTelnetService telnetService;

  @Scheduled(
      initialDelayString = "${app.sevendays.telnet.initial-delay-ms:30000}",
      fixedDelayString = "#{T(java.lang.Math).max(${app.sevendays.telnet.lp-interval-seconds:60}, 60) * 1000}")
  public void scheduledPlayerStateFetch() {
    if (!properties.telnet().scheduledEnabled()) {
      return;
    }
    GameLogImportResult result = telnetService.fetchPlayerList();
    log.info("7DTD telnet lp import finished. {}", result);
  }
}
