package com.example.telecom.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void sqliteConstraintFailureBecomesAConflict() {
        // 2067 is SQLITE_CONSTRAINT_UNIQUE; its low byte is the primary code 19.
        RuntimeException wrapped = new IllegalStateException("could not execute statement",
                new SQLException("[SQLITE_CONSTRAINT_UNIQUE] UNIQUE constraint failed: customers.email", null, 2067));

        ProblemDetail problem = handler.handleUnexpected(wrapped);

        assertEquals(409, problem.getStatus());
        assertTrue(GlobalExceptionHandler.isConstraintViolation(wrapped));
    }

    @Test
    void otherFailuresStayServerErrorsWithoutLeakingDetails() {
        ProblemDetail problem = handler.handleUnexpected(new IllegalStateException("secret internals"));

        assertEquals(500, problem.getStatus());
        assertFalse(problem.getDetail().contains("secret"));
    }
}
