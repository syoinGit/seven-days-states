package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerLeaveTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerLeaveTransactionRepository extends JpaRepository<T_PlayerLeaveTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);
}
