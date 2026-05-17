package com.myapi.middleware;

import com.myapi.utils.JWTUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/api/events/*")
public class AuthFilter implements Filter {
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Skip OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        
        String authHeader = httpRequest.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"errors\":true,\"message\":\"Missing or invalid token\"}");
            return;
        }
        
        String token = authHeader.substring(7);
        
        if (!JWTUtil.isTokenValid(token)) {
            httpResponse.setStatus(401);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"errors\":true,\"message\":\"Invalid or expired token\"}");
            return;
        }
        
        // Add user info to request attributes
        String email = JWTUtil.getEmailFromToken(token);
        int userId = JWTUtil.getUserIdFromToken(token);
        httpRequest.setAttribute("email", email);
        httpRequest.setAttribute("userId", userId);
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {}
}