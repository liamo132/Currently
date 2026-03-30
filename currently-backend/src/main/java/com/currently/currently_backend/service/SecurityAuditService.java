package com.currently.currently_backend.service;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class SecurityAuditService {

    private static final Logger auditLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    public void logRegistrationSuccess(String email) {
        log("registration_success", "principal=" + safe(email));
    }

    public void logLoginSuccess(String email) {
        log("login_success", "principal=" + safe(email));
    }

    public void logLoginFailure(String email, String reason) {
        log("login_failure", "principal=" + safe(email) + " reason=" + safe(reason));
    }

    public void logVaultPinFailure(Long userId, String action) {
        log("vault_pin_failure", "userId=" + userId + " action=" + safe(action));
    }

    public void logVaultPinSuccess(Long userId, String action) {
        log("vault_pin_success", "userId=" + userId + " action=" + safe(action));
    }

    public void logVaultAction(Long userId, String action, String details) {
        log(action, "userId=" + userId + " " + safe(details));
    }

    private void log(String event, String details) {
        HttpServletRequest request = currentRequest();
        String method = request != null ? request.getMethod() : "-";
        String path = request != null ? request.getRequestURI() : "-";
        String ip = request != null ? request.getRemoteAddr() : "-";
        auditLog.info("event={} method={} path={} ip={} {}", event, method, path, ip, details);
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    private String safe(String value) {
        if (value == null) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t]", "_").trim();
    }
}
