package org.example.exception;

import lombok.Getter;

@Getter
public class GrouException extends RuntimeException {

    private final ErrorCode errorCode;

    public GrouException(ErrorCode errorCode){
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
