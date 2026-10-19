package com.meridian.common.web;

import com.meridian.change.domain.ChangeWorkflowException;
import com.meridian.change.domain.FourEyesViolationException;
import com.meridian.common.NotFoundException;
import com.meridian.contract.adapter.document.DocumentReadException;
import com.meridian.routing.domain.InvalidRulesException;
import com.meridian.routing.domain.StaleRuleSetException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(InvalidRulesException.class)
    ProblemDetail invalidRules(InvalidRulesException e) {
        ProblemDetail problem = problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid rules", e.getMessage());
        problem.setProperty("problems", e.problems());
        return problem;
    }

    @ExceptionHandler({ChangeWorkflowException.class, StaleRuleSetException.class})
    ProblemDetail conflict(RuntimeException e) {
        return problem(HttpStatus.CONFLICT, "Change not allowed", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "Bad request", e.getMessage());
    }

    @ExceptionHandler(DocumentReadException.class)
    ProblemDetail unreadableDocument(DocumentReadException e) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Document not readable", e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail tooLarge(MaxUploadSizeExceededException e) {
        return problem(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", "The file is larger than 5 MB.");
    }

    @ExceptionHandler(FourEyesViolationException.class)
    ProblemDetail fourEyes(FourEyesViolationException e) {
        return problem(HttpStatus.FORBIDDEN, "Second person required", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
