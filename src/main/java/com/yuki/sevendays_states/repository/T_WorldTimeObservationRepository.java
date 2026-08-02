package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_WorldTimeObservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_WorldTimeObservationRepository extends JpaRepository<T_WorldTimeObservation, Long> {

  boolean existsBySourceHash(String sourceHash);
}
