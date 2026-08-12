package com.parkyc.poelens.common.code;

public enum ResponseCode {
    OK("SUCCESS");

    private final String message;

    ResponseCode(String message) {
        this.message = message;
    }

    public String message() {
        return message;
    }
}
