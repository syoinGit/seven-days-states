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
}
