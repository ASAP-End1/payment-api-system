package com.bootcamp.paymentdemo.user.entity;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Table(name = "user_grade_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGradeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_history_id")
    private Long gradeHistoryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_grade_id")
    private Membership fromGrade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_grade_id", nullable = false)
    private Membership toGrade;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "trigger_order_id")
    private Long triggerOrderId;

    @Column(name = "reason", length = 100)
    private String reason;


    public static UserGradeHistory createInitial(User user, Membership toGrade) {
        UserGradeHistory history = new UserGradeHistory();
        history.user = user;
        history.fromGrade = null;
        history.toGrade = toGrade;
        history.updatedAt = LocalDateTime.now();
        history.reason = "회원가입";
        return history;
    }


    public static UserGradeHistory createChange(
            User user,
            Membership fromGrade,
            Membership toGrade,
            Long triggerOrderId,
            String reason
    ) {
        UserGradeHistory history = new UserGradeHistory();
        history.user = user;
        history.fromGrade = fromGrade;
        history.toGrade = toGrade;
        history.updatedAt = LocalDateTime.now();
        history.triggerOrderId = triggerOrderId;
        history.reason = reason;
        return history;
    }

}
