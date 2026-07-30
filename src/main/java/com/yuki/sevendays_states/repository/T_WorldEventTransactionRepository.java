package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_WorldEventTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_WorldEventTransactionRepository extends JpaRepository<T_WorldEventTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);
}
