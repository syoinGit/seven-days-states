package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerStateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerStateSnapshotRepository extends JpaRepository<T_PlayerStateSnapshot, Long> {

  boolean existsBySourceHash(String sourceHash);
}
