package com.bootcamp.paymentdemo.domain.user.repository;

import com.bootcamp.paymentdemo.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.currentGrade WHERE u.email = :email")
    Optional<User> findByEmailWithGrade(@Param("email") String email);
}
