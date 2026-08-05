package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.AiCommentService;
import com.yuki.sevendays_states.service.CurrentWebAccountService;
import com.yuki.sevendays_states.service.PlayerSocialService;
import com.yuki.sevendays_states.service.PlayerStatusService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private static final DateTimeFormatter TIMELINE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final int EVENT_WINDOW_MINUTES = 5;

  private final DashboardViewService dashboardViewService;
  private final AiCommentService aiCommentService;
  private final DiaryMaintenanceService diaryMaintenanceService;
  private final DiaryViewService diaryViewService;
  private final PlayerStatusService playerStatusService;
  private final CurrentWebAccountService currentAccountService;
  private final PlayerSocialService playerSocialService;

  @GetMapping("/")
  public String index(Model model, Authentication authentication) {
    DashboardViewService.DashboardView dashboard = dashboardViewService.dashboard();
    model.addAttribute("dashboard", dashboard);
    model.addAttribute("timeline", timeline(dashboard.travelEntries(), playerSocialService.feed(authentication)));
    return "dashboard";
  }

  @PostMapping("/players/{playerId}/status")
  public String updateStatus(
      @PathVariable Long playerId,
      @RequestParam String status,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var updated = currentAccountService.current(authentication)
        .filter(account -> playerId.equals(account.getPlayerId()))
        .flatMap(account -> playerStatusService.updateByPlayerId(playerId, status, "WEB"));
    redirectAttributes.addFlashAttribute(updated.isPresent() ? "notice" : "error",
        updated.isPresent() ? "ステータスを更新しました。" : "自分のオンライン中プレイヤーだけ更新できます。");
    return "redirect:/players/" + playerId;
  }

  @GetMapping("/community")
  public String community() {
    return "redirect:/#timeline";
  }

  @PostMapping("/posts")
  public String createPost(
      @RequestParam String body,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var result = playerSocialService.createPost(authentication, body);
    redirectAttributes.addFlashAttribute(result.success() ? "notice" : "error", result.message());
    return "redirect:/#timeline";
  }

  @PostMapping("/posts/{postId}/like")
  public String toggleLike(
      @PathVariable Long postId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var result = playerSocialService.toggleLike(authentication, postId);
    redirectAttributes.addFlashAttribute(result.success() ? "notice" : "error", result.message());
    return "redirect:/#timeline";
  }

  @PostMapping(value = "/posts/{postId}/like.json", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public PlayerSocialService.LikeResult toggleLikeJson(
      @PathVariable Long postId,
      Authentication authentication) {
    return playerSocialService.toggleLike(authentication, postId);
  }

  @PostMapping("/posts/{postId}/delete")
  public String deletePost(
      @PathVariable Long postId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var result = playerSocialService.deletePost(authentication, postId);
    redirectAttributes.addFlashAttribute(result.success() ? "notice" : "error", result.message());
    return "redirect:/#timeline";
  }

  static List<TimelineItem> timeline(
      List<DashboardViewService.TravelEntry> events,
      List<PlayerSocialService.PostView> posts) {
    List<TimelineItem> timeline = new ArrayList<>(events.size() + posts.size());
    sampledEvents(events).stream().map(TimelineItem::event).forEach(timeline::add);
    posts.stream().map(TimelineItem::post).forEach(timeline::add);
    timeline.sort(Comparator.comparing(
        TimelineItem::occurredAt,
        Comparator.nullsLast(Comparator.reverseOrder())));
    return List.copyOf(timeline);
  }

  /**
   * Keeps the public feed readable when the game emits many events at once. Each five-minute
   * window contributes one stable pseudo-random event, while player-authored posts are never
   * sampled. Stability prevents the feed from changing merely because the page was refreshed.
   */
  static List<DashboardViewService.TravelEntry> sampledEvents(
      List<DashboardViewService.TravelEntry> events) {
    Map<LocalDateTime, DashboardViewService.TravelEntry> selected = new LinkedHashMap<>();
    for (DashboardViewService.TravelEntry event : events) {
      LocalDateTime occurredAt = parseTimelineTime(event.occurredAt());
      if (occurredAt == null) {
        selected.putIfAbsent(LocalDateTime.MIN.plusNanos(selected.size()), event);
        continue;
      }
      LocalDateTime window = occurredAt
          .withMinute((occurredAt.getMinute() / EVENT_WINDOW_MINUTES) * EVENT_WINDOW_MINUTES)
          .withSecond(0)
          .withNano(0);
      selected.merge(window, event, (current, candidate) ->
          eventSampleScore(window, candidate) > eventSampleScore(window, current)
              ? candidate : current);
    }
    return List.copyOf(selected.values());
  }

  private static LocalDateTime parseTimelineTime(String value) {
    try {
      return value == null ? null : LocalDateTime.parse(value, TIMELINE_TIME);
    } catch (RuntimeException ignored) {
      return null;
    }
  }

  private static int eventSampleScore(
      LocalDateTime window,
      DashboardViewService.TravelEntry event) {
    return java.util.Objects.hash(window, event.actor(), event.kind(), event.occurredAt());
  }

  @GetMapping("/players/{playerId}")
  public String player(@PathVariable Long playerId, Model model) {
    DashboardViewService.PlayerDetailView player = dashboardViewService.playerDetail(playerId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("player", player);
    return "player-detail";
  }

  @GetMapping("/server")
  public String server(Model model) {
    model.addAttribute("server", dashboardViewService.serverDetail());
    return "server-detail";
  }

  @GetMapping("/kills")
  public String kills(Model model) {
    model.addAttribute("kills", dashboardViewService.killDetail());
    return "kill-detail";
  }

  @GetMapping("/vehicles")
  public String vehicles(Model model) {
    model.addAttribute("vehicles", dashboardViewService.vehicleDetail());
    return "vehicle-detail";
  }

  @GetMapping("/exploration")
  public String exploration(Model model) {
    model.addAttribute("exploration", dashboardViewService.explorationDetail());
    return "exploration-detail";
  }

  @GetMapping("/diaries")
  public String diaries(Model model) {
    model.addAttribute("diaries", diaryViewService.archive());
    return "diaries";
  }

  @GetMapping("/diaries/{date}")
  public String diary(@PathVariable LocalDate date, Model model) {
    DiaryViewService.DiaryDetail diary = diaryViewService.detail(date)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("diary", diary);
    return "diary-detail";
  }

  @GetMapping("/maintenance/diaries")
  public String diaryMaintenance(Model model) {
    model.addAttribute("days", diaryMaintenanceService.days());
    return "diary-maintenance";
  }

  @GetMapping("/maintenance/diaries/{date}")
  public String diaryGenerationData(@PathVariable LocalDate date, Model model) {
    model.addAttribute("packet", diaryMaintenanceService.packet(date));
    return "diary-generation-data";
  }

  @GetMapping("/maintenance/diaries/{date}/edit")
  public String diaryEditor(@PathVariable LocalDate date, Model model) {
    model.addAttribute("packet", diaryMaintenanceService.packet(date));
    model.addAttribute("editorEnabled", aiCommentService.editorEnabled());
    return "diary-editor";
  }

  @PostMapping("/maintenance/diaries/{date}/edit")
  public String publishDiary(
      @PathVariable LocalDate date,
      @RequestParam String title,
      @RequestParam String body,
      @RequestParam(required = false, defaultValue = "") String editorKey,
      RedirectAttributes redirectAttributes) {
    try {
      aiCommentService.publish(date, title, body, editorKey);
      redirectAttributes.addFlashAttribute("notice", date + " の冒険日記を登録しました。");
      return "redirect:/maintenance/diaries/" + date;
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      redirectAttributes.addFlashAttribute("draftTitle", title);
      redirectAttributes.addFlashAttribute("draftBody", body);
      return "redirect:/maintenance/diaries/" + date + "/edit";
    }
  }

  public record TimelineItem(
      String itemType,
      Long postId,
      Long playerId,
      String actor,
      String kind,
      String occurredAt,
      String message,
      String coordinate,
      String tone,
      Long likeCount,
      boolean likedByCurrentAccount,
      boolean ownPost) {

    static TimelineItem event(DashboardViewService.TravelEntry event) {
      return new TimelineItem(
          "EVENT", null, null, event.actor(), event.kind(), event.occurredAt(),
          event.message(), event.coordinate(), event.tone(), null, false, false);
    }

    static TimelineItem post(PlayerSocialService.PostView post) {
      return new TimelineItem(
          "POST", post.id(), post.playerId(), post.playerName(), "つぶやき", post.createdAt(),
          post.body(), "", "community", post.likeCount(), post.likedByCurrentAccount(), post.own());
    }
  }

}
