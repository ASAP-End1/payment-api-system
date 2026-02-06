package com.bootcamp.paymentdemo.product.consts;

import lombok.Getter;

@Getter
public enum ProductStatus {
    FOR_SALE("FOR_SALE"), // 판매중
    SOLD_OUT("SOLD_OUT"), // 품절
    DISCONTINUED("DISCONTINUED"); // 단종

    private final String statusName;

    ProductStatus(String statusName) {
        this.statusName = statusName;
    }
}
