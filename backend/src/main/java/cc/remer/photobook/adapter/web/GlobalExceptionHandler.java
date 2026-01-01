package cc.remer.photobook.adapter.web;

import cc.remer.photobook.adapter.web.model.ErrorResponse;
import cc.remer.photobook.usecase.AuthenticationService;
import cc.remer.photobook.usecase.UserService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationService.InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(AuthenticationService.InvalidCredentialsException ex) {
        log.warn("Invalid credentials: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("INVALID_CREDENTIALS")
                .message(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationService.InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(AuthenticationService.InvalidTokenException ex) {
        log.warn("Invalid token: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("INVALID_TOKEN")
                .message(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.UNAUTHORIZED.value())
                .error("AUTHENTICATION_ERROR")
                .message("Authentication failed");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.FORBIDDEN.value())
                .error("ACCESS_DENIED")
                .message("Access denied");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(UserService.UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserService.UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.NOT_FOUND.value())
                .error("USER_NOT_FOUND")
                .message(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UserService.DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(UserService.DuplicateEmailException ex) {
        log.warn("Duplicate email: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("DUPLICATE_EMAIL")
                .message(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_ARGUMENT")
                .message(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(message.isEmpty() ? "Validation failed" : message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {}", ex.getMessage());
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_ERROR")
                .message(message.isEmpty() ? "Validation failed" : message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);
        ErrorResponse error = new ErrorResponse()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("INTERNAL_ERROR")
                .message("An unexpected error occurred");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
