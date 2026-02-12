package com.bootcamp.paymentdemo.membership.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.membership.dto.MembershipSearchResponse;
import com.bootcamp.paymentdemo.membership.entity.Membership;
import com.bootcamp.paymentdemo.membership.service.MembershipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/membership")
public class MembershipController {
    private final MembershipService membershipService;

    // 멤버십 등급 정책 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipSearchResponse>>> getAllGrades() {
        List<Membership> grades = membershipService.getAllGradePolices();

        List<MembershipSearchResponse> response = grades.stream().map(MembershipSearchResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "멤버십 등급 정책 조회 성공", response));
    }
}
