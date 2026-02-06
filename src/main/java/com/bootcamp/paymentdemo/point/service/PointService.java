package com.bootcamp.paymentdemo.point.service;

import com.bootcamp.paymentdemo.point.repository.PointRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final PointRepository pointRepository;
}
