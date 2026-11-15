package com.example.telecom.error;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every failure to an RFC 9457 problem response so clients always get a
 * status code that matches the cause and a message they can show to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    /**
     * SQLite primary result code for UNIQUE, NOT NULL, CHECK and FOREIGN KEY
     * failures.
     */
    private static final int SQLITE_CONSTRAINT = 19;
    private static final String VALIDATION_FAILED = "Validation failed";
    private static final String INVALID_REQUEST = "Invalid request";
    private static final String CONFLICT_DETAIL = "The change conflicts with existing data, for example a duplicate value or a record that is still in use.";

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", ex.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(FieldValidationException.class)
    public ProblemDetail handleFieldValidation(FieldValidationException ex) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, ex.getMessage());
        body.setProperty("errors", ex.getErrors());
        return body;
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleDateParse(DateTimeParseException ex) {
        return problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
                "Dates must use the ISO format YYYY-MM-DD: '" + ex.getParsedString() + "'");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        return problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        LOG.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", CONFLICT_DETAIL);
    }

    /**
     * Also catches SQLite constraint failures, which Spring and Hibernate do not
     * classify as integrity violations because the SQLite driver reports no SQL
     * state.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        if (isConstraintViolation(ex)) {
            LOG.warn("Database constraint violation: {}", ex.getMessage());
            return problem(HttpStatus.CONFLICT, "Conflict", CONFLICT_DETAIL);
        }
        LOG.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "Something went wrong on the server. Please try again or contact support.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, VALIDATION_FAILED, "One or more fields are invalid.");
        body.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
                "The request body is missing, is not valid JSON, or contains a value of the wrong type.");
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Explains which query or path parameter had the wrong type, listing the
     * allowed values for enums.
     */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String name = ex.getPropertyName() != null ? ex.getPropertyName() : "parameter";
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            name = mismatch.getName();
        }
        Class<?> type = ex.getRequiredType();
        String detail;
        if (type != null && type.isEnum()) {
            detail = "Invalid value '" + ex.getValue() + "' for " + name + ". Allowed values: "
                    + Arrays.stream(type.getEnumConstants()).map(String::valueOf).collect(Collectors.joining(", "))
                    + ".";
        } else if (type != null && Temporal.class.isAssignableFrom(type)) {
            detail = "Invalid value '" + ex.getValue() + "' for " + name + ". Dates must use the format YYYY-MM-DD.";
        } else {
            detail = "Invalid value '" + ex.getValue() + "' for " + name + ".";
        }
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST, detail));
    }

    static boolean isConstraintViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && (sql.getErrorCode() & 0xFF) == SQLITE_CONSTRAINT) {
                return true;
            }
        }
        return false;
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        return problem;
    }
}
