package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_EntityKillTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_EntityKillTransactionRepository extends JpaRepository<T_EntityKillTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);
}
