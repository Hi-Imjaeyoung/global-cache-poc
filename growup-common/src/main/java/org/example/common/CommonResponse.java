package org.example.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@AllArgsConstructor
@Getter
@ToString
public class CommonResponse<T> implements Serializable {
    private final String message;
    private final T data;
    public static class ResponseBuilder<T> {

        private final String message;
        private T data;

        private ResponseBuilder(String message) {
            this.message = message;
        }

        public ResponseBuilder<T> data(T value) {
            data = value;
            return this;
        }

        public CommonResponse<T> build() {
            return new CommonResponse<>(this);
        }
    }

    public static <T> ResponseBuilder<T> builder(String message) {
        return new ResponseBuilder<>(message);
    }

    private CommonResponse(ResponseBuilder<T> responseBuilder) {
        message = responseBuilder.message;
        data = responseBuilder.data;
    }
}
