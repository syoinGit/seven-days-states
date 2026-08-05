package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.service.GuestAuthenticationService;
import com.yuki.sevendays_states.service.WebAccountAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class WebAccountController {

  private final WebAccountAdminService accountAdminService;
  private final GuestAuthenticationService guestAuthenticationService;

  @GetMapping("/login")
  public String login() {
    return "login";
  }

  @PostMapping("/guest-login")
  public String guestLogin(
      HttpServletRequest request,
      HttpServletResponse response,
      RedirectAttributes redirectAttributes) {
    if (!guestAuthenticationService.login(request, response)) {
      redirectAttributes.addFlashAttribute("error", "ゲストログインを準備できませんでした。");
      return "redirect:/login";
    }
    return "redirect:/community";
  }

  @GetMapping("/maintenance/accounts")
  public String accounts(Model model) {
    model.addAttribute("accountAdmin", accountAdminService.view());
    return "account-maintenance";
  }

  @PostMapping("/maintenance/accounts")
  public String createAccount(
      @RequestParam String loginId,
      @RequestParam String password,
      @RequestParam(defaultValue = "PLAYER") String role,
      @RequestParam(required = false) Long playerId,
      RedirectAttributes redirectAttributes) {
    try {
      accountAdminService.create(loginId, password, role, playerId);
      redirectAttributes.addFlashAttribute("notice", "アカウントを作成しました。");
    } catch (IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/maintenance/accounts";
  }
}
