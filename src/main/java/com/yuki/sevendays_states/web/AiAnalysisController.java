package com.yuki.sevendays_states.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maintenance/ai-analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

  private final WatchpointAiObservationService observationService;

  @GetMapping(value = "/payload", produces = MediaType.APPLICATION_JSON_VALUE)
  public WatchpointAiObservationService.AnalysisRequest payload() {
    return observationService.buildRequest();
  }
}
