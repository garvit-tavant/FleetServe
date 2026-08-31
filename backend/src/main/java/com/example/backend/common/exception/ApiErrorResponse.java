package com.example.backend.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

public class ApiErrorResponse {

    private OffsetDateTime timestamp;

    private int status;

    private String error;

    private String message;

    private String path;

    private List<FieldValidationError> fieldErrors;

    public ApiErrorResponse() {
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<FieldValidationError> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(
            List<FieldValidationError> fieldErrors
    ) {
        this.fieldErrors = fieldErrors;
    }

    public static class FieldValidationError {

        private String field;

        private String message;

        private Object rejectedValue;

        public FieldValidationError() {
        }

        public FieldValidationError(
                String field,
                String message,
                Object rejectedValue
        ) {
            this.field = field;
            this.message = message;
            this.rejectedValue = rejectedValue;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Object getRejectedValue() {
            return rejectedValue;
        }

        public void setRejectedValue(Object rejectedValue) {
            this.rejectedValue = rejectedValue;
        }
    }
}