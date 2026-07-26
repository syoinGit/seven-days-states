package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_WorldPoi;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_WorldPoiRepository extends JpaRepository<M_WorldPoi, Long> {

  boolean existsBySourceHash(String sourceHash);

  List<M_WorldPoi> findByWorldName(String worldName);
}
