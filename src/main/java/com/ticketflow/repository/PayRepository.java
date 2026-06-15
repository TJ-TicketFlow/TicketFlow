package com.ticketflow.repository;

import com.ticketflow.entity.Pay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PayRepository extends JpaRepository<Pay, Long> {
    Optional<Pay> findByMerchantUid(String merchantUid);
    List<Pay> findByReservation_SelectedSeat_User_UserId(String userId);
}
