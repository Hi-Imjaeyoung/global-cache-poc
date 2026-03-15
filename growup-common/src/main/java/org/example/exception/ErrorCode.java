package org.example.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNKNOWN_ERROR("400","알 수 없는 에러")
    ;
    private String httpCode;
    private String message;

    ErrorCode(String httpCode, String message) {
        this.message = message;
        this.httpCode = httpCode;
    };
}