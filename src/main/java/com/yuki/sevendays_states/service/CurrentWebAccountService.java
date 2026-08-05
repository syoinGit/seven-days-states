package com.yuki.sevendays_states.service;

import com.yuki.sevendays_states.entity.M_WebAccount;
import com.yuki.sevendays_states.repository.M_WebAccountRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentWebAccountService {

  private final M_WebAccountRepository accountRepository;

  public Optional<M_WebAccount> current(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || "anonymousUser".equals(authentication.getName())) {
      return Optional.empty();
    }
    return accountRepository.findByLoginId(authentication.getName())
        .filter(M_WebAccount::isEnabled);
  }
}
