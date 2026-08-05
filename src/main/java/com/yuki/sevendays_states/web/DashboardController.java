package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.AiCommentService;
import com.yuki.sevendays_states.service.CurrentWebAccountService;
import com.yuki.sevendays_states.service.PlayerSocialService;
import com.yuki.sevendays_states.service.PlayerStatusService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardViewService dashboardViewService;
  private final AiCommentService aiCommentService;
  private final DiaryMaintenanceService diaryMaintenanceService;
  private final DiaryViewService diaryViewService;
  private final PlayerStatusService playerStatusService;
  private final CurrentWebAccountService currentAccountService;
  private final PlayerSocialService playerSocialService;

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("dashboard", dashboardViewService.dashboard());
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
  public String community(Model model, Authentication authentication) {
    model.addAttribute("posts", playerSocialService.feed(authentication));
    return "community";
  }

  @PostMapping("/posts")
  public String createPost(
      @RequestParam String body,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var result = playerSocialService.createPost(authentication, body);
    redirectAttributes.addFlashAttribute(result.success() ? "notice" : "error", result.message());
    return "redirect:/community";
  }

  @PostMapping("/posts/{postId}/like")
  public String toggleLike(
      @PathVariable Long postId,
      Authentication authentication,
      RedirectAttributes redirectAttributes) {
    var result = playerSocialService.toggleLike(authentication, postId);
    redirectAttributes.addFlashAttribute(result.success() ? "notice" : "error", result.message());
    return "redirect:/community";
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

}
