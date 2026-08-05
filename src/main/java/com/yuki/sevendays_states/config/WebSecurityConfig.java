package com.yuki.sevendays_states.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .authorizeHttpRequests(authorize -> authorize
            .requestMatchers(
                "/login", "/guest-login", "/css/**", "/img/**", "/js/**", "/favicon.ico", "/error")
            .permitAll()
            .requestMatchers(
                HttpMethod.GET, "/", "/community", "/players/**", "/kills", "/vehicles", "/exploration",
                "/diaries", "/diaries/**", "/server")
            .permitAll()
            .requestMatchers("/maintenance/**").hasRole("ADMIN")
            .requestMatchers(
                HttpMethod.POST, "/players/*/status", "/posts", "/posts/*/like",
                "/posts/*/like.json", "/posts/*/delete")
            .hasAnyRole("PLAYER", "ADMIN")
            .anyRequest().permitAll())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/", true)
            .permitAll())
        .logout(logout -> logout
            .logoutSuccessUrl("/")
            .permitAll());
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
