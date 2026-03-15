# API Error Contract

All non-2xx API responses should use this JSON shape:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "One or more fields are invalid.",
  "details": {
    "email": "Email must be valid."
  },
  "timestamp": "2026-03-15T10:05:23.455Z",
  "path": "/api/auth/register"
}
```

## Fields
- `code`: stable machine-readable identifier for frontend mapping.
- `message`: safe user-facing summary.
- `details`: optional field-level issues for form errors.
- `timestamp`: UTC instant.
- `path`: API route.

## Current codes
- `UNAUTHORIZED`: missing/invalid JWT for protected endpoint.
- `FORBIDDEN`: authenticated but not allowed to perform operation.
- `INVALID_CREDENTIALS`: login credentials invalid.
- `VALIDATION_ERROR`: bean validation failed.
- `MALFORMED_JSON`: request body missing or invalid JSON.
- `INVALID_REQUEST`: domain validation failed in service layer.
- `REQUEST_REJECTED`: state conflict or rule violation.
- `RATE_LIMITED`: endpoint rate limit exceeded.
- `INTERNAL_ERROR`: unhandled server error.

## Frontend message mapping guidance
- `VALIDATION_ERROR`: show field messages from `details` near inputs.
- `INVALID_CREDENTIALS`: show "Incorrect email or password."
- `UNAUTHORIZED`: route to login and show "Session expired. Please sign in again."
- `FORBIDDEN`: show "You don't have access to this action."
- `RATE_LIMITED`: show "Too many attempts. Try again in a minute."
- Any unknown code: fallback to `message` and log for telemetry.
