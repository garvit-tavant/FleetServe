package com.example.backend.common.exception;

import com.example.backend.AssetManagamentService.exception.AssetRetirementBlockedException;
import com.example.backend.AssetManagamentService.exception.BusinessValidationException;
import com.example.backend.AssetManagamentService.exception.DuplicateResourceException;
import com.example.backend.AssetManagamentService.exception.ResourceNotFoundException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleBusinessValidation(
            BusinessValidationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(AssetRetirementBlockedException.class)
    public ResponseEntity<ApiErrorResponse>
    handleAssetRetirementBlocked(
            AssetRetirementBlockedException exception,
            HttpServletRequest request
    ) {
        String message =
                exception.getMessage()
                        + ". Blocking records: "
                        + String.join(
                        ", ",
                        exception.getBlockingRecords()
                );

        return buildResponse(
                HttpStatus.CONFLICT,
                message,
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse>
    handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(fieldError ->
                                new ApiErrorResponse.FieldValidationError(
                                        fieldError.getField(),
                                        fieldError.getDefaultMessage(),
                                        fieldError.getRejectedValue()
                                )
                        )
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request
    ) {
        List<ApiErrorResponse.FieldValidationError> fieldErrors =
                exception.getConstraintViolations()
                        .stream()
                        .map(violation ->
                                new ApiErrorResponse.FieldValidationError(
                                        violation.getPropertyPath()
                                                .toString(),
                                        violation.getMessage(),
                                        violation.getInvalidValue()
                                )
                        )
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse>
    handleUnreadableRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or contains invalid values",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handleMissingRequestValue(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse>
    handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "The operation violates a database constraint",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse>
    handleOptimisticLockFailure(
            ObjectOptimisticLockingFailureException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "The resource was modified by another request. "
                        + "Reload the latest data and retry.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler({
            PessimisticLockException.class,
            PessimisticLockingFailureException.class
    })
    public ResponseEntity<ApiErrorResponse>
    handlePessimisticLockFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "The resource is currently being updated by another "
                        + "request. Retry the operation.",
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse>
    handleUnexpectedException(
            Exception exception,
            HttpServletRequest request
    ) {
        /*
         * Log the full exception internally.
         * Do not expose stack traces or internal database details
         * in the API response.
         */

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected server error occurred",
                request.getRequestURI(),
                null
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<ApiErrorResponse.FieldValidationError> fieldErrors
    ) {
        ApiErrorResponse response =
                new ApiErrorResponse();

        response.setTimestamp(
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        response.setStatus(status.value());
        response.setError(status.getReasonPhrase());
        response.setMessage(message);
        response.setPath(path);
        response.setFieldErrors(fieldErrors);

        return ResponseEntity
                .status(status)
                .body(response);
    }
}