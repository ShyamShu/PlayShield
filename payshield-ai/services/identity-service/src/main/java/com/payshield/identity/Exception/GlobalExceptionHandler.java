package com.payshield.identity.Exception;


import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(this::toFieldError)
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                Errorcode.VALIDATION_FAILED,
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        List<FieldValidationError> fieldErrors =
                ex.getConstraintViolations()
                        .stream()
                        .map(v -> new FieldValidationError(
                                v.getPropertyPath().toString(),
                                v.getMessage()))
                        .toList();

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                Errorcode.VALIDATION_FAILED,
                "Request validation failed",
                request,
                fieldErrors
        );
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(
            Exception ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                Errorcode.INVALID_REQUEST,
                "Invalid request",
                request,
                List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                Errorcode.INVALID_REQUEST,
                "HTTP method not supported for this endpoint",
                request,
                List.of()
        );
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                Errorcode.RESOURCE_NOT_FOUND,
                ex.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(
            DuplicateResourceException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.CONFLICT,
                Errorcode.DUPLICATE_RESOURCE,
                ex.getMessage(),
                request,
                List.of()
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                Errorcode.INVALID_CREDENTIALS,
                "Invalid email or password",
                request,
                List.of()
        );
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.LOCKED,
                Errorcode.ACCOUNT_LOCKED,
                ex.getMessage(),
                request,
                List.of()
        );
    }


    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidToken(
            InvalidTokenException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                Errorcode.INVALID_TOKEN,
                "Invalid token",
                request,
                List.of()
        );
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleTokenExpired(
            TokenExpiredException ex,
            HttpServletRequest request) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                Errorcode.TOKEN_EXPIRED,
                ex.getMessage(),
                request,
                List.of()
        );
    }



    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Database integrity constraint violation. traceId={}",
                traceId(request));

        return buildResponse(
                HttpStatus.CONFLICT,
                Errorcode.DUPLICATE_RESOURCE,
                "The request conflicts with existing data",
                request,
                List.of()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception ex,
            HttpServletRequest request) {

        String traceId = traceId(request);

        log.error("Unexpected identity-service error. traceId={}",
                traceId, ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                Errorcode.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request,
                List.of()
        );
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(
                error.getField(),
                error.getDefaultMessage() != null
                        ? error.getDefaultMessage()
                        : "Invalid value"
        );
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            Errorcode code,
            String message,
            HttpServletRequest request,
            List<FieldValidationError> fieldErrors) {

        ApiErrorResponse body = new ApiErrorResponse(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code.name(),
                message,
                request.getRequestURI(),
                traceId(request),
                fieldErrors
        );

        return ResponseEntity.status(status).body(body);
    }

    private String traceId(HttpServletRequest request) {
        Object existing = request.getAttribute("traceId");

        if (existing instanceof String value && !value.isBlank()) {
            return value;
        }

        String traceId = UUID.randomUUID().toString();
        request.setAttribute("traceId", traceId);
        return traceId;
    }


    @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleAccessDenied(
                AccessDeniedException ex,
                HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "FORBIDDEN",
                "You do not have permission to access this resource",
                request.getRequestURI(),
                getTraceId(request),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(AuthenticationException.class)
        public ResponseEntity<ApiErrorResponse> handleAuthentication(
                AuthenticationException ex,
                HttpServletRequest request) {

        ApiErrorResponse response = new ApiErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "UNAUTHORIZED",
                "Authentication is required or invalid",
                request.getRequestURI(),
                getTraceId(request),
                List.of()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        private String getTraceId(HttpServletRequest request) {
                String traceId = request.getHeader("X-Trace-Id");
                return traceId != null && !traceId.isBlank()
                        ? traceId
                        : "N/A";
        }
}

