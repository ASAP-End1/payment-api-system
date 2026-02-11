package com.bootcamp.paymentdemo.user.repository;

import com.bootcamp.paymentdemo.user.entity.UserGradeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserGradeHistoryRepository extends JpaRepository<UserGradeHistory, Long> {

    // 특정 사용자의 등급 변경 이력 조회
    List<UserGradeHistory> findByUserUserIdOrderByUpdatedAtAsc(Long userId);
}
