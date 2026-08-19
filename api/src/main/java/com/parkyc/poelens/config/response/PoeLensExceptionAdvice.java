package com.parkyc.poelens.config.response;

import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.common.dto.CommonDTO;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PoeLensExceptionAdvice {
    private static final Logger log = LogManager.getLogger(PoeLensExceptionAdvice.class);

    @ExceptionHandler(PoeLensException.class)
    public ResponseEntity<CommonDTO.Exception> handlePoeLensException(PoeLensException exception) {
        log.warn("PoeLens 예외 처리: 코드={}", exception.errorCode());
        return error(exception.errorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonDTO.Exception> handleValidationException(MethodArgumentNotValidException exception) {
        log.warn("잘못된 빌드 분석 요청 거부: 오류 수={}", exception.getErrorCount());
        return error(ErrorCode.MISSING_GAME_VERSION);
    }

    private ResponseEntity<CommonDTO.Exception> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(new CommonDTO.Exception(errorCode.name(), errorCode.message()));
    }
}
