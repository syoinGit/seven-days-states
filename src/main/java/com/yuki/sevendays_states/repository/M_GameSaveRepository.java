package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_GameSave;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_GameSaveRepository extends JpaRepository<M_GameSave, Long> {

  Optional<M_GameSave> findByWorldNameAndGameName(String worldName, String gameName);

  boolean existsBySourceHash(String sourceHash);
}
