package com.flatox.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String phone;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            phone = jwtService.extractPhone(jwt);
            String approvalStatus = jwtService.extractApprovalStatus(jwt);

            // Access Control checking for PENDING approvalStatus
            if ("PENDING".equalsIgnoreCase(approvalStatus)) {
                String uri = request.getRequestURI();
                // Block access to core features (like /api/payments, /api/notices, /api/complaints, etc.)
                // Allow only public/utility endpoints such as /api/auth/status and /api/society/admin-contact, or registration/lookup paths
                if (!isAllowedPendingUri(uri)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":\"PENDING\",\"message\":\"Waiting for Admin Approval\"}");
                    return;
                }
            }

            if (phone != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                if (!jwtService.isTokenExpired(jwt)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            phone, null, Collections.emptyList()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Ignore / do not authenticate if parsing fails
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedPendingUri(String uri) {
        return uri.equals("/api/auth/status") || uri.startsWith("/api/auth/status/") ||
               uri.equals("/api/society/admin-contact") || uri.startsWith("/api/society/admin-contact/") ||
               uri.equals("/api/auth/register") ||
               uri.equals("/api/auth/send-otp") ||
               uri.equals("/api/auth/verify-otp") ||
               uri.equals("/api/auth/check-phone") ||
               uri.equals("/api/auth/check-email") ||
               uri.startsWith("/api/apartments") ||
               uri.startsWith("/api/flats");
    }
}
