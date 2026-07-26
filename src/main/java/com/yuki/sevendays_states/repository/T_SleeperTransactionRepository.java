package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_SleeperTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_SleeperTransactionRepository extends JpaRepository<T_SleeperTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);

  Optional<T_SleeperTransaction> findBySourceLogHash(String sourceLogHash);
}
