package com.couple.privatespace.identity;

import jakarta.servlet.http.HttpServletResponse;
import java.util.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class AuthErrorHandler {
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String,String>> uploadTooLarge(){return ResponseEntity.status(413).body(Map.of("code","FILE_TOO_LARGE","message","This file exceeds the upload limit."));}
    public record ErrorBody(String code, String message, Map<String,String> fieldErrors, String requestId) { }
    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<ErrorBody> known(ApiFailure failure, HttpServletResponse response) {
        return ResponseEntity.status(failure.status()).body(new ErrorBody(failure.code(), failure.getMessage(),
            failure.fields(), response.getHeader("X-Request-ID")));
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorBody> malformed(HttpServletResponse response) {
        return ResponseEntity.badRequest().body(new ErrorBody("INVALID_REQUEST", "Check your input and try again.", Map.of(), response.getHeader("X-Request-ID")));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorBody> conflict(HttpServletResponse response) {
        return ResponseEntity.status(409).body(new ErrorBody("REGISTRATION_CONFLICT", "This registration could not be completed. Check your invitation or sign in.", Map.of(), response.getHeader("X-Request-ID")));
    }
}
