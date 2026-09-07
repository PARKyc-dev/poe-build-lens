package com.parkyc.poelens.common.code;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    MISSING_GAME_VERSION(HttpStatus.BAD_REQUEST, "Game version is required."),
    MISSING_BUILD_INPUT(HttpStatus.BAD_REQUEST, "PoB build input is required."),
    INVALID_POB_INPUT(HttpStatus.BAD_REQUEST, "Provide a raw Path of Building XML export."),
    INVALID_POBB_IN_LINK(HttpStatus.BAD_REQUEST, "올바른 pobb.in 공유 링크를 입력해 주세요."),
    POBB_IN_FETCH_FAILED(HttpStatus.BAD_GATEWAY, "pobb.in 빌드 코드를 불러올 수 없습니다."),
    AI_DAILY_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "금일 AI 분석 리미트에 도달했습니다."),
    AI_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "AI 분석 문장 생성에 실패했습니다. 잠시 후 다시 시도해 주세요.");

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
