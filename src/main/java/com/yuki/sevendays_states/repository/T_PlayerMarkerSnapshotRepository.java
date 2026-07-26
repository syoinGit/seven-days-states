package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerMarkerSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerMarkerSnapshotRepository extends JpaRepository<T_PlayerMarkerSnapshot, Long> {

  boolean existsBySourceHash(String sourceHash);
}
