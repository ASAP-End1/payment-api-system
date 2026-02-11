package com.bootcamp.paymentdemo.refund.consts;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import static com.bootcamp.paymentdemo.refund.consts.Constants.*;

@Getter
@RequiredArgsConstructor
public enum ErrorEnum {
    ERR_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, MSG_PAYMENT_NOT_FOUND),
    ERR_REFUND_ALREADY_PROCESSED(HttpStatus.CONFLICT, MSG_REFUND_ALREADY_PROCESSED),
    ERR_REFUND_INVALID_STATUS(HttpStatus.CONFLICT, MSG_REFUND_INVALID_STATUS);

    private final HttpStatus status;
    private final String message;


}
