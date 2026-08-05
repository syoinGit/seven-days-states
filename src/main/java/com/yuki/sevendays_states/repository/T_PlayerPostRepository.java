package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_PlayerPost;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_PlayerPostRepository extends JpaRepository<T_PlayerPost, Long> {

  List<T_PlayerPost> findTop50ByOrderByCreatedAtDescIdDesc();
}
