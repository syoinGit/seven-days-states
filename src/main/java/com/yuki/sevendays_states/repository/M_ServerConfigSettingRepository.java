package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_ServerConfigSetting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_ServerConfigSettingRepository extends JpaRepository<M_ServerConfigSetting, Long> {

  Optional<M_ServerConfigSetting> findBySettingKey(String settingKey);
}
