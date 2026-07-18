package com.compliance.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${app.internal-api-key}")
    private String internalApiKey;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/internal/") && !path.startsWith("/api/v1/obligations/internal/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = request.getHeader("X-Internal-Api-Key");
        if (key == null || !key.equals(internalApiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid internal API key");
            return;
        }
        chain.doFilter(request, response);
    }
}
