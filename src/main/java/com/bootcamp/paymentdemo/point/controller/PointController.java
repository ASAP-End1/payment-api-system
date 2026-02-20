package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.common.dto.ApiResponse;
import com.bootcamp.paymentdemo.common.dto.PageResponse;
import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;


    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PointGetResponse>>> getPointHistory(
            Principal principal,
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        log.info("포인트 내역 조회 요청: email={}", principal.getName());
        PageResponse<PointGetResponse> response = pointService.getPointHistory(principal.getName(), pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(HttpStatus.OK, "포인트 내역 조회 성공", response));
    }
}
