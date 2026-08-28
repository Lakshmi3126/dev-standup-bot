package com.example.standupbot.exception;

import com.example.standupbot.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StandupBotException.class)
    public ResponseEntity<ErrorResponse> handleStandupBotException(
            StandupBotException exception, HttpServletRequest request) {
        HttpStatus status = statusFor(exception);
        logException(status, exception);
        return ResponseEntity.status(status).body(errorBody(status, exception.getErrorCode(), exception.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Request validation failed");
        log.warn("Validation failed on {}: {}", request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception, HttpServletRequest request) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse("Request validation failed");
        log.warn("Constraint violation on {}: {}", request.getRequestURI(), message);
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message, request));
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        log.warn("Malformed request on {}: {}", request.getRequestURI(), exception.getMessage());
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Request is invalid", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected error occurred",
                        request));
    }

    private static HttpStatus statusFor(StandupBotException exception) {
        return switch (exception) {
            case ResourceNotFoundException ignored -> HttpStatus.NOT_FOUND;
            case DuplicateSubmissionException ignored -> HttpStatus.CONFLICT;
            case InvalidTimezoneException ignored -> HttpStatus.BAD_REQUEST;
            case TeamMemberMismatchException ignored -> HttpStatus.BAD_REQUEST;
            case SlackDeliveryException ignored -> HttpStatus.BAD_GATEWAY;
            case ValidationException ignored -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static void logException(HttpStatus status, StandupBotException exception) {
        if (status.is5xxServerError()) {
            log.error("[{}] {}", exception.getErrorCode(), exception.getMessage(), exception);
        } else {
            log.warn("[{}] {}", exception.getErrorCode(), exception.getMessage());
        }
    }

    private static ErrorResponse errorBody(HttpStatus status, String error, String message, HttpServletRequest request) {
        return new ErrorResponse(Instant.now(), status.value(), error, message, request.getRequestURI());
    }
}
