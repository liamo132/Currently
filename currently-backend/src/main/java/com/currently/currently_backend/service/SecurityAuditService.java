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

    // Audit Logging: records a successful Register event with sanitized principal details.
    public void logRegistrationSuccess(String email) {
        log("registration_success", "principal=" + safe(email));
    }

    // Audit Logging: records a successful Login event for security review.
    public void logLoginSuccess(String email) {
        log("login_success", "principal=" + safe(email));
    }

    // Audit Logging: records failed Login attempts without logging sensitive password data.
    public void logLoginFailure(String email, String reason) {
        log("login_failure", "principal=" + safe(email) + " reason=" + safe(reason));
    }

    // Audit Logging: records failed Bills Vault PIN checks for abuse detection.
    public void logVaultPinFailure(Long userId, String action) {
        log("vault_pin_failure", "userId=" + userId + " action=" + safe(action));
    }

    // Audit Logging: records successful Bills Vault PIN checks.
    public void logVaultPinSuccess(Long userId, String action) {
        log("vault_pin_success", "userId=" + userId + " action=" + safe(action));
    }

    // Audit Logging: records Bills Vault file actions such as upload, list, download, and delete.
    public void logVaultAction(Long userId, String action, String details) {
        log(action, "userId=" + userId + " " + safe(details));
    }

    // Security helper: writes method, path, IP, event name, and sanitized details to the SECURITY_AUDIT logger.
    private void log(String event, String details) {
        HttpServletRequest request = currentRequest();
        String method = request != null ? request.getMethod() : "-";
        String path = request != null ? request.getRequestURI() : "-";
        String ip = request != null ? request.getRemoteAddr() : "-";
        auditLog.info("event={} method={} path={} ip={} {}", event, method, path, ip, details);
    }

    // Security helper: reads the current HTTP request so Audit Logging can include path and IP context.
    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        return null;
    }

    // Security helper: removes control characters to prevent log injection in Audit Logging output.
    private String safe(String value) {
        if (value == null) {
            return "-";
        }
        return value.replaceAll("[\\r\\n\\t]", "_").trim();
    }
}
