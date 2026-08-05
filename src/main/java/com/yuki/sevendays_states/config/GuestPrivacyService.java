package com.yuki.sevendays_states.config;

import com.yuki.sevendays_states.entity.M_Player;
import com.yuki.sevendays_states.repository.M_PlayerRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Applies the presentation boundary for the read-only guest role.
 *
 * <p>Guest pages are deliberately anonymized at the last possible boundary as a safety net.
 * Controllers still block player dossiers and mutating actions; this service prevents a future
 * view change from accidentally putting a real player name or external platform identifier into
 * a guest response.</p>
 */
@Service
@RequiredArgsConstructor
public class GuestPrivacyService {

  private static final String PRIVACY_BADGE = """
      <aside class="privacy-badge" aria-label="guest privacy mode">
        <b>PRIVATE GUEST VIEW</b><span>プレイヤー名を匿名化しています</span>
      </aside>
      """;
  private static final String PRIVACY_STYLESHEET =
      "<link rel=\"stylesheet\" href=\"/css/app.css?v=20260805-3\">";
  private static final Pattern EXTERNAL_ID =
      Pattern.compile("(?i)(?:Steam|EOS)(?:_|:)[A-Za-z0-9-]+");

  private final M_PlayerRepository playerRepository;

  public String anonymizeHtml(String html) {
    return anonymizeHtml(html, aliases());
  }

  static String anonymizeHtml(String html, Map<String, String> aliases) {
    String anonymized = replaceSensitiveValues(html, aliases)
        .replaceAll("(?i)href=(\\\"|')/players/[^\\\"']*(\\\"|')", "href=\"/dashboard\"")
        .replaceAll("(?i)</head>", Matcher.quoteReplacement(PRIVACY_STYLESHEET) + "</head>")
        .replaceAll("(?i)</body>", Matcher.quoteReplacement(PRIVACY_BADGE) + "</body>");
    if (!anonymized.contains("class=\"privacy-badge\"")) {
      anonymized += PRIVACY_BADGE;
    }
    return anonymized;
  }

  private static String replaceSensitiveValues(String html, Map<String, String> aliases) {
    Map<String, String> replacements = new LinkedHashMap<>();
    aliases.entrySet().stream()
        .sorted(Map.Entry.<String, String>comparingByKey(
            Comparator.comparingInt(String::length).reversed()))
        .forEach(entry -> {
          replacements.put(entry.getKey(), entry.getValue());
          String escaped = HtmlUtils.htmlEscape(entry.getKey());
          if (!escaped.equals(entry.getKey())) {
            replacements.put(escaped, entry.getValue());
          }
        });
    if (replacements.isEmpty()) {
      return EXTERNAL_ID.matcher(html).replaceAll("EXTERNAL-ID");
    }
    String alternatives = replacements.keySet().stream()
        .map(Pattern::quote)
        .reduce((left, right) -> left + "|" + right)
        .orElseThrow();
    Matcher matcher = Pattern.compile(alternatives).matcher(html);
    StringBuffer result = new StringBuffer(html.length());
    while (matcher.find()) {
      matcher.appendReplacement(result,
          Matcher.quoteReplacement(replacements.get(matcher.group())));
    }
    matcher.appendTail(result);
    return EXTERNAL_ID.matcher(result).replaceAll("EXTERNAL-ID");
  }

  private Map<String, String> aliases() {
    Map<String, String> aliases = new LinkedHashMap<>();
    var players = playerRepository.findAll().stream()
        .sorted(Comparator.comparing(M_Player::getId))
        .toList();
    for (int index = 0; index < players.size(); index++) {
      String name = players.get(index).getPlayerName();
      if (name != null && !name.isBlank()) {
        aliases.putIfAbsent(name, "SURVIVOR-%02d".formatted(index + 1));
      }
    }
    return aliases;
  }

  public static boolean isGuest(Authentication authentication) {
    return authentication != null && authentication.isAuthenticated()
        && authentication.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .anyMatch("ROLE_VIEWER"::equals);
  }
}
