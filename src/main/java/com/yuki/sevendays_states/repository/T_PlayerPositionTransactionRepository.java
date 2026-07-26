package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerPositionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerPositionTransactionRepository extends JpaRepository<T_PlayerPositionTransaction, Long> {

  boolean existsBySourceEventHash(String sourceEventHash);
}
