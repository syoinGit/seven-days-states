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
    return key.replace('_', ' ');
  }
}
