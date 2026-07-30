package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_VehiclePositionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_VehiclePositionTransactionRepository extends JpaRepository<T_VehiclePositionTransaction, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);
}
