package com.bootcamp.paymentdemo.refund.portOne.util;

import org.springframework.http.HttpStatus;

public class PortOneErrorCase {

    public static HttpStatus caseToHttpStatus(String type) {
        if(type == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return switch (type) {
            case "INVALID_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
            case "PAYMENT_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "PAYMENT_ALREADY_CANCELLED"  -> HttpStatus.CONFLICT;
            case "PG_PROVIDER" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
