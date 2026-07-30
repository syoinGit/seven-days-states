package com.yuki.sevendays_states.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardViewService dashboardViewService;

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
}
