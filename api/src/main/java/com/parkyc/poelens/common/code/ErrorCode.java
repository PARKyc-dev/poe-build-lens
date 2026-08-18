package com.parkyc.poelens.common.code;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MISSING_GAME_VERSION(HttpStatus.BAD_REQUEST, "Game version is required."),
    MISSING_BUILD_INPUT(HttpStatus.BAD_REQUEST, "PoB build input is required."),
    INVALID_POB_INPUT(HttpStatus.BAD_REQUEST, "Provide a raw Path of Building XML export.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
