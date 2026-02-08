package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    // TODO 페이징 적용
    // 포인트 내역 조회
    @GetMapping
    public ResponseEntity<List<PointGetResponse>> getPointHistory(Principal principal) {
        return ResponseEntity.status(HttpStatus.OK).body(pointService.getPointHistory(principal.getName()));
    }
}
