package com.classservice.common.exception;

public class BillAlreadyIssuedException extends RuntimeException {
    public BillAlreadyIssuedException(String message) {
        super(message);
    }
}
