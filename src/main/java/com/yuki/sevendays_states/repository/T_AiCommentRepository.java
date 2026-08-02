package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.T_AiComment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface T_AiCommentRepository extends JpaRepository<T_AiComment, Long> {

  Optional<T_AiComment> findTopByOrderByPublishedAtDescIdDesc();

  List<T_AiComment> findTop50ByOrderByPublishedAtDescIdDesc();
}
