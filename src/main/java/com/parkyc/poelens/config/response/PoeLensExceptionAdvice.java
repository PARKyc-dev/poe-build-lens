package com.parkyc.poelens.config.response;

import com.parkyc.poelens.common.code.ErrorCode;
import com.parkyc.poelens.common.dto.CommonDTO;
import com.parkyc.poelens.config.exception.PoeLensException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PoeLensExceptionAdvice {

    @ExceptionHandler(PoeLensException.class)
    public ResponseEntity<CommonDTO.Exception> handlePoeLensException(PoeLensException exception) {
        return error(exception.errorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonDTO.Exception> handleValidationException(MethodArgumentNotValidException exception) {
        return error(ErrorCode.MISSING_BUILD_INPUT);
    }

    private ResponseEntity<CommonDTO.Exception> error(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status())
                .body(new CommonDTO.Exception(errorCode.name(), errorCode.message()));
    }
}
