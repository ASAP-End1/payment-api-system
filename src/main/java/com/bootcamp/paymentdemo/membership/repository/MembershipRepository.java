package com.bootcamp.paymentdemo.membership.repository;

import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.entity.MembershipGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MembershipRepository extends JpaRepository<Membership, MembershipGrade> {
    Optional<Membership> findByGradeName(MembershipGrade gradeName);
}
