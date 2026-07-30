package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.config.SevenDaysDataProperties;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PoiNameService {

  private final SevenDaysDataProperties properties;
  private final ResourceLoader resourceLoader;
  private final Map<String, String> translations = new HashMap<>();
  private final Map<String, String> fallbackTokenTranslations = Map.ofEntries(
      Map.entry("countrytown", "田舎町"),
      Map.entry("downtown", "市街地"),
      Map.entry("residential", "住宅街"),
      Map.entry("commercial", "商業地区"),
      Map.entry("industrial", "工業地区"),
      Map.entry("remnant", "廃墟"),
      Map.entry("business", "事務所"),
      Map.entry("house", "住宅"),
      Map.entry("store", "店舗"),
      Map.entry("grocery", "食料品店"),
      Map.entry("pharmacy", "薬局"),
      Map.entry("hardware", "工具店"),
      Map.entry("book", "本屋"),
      Map.entry("gun", "銃砲店"),
      Map.entry("electronics", "電器店"),
      Map.entry("auto", "自動車店"),
      Map.entry("restaurant", "レストラン"),
      Map.entry("diner", "食堂"),
      Map.entry("gas", "ガソリン"),
      Map.entry("station", "スタンド"),
      Map.entry("church", "教会"),
      Map.entry("school", "学校"),
      Map.entry("hospital", "病院"),
      Map.entry("police", "警察署"),
      Map.entry("fire", "消防署"),
      Map.entry("factory", "工場"),
      Map.entry("warehouse", "倉庫"),
      Map.entry("farm", "農場"),
      Map.entry("camp", "キャンプ地"),
      Map.entry("trader", "トレーダー"),
      Map.entry("garage", "ガレージ"),
      Map.entry("barn", "納屋"),
      Map.entry("cabin", "小屋"),
      Map.entry("hotel", "ホテル"),
      Map.entry("motel", "モーテル"),
      Map.entry("parking", "駐車場"),
      Map.entry("lot", "空き地"),
      Map.entry("army", "軍施設"),
      Map.entry("construction", "建設現場"),
      Map.entry("utility", "公共施設"),
      Map.entry("wilderness", "荒野"));
  private final Set<String> missingKeys = ConcurrentHashMap.newKeySet();

  @PostConstruct
  void loadTranslations() {
    Resource resource = resourceLoader.getResource(properties.poi().translationResource());
    if (!resource.exists()) {
      log.info("POI translation resource was not found. resource={}", properties.poi().translationResource());
      return;
    }
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      int lineNumber = 0;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (lineNumber == 1 && line.startsWith("poi_key,")) {
          continue;
        }
        if (line.isBlank() || line.trim().startsWith("#")) {
          continue;
        }
        String[] columns = line.split(",", 2);
        if (columns.length != 2 || columns[0].isBlank() || columns[1].isBlank()) {
          log.warn("Invalid POI translation row skipped. resource={}, line={}",
              properties.poi().translationResource(), lineNumber);
          continue;
        }
        String key = normalizeKey(columns[0]);
        if (translations.containsKey(key)) {
          log.warn("Duplicate POI translation key ignored. resource={}, line={}, key={}",
              properties.poi().translationResource(), lineNumber, key);
          continue;
        }
        translations.put(key, columns[1].trim());
      }
      log.info("POI translations loaded. resource={}, count={}",
          properties.poi().translationResource(), translations.size());
    } catch (Exception e) {
      log.warn("POI translation resource cannot be loaded. resource={}",
          properties.poi().translationResource(), e);
    }
  }

  public String displayName(String poiName) {
    if (poiName == null || poiName.isBlank()) {
      return "荒野のどこか";
    }
    String key = normalizeKey(poiName);
    String translated = translations.get(key);
    if (translated != null && !translated.isBlank()) {
      return translated;
    }
    if (missingKeys.add(key)) {
      log.info("POI translation is not registered. key={}, raw={}", key, poiName);
    }
    return fallbackDisplayName(key);
  }

  public String normalizeKey(String poiName) {
    if (poiName == null) {
      return "";
    }
    return poiName.trim()
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s*#\\s*\\d{1,3}$", "")
        .replaceAll("[\\s_-]+\\d{1,3}$", "")
        .replaceAll("[^a-z0-9]+", "_")
        .replaceAll("_+", "_")
        .replaceAll("^_|_$", "");
  }

  private String fallbackDisplayName(String key) {
    if (key == null || key.isBlank()) {
      return "荒野のどこか";
    }
    String[] tokens = key.replaceAll("_\\d+$", "").split("_");
    StringBuilder displayName = new StringBuilder();
    for (String token : tokens) {
      if (token.isBlank() || token.matches("\\d+")) {
        continue;
      }
      String translated = fallbackTokenTranslations.get(token);
      if (translated == null) {
        translated = token;
      }
      if (!displayName.isEmpty()) {
        displayName.append(" ");
      }
      displayName.append(translated);
    }
    return displayName.isEmpty() ? key.replace('_', ' ') : displayName.toString();
  }
}
