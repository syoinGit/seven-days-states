package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_ServerMetric;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_ServerMetricRepository extends JpaRepository<T_ServerMetric, Long> {

  boolean existsBySourceLogHash(String sourceLogHash);

  Optional<T_ServerMetric> findTopByOrderByOccurredAtDesc();
}
