package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_Block;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_BlockRepository extends JpaRepository<M_Block, Long> {

  Optional<M_Block> findByBlockKey(String blockKey);
}
