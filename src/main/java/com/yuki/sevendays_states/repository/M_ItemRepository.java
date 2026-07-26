package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_Item;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_ItemRepository extends JpaRepository<M_Item, Long> {

  Optional<M_Item> findByItemKey(String itemKey);
}
