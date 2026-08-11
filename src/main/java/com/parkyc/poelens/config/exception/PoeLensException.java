package com.parkyc.poelens.config.exception;

import com.parkyc.poelens.common.code.ErrorCode;

public class PoeLensException extends RuntimeException {

    private final ErrorCode errorCode;

    public PoeLensException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
