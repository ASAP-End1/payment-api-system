package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {
    // TODO 페이징 적용
    List<PointTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    // TODO User 엔티티 연결하면 p.userId -> p.user.id로 수정
    @Query("SELECT SUM(p.amount) FROM PointTransaction p WHERE p.userId=:userId")
    Long calculateBalance(@Param("userId") Long userId);
}
