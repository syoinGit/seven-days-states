package com.yuki.sevendays_states.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yuki.sevendays_states.entity.M_WebAccount;
import com.yuki.sevendays_states.repository.M_PlayerRepository;
import com.yuki.sevendays_states.repository.M_WebAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class WebAccountAdminServiceTests {

  @Mock
  private M_WebAccountRepository accountRepository;

  @Mock
  private M_PlayerRepository playerRepository;

  private PasswordEncoder passwordEncoder;
  private WebAccountAdminService service;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    service = new WebAccountAdminService(accountRepository, playerRepository, passwordEncoder);
  }

  @Test
  void changesPasswordUsingANewHash() {
    M_WebAccount account = account("PLAYER");
    when(accountRepository.findById(7L)).thenReturn(Optional.of(account));

    service.changePassword(7L, "new-password", "new-password");

    assertThat(passwordEncoder.matches("new-password", account.getPasswordHash())).isTrue();
    verify(accountRepository).save(account);
  }

  @Test
  void rejectsMismatchedConfirmationAndGuestAccountChanges() {
    M_WebAccount player = account("PLAYER");
    when(accountRepository.findById(7L)).thenReturn(Optional.of(player));

    assertThatThrownBy(() -> service.changePassword(7L, "new-password", "different-password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("一致しません");
    verify(accountRepository, never()).save(player);

    M_WebAccount guest = account("VIEWER");
    when(accountRepository.findById(8L)).thenReturn(Optional.of(guest));
    assertThatThrownBy(() -> service.changePassword(8L, "new-password", "new-password"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ゲスト");
    verify(accountRepository, never()).save(guest);
  }

  private static M_WebAccount account(String role) {
    M_WebAccount account = new M_WebAccount();
    account.setId(7L);
    account.setLoginId("player-a");
    account.setRole(role);
    account.setPasswordHash("old-hash");
    account.setEnabled(true);
    return account;
  }
}
