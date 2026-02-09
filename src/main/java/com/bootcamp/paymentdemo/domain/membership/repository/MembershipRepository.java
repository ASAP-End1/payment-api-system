package com.bootcamp.paymentdemo.domain.membership.repository;

import com.bootcamp.paymentdemo.domain.membership.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, String> {
    Optional<Membership> findByGradeName(String gradeName);
}
