package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_LevelXpSummaryTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_LevelXpSummaryTransactionRepository extends JpaRepository<T_LevelXpSummaryTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);

  Optional<T_LevelXpSummaryTransaction> findBySourceLogHash(String sourceLogHash);
}
