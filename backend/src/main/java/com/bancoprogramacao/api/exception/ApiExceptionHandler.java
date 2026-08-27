package com.bancoprogramacao.api.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BankBusinessException.class)
    ResponseEntity<ApiErrorResponse> handleBusiness(BankBusinessException exception) {
        return response(exception.getStatus(), exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiFieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "Dados da requisição inválidos.", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return response(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido.", List.of());
    }

    private ApiFieldError toFieldError(FieldError fieldError) {
        String message = fieldError.getDefaultMessage() == null
                ? "Valor inválido."
                : fieldError.getDefaultMessage();
        return new ApiFieldError(fieldError.getField(), message);
    }

    private ResponseEntity<ApiErrorResponse> response(
            HttpStatus status,
            String message,
            List<ApiFieldError> fieldErrors
    ) {
        ApiErrorResponse body = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                fieldErrors
        );
        return ResponseEntity.status(status).body(body);
    }
}

