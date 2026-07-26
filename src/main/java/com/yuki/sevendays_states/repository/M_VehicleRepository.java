package com.yuki.sevendays_states.repository;

import com.yuki.sevendays_states.entity.M_Vehicle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface M_VehicleRepository extends JpaRepository<M_Vehicle, Long> {

  Optional<M_Vehicle> findByVehicleKey(String vehicleKey);
}
