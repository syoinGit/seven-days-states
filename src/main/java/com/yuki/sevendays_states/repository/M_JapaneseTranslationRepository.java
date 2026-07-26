package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_JapaneseTranslation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_JapaneseTranslationRepository extends JpaRepository<M_JapaneseTranslation, Long> {

  Optional<M_JapaneseTranslation> findByLocalizationKey(String localizationKey);
}
