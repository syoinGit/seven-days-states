package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_WebAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_WebAccountRepository extends JpaRepository<M_WebAccount, Long> {

  Optional<M_WebAccount> findByLoginId(String loginId);

  boolean existsByLoginId(String loginId);

  Optional<M_WebAccount> findByPlayerId(Long playerId);

  List<M_WebAccount> findAllByOrderByLoginIdAsc();
}
