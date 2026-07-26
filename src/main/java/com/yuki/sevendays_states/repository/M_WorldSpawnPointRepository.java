package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_WorldSpawnPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_WorldSpawnPointRepository extends JpaRepository<M_WorldSpawnPoint, Long> {

  boolean existsBySourceHash(String sourceHash);

  List<M_WorldSpawnPoint> findByWorldName(String worldName);
}
