package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_Player;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_PlayerRepository extends JpaRepository<M_Player, Long> {

  Optional<M_Player> findByPlayerKey(String playerKey);
}
