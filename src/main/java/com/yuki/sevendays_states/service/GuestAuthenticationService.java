package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.entity.M_WebAccount;
import com.yuki.sevendays_states.repository.M_WebAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestAuthenticationService {

  private static final String GUEST_LOGIN = "guest";

  private final M_WebAccountRepository accountRepository;

  public boolean login(HttpServletRequest request, HttpServletResponse response) {
    M_WebAccount guest = accountRepository.findByLoginId(GUEST_LOGIN)
        .filter(M_WebAccount::isEnabled)
        .filter(account -> "VIEWER".equals(account.getRole()))
        .orElse(null);
    if (guest == null) {
      return false;
    }
    Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
        guest.getLoginId(), null, List.of(new SimpleGrantedAuthority("ROLE_VIEWER")));
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    new HttpSessionSecurityContextRepository().saveContext(context, request, response);
    return true;
  }
}
