package com.yuki.sevendays_states.web;

import org.springframework.stereotype.Component;

@Component
public class EventMessageFormatter {

  public String format(String kind, String actor, String actionText, String detailText, String poiName) {
    String safeActor = actor == null || actor.isBlank() ? "誰か" : actor;
    if ("KILL".equals(kind)) {
      if (detailText == null || detailText.isBlank()) {
        return safeActor + "が討伐した！";
      }
      return safeActor + "が" + detailText + "を討伐した！";
    }
    if ("JOIN".equals(kind)) {
      if (poiName == null || poiName.isBlank()) {
        return safeActor + "がログインした";
      }
      return safeActor + "が" + poiName + "でログインした";
    }
    if ("LEAVE".equals(kind)) {
      return safeActor + "がログアウトした";
    }
    if ("SLEEPER_RESTORE".equals(kind)) {
      return "眠っていた敵が再配置された";
    }
    if (poiName == null || poiName.isBlank()) {
      return safeActor + "が" + actionText;
    }
    return safeActor + "が" + poiName + "で" + actionText;
  }
}
