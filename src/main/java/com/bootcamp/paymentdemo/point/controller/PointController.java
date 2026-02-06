package com.bootcamp.paymentdemo.point.controller;

import com.bootcamp.paymentdemo.point.dto.PointGetResponse;
import com.bootcamp.paymentdemo.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/points")
public class PointController {

    private final PointService pointService;

    // TODO Spring Security 인증 사용자 정보 주입
    // TODO 페이징 적용
    @GetMapping
    public ResponseEntity<List<PointGetResponse>> getPointHistory(@RequestParam Long userId) {
        return ResponseEntity.status(HttpStatus.OK).body(pointService.getPointHistory(userId));
    }
}
