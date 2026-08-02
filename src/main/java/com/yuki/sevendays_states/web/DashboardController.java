package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.AiCommentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardViewService dashboardViewService;
  private final AiCommentService aiCommentService;
  private final DisplayTimeFormatter displayTimeFormatter = new DisplayTimeFormatter();

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("dashboard", dashboardViewService.dashboard());
    return "dashboard";
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

  @GetMapping("/ai-comments")
  public String aiComments(Model model) {
    List<AiCommentView> comments = aiCommentService.history().stream()
        .map(comment -> new AiCommentView(
            comment.id(), comment.title(), comment.body(),
            displayTimeFormatter.format(comment.publishedAt()), comment.sourceType()))
        .toList();
    model.addAttribute("comments", comments);
    model.addAttribute("editorEnabled", aiCommentService.editorEnabled());
    return "ai-comments";
  }

  @PostMapping("/ai-comments")
  public String publishAiComment(
      @RequestParam String title,
      @RequestParam String body,
      @RequestParam(required = false, defaultValue = "") String editorKey,
      RedirectAttributes redirectAttributes) {
    try {
      aiCommentService.publish(title, body, editorKey);
      redirectAttributes.addFlashAttribute("notice", "AI観測コメントを公開しました。");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
      redirectAttributes.addFlashAttribute("draftTitle", title);
      redirectAttributes.addFlashAttribute("draftBody", body);
    }
    return "redirect:/ai-comments";
  }

  public record AiCommentView(
      Long id, String title, String body, String publishedAt, String sourceType) {
  }
}
