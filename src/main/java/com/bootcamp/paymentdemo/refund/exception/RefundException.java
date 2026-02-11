package com.bootcamp.paymentdemo.refund.exception;

import com.bootcamp.paymentdemo.common.exception.ServiceException;
import com.bootcamp.paymentdemo.refund.consts.ErrorEnum;

public class RefundException extends ServiceException {
    public RefundException(ErrorEnum errorEnum) {
        super(errorEnum.getStatus().name(), errorEnum.getMessage(), errorEnum.getStatus());
    }
}
