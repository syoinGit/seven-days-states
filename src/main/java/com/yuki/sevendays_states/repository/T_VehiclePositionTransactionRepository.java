package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_VehiclePositionTransaction;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_VehiclePositionTransactionRepository extends JpaRepository<T_VehiclePositionTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);

  Optional<T_VehiclePositionTransaction> findTopByAttributedPlayerIdAndMovementValidTrueAndOccurredAtBetweenOrderByOccurredAtDescIdDesc(
      Long playerId, OffsetDateTime from, OffsetDateTime to);
}
