package com.yuki.sevendays_states.web;

import com.yuki.sevendays_states.entity.M_WebAccount;
import com.yuki.sevendays_states.service.CurrentWebAccountService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class WebAuthModelAdvice {

  private final CurrentWebAccountService currentAccountService;

  @ModelAttribute
  public void addAuthenticationModel(Model model, Authentication authentication) {
    Optional<M_WebAccount> account = currentAccountService.current(authentication);
    model.addAttribute("loggedIn", account.isPresent());
    model.addAttribute("currentAccountLogin", account.map(M_WebAccount::getLoginId).orElse(null));
    model.addAttribute("currentPlayerId", account.flatMap(value -> Optional.ofNullable(value.getPlayerId())).orElse(null));
    model.addAttribute("currentAdmin", account.map(value -> "ADMIN".equals(value.getRole())).orElse(false));
    model.addAttribute("currentViewer", account.map(value -> "VIEWER".equals(value.getRole())).orElse(false));
  }
}
