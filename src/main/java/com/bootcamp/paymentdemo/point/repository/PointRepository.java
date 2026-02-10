package com.bootcamp.paymentdemo.point.repository;

import com.bootcamp.paymentdemo.point.entity.PointTransaction;
import com.bootcamp.paymentdemo.point.consts.PointType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PointRepository extends JpaRepository<PointTransaction, Long> {
    // TODO 페이징 적용
    List<PointTransaction> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT SUM(p.amount) FROM PointTransaction p WHERE p.user.userId = :userId")
    BigDecimal calculatePointBalance(@Param("userId") Long userId);

    Optional<PointTransaction> findByOrderIdAndType(Long orderId, PointType pointType);

    List<PointTransaction> findByUser_UserIdAndTypeAndRemainingAmountGreaterThanAndExpiresAtAfterOrderByExpiresAtAsc(Long userId, PointType pointType, BigDecimal i, LocalDate now);

    List<PointTransaction> findByTypeAndRemainingAmountGreaterThanAndExpiresAtBefore(PointType pointType, BigDecimal i, LocalDate now);
}
