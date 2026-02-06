package com.bootcamp.paymentdemo.point.entity;

public enum PointType {
    SPENT,      // 사용
    REFUNDED,   // 복구
    EARNED,     // 적립
    CANCELED,   // 취소
    EXPIRED     // 소멸
}
