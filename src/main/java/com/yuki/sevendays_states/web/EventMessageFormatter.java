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
    if ("AIR_DROP".equals(kind)) {
      return poiName == null || poiName.isBlank()
          ? "補給物資が投下された"
          : poiName + "付近に補給物資が投下された";
    }
    if ("WANDERING_HORDE".equals(kind)) {
      return safeActor + "の近くで徘徊ホードが発生した";
    }
    if ("SCOUT_HORDE".equals(kind)) {
      return poiName == null || poiName.isBlank()
          ? "スクリーマーの気配がした"
          : poiName + "付近でスクリーマーの気配がした";
    }
    if ("SCREAMER_SPAWN".equals(kind)) {
      return poiName == null || poiName.isBlank()
          ? "スクリーマーが出現した"
          : poiName + "付近にスクリーマーが出現した";
    }
    if ("BLOOD_MOON".equals(kind)) {
      return detailText == null || detailText.isBlank()
          ? "ブラッドムーン予定が更新された"
          : "ブラッドムーン予定が更新された（" + detailText + "）";
    }
    if ("VEHICLE_REMOVED".equals(kind)) {
      if (detailText == null || detailText.isBlank()) {
        return safeActor + "の乗り物が消失した";
      }
      return safeActor + "の" + detailText + "が消失した";
    }
    if ("VEHICLE_LOADED".equals(kind) || "VEHICLE_POST_INIT".equals(kind)) {
      if (detailText == null || detailText.isBlank()) {
        return safeActor + "の乗り物を確認した";
      }
      return safeActor + "の" + detailText + "を確認した";
    }
    if (poiName == null || poiName.isBlank()) {
      return safeActor + "が" + actionText;
    }
    return safeActor + "が" + poiName + "で" + actionText;
  }
}
