package com.chat5000.api;

import com.chat5000.ai.AiProviderException;
import com.chat5000.domain.ApiError;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * Translates Jakarta Validation and AI provider exceptions into structured JSON {@link ApiError} payloads.
 */
@Singleton
@Replaces(ConstraintExceptionHandler.class)
public class ValidationExceptionHandler implements ExceptionHandler<ConstraintViolationException, HttpResponse<ApiError>> {

    @Override
    @Produces
    public HttpResponse<ApiError> handle(HttpRequest request, ConstraintViolationException exception) {
        String details = exception.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return HttpResponse.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of(400, "validation_error", details, request.getPath()));
    }

    @Singleton
    public static class AiProviderExceptionHandler implements ExceptionHandler<AiProviderException, HttpResponse<ApiError>> {

        @Override
        @Produces
        public HttpResponse<ApiError> handle(HttpRequest request, AiProviderException exception) {
            return HttpResponse.status(HttpStatus.BAD_GATEWAY)
                    .body(ApiError.of(502, "ai_provider_error", exception.getMessage(), request.getPath()));
        }
    }
}
