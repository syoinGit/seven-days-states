package com.yuki.sevendays_states.web;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

  private final DashboardViewService dashboardViewService;

  @GetMapping("/")
  public String index(Model model) {
    model.addAttribute("dashboard", dashboardViewService.dashboard());
    return "dashboard";
  }
}
