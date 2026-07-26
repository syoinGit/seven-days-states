package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerJoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerJoinTransactionRepository extends JpaRepository<T_PlayerJoinTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);
}
