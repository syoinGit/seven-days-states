package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerCurrentState;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerCurrentStateRepository extends JpaRepository<T_PlayerCurrentState, Integer> {

  List<T_PlayerCurrentState> findByOnlineTrue();

  List<T_PlayerCurrentState> findByOnlineTrueAndPlayerEntityIdNotIn(Collection<Integer> playerEntityIds);

  List<T_PlayerCurrentState> findByCrossPlatformIdIn(Collection<String> crossPlatformIds);

  List<T_PlayerCurrentState> findByPlatformIdIn(Collection<String> platformIds);
}
