package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_World;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_WorldRepository extends JpaRepository<M_World, Long> {

  Optional<M_World> findByWorldName(String worldName);
}
