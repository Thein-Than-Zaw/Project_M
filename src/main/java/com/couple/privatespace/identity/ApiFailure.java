package com.couple.privatespace.identity;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class ApiFailure extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, String> fields;
    public ApiFailure(int status, String code, String message) { this(status, code, message, Map.of()); }
    public ApiFailure(int status, String code, String message, Map<String,String> fields) {
        super(message); this.status = status; this.code = code; this.fields = fields;
    }
    public int status() { return status; }
    public String code() { return code; }
    public Map<String,String> fields() { return fields; }

    // Filter messages/codes are application constants, never client input.
    public static void write(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String requestId = response.getHeader("X-Request-ID");
        if (requestId == null) requestId = UUID.randomUUID().toString();
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
            + "\",\"fieldErrors\":{},\"requestId\":\"" + requestId + "\"}");
    }
}
