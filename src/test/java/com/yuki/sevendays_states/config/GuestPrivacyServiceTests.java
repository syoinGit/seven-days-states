package com.yuki.sevendays_states.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GuestPrivacyServiceTests {

  @Test
  void replacesPlayerNamesAndAddsPrivacyIndicator() {
    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put("魅惑のこし餡ぼでぃ", "SURVIVOR-01");
    aliases.put("DDD烈火王テムジン", "SURVIVOR-02");
    String html = "<body><a href=\"/players/42\">魅惑のこし餡ぼでぃ</a>がDDD烈火王テムジンを発見した</body>";

    String anonymized = GuestPrivacyService.anonymizeHtml(html, aliases);

    assertThat(anonymized)
        .contains("SURVIVOR-01", "SURVIVOR-02")
        .contains("PRIVATE GUEST VIEW")
        .contains("href=\"/dashboard\"")
        .doesNotContain("/players/42")
        .doesNotContain("魅惑のこし餡ぼでぃ", "DDD烈火王テムジン");
  }

  @Test
  void replacesNamesInsideAttributesWithoutChainingAliases() {
    Map<String, String> aliases = new LinkedHashMap<>();
    aliases.put("Alpha", "SURVIVOR-01");
    aliases.put("SURVIVOR-01", "SURVIVOR-02");
    String html = "<body title=\"Alpha\">Alpha / Steam_76561198000000000 / EOS:abcdef</body>";

    String anonymized = GuestPrivacyService.anonymizeHtml(html, aliases);

    assertThat(anonymized)
        .contains("title=\"SURVIVOR-01\"")
        .contains("SURVIVOR-01 / EXTERNAL-ID / EXTERNAL-ID")
        .doesNotContain("Steam_76561198000000000", "EOS:abcdef")
        .doesNotContain("SURVIVOR-02</body>");
  }
}
