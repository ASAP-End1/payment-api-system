package com.bootcamp.paymentdemo.user.repository;

import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserGradeHistoryRepository extends JpaRepository<UserGradeHistory, Long> {


    List<UserGradeHistory> findByUserUserIdOrderByUpdatedAtAsc(Long userId);
}
