package com.haset.hasetapp.models;

import androidx.annotation.Nullable;

import java.util.Map;

/**
 * Typed representation of a failure returned by the API or the network layer.
 *
 * Replaces the previous free-form String errors so the UI can react to the
 * failure category (network / auth / validation / server / business) instead of
 * pattern-matching on English text.
 */
public final class ApiError {

    public enum Kind {
        NETWORK,
        AUTH,
        VALIDATION,
        FORBIDDEN,
        NOT_FOUND,
        SERVER,
        BUSINESS,
        UNKNOWN
    }

    private final int httpCode;
    @Nullable private final String code;
    @Nullable private final String message;
    @Nullable private final Map<String, String> fieldErrors;
    private final Kind kind;

    public ApiError(int httpCode,
                    @Nullable String code,
                    @Nullable String message,
                    @Nullable Map<String, String> fieldErrors,
                    Kind kind) {
        this.httpCode = httpCode;
        this.code = code;
        this.message = message;
        this.fieldErrors = fieldErrors;
        this.kind = kind;
    }

    public int getHttpCode() {
        return httpCode;
    }

    @Nullable
    public String getCode() {
        return code;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isSuccess() {
        return kind == Kind.UNKNOWN && message == null;
    }

    /** Best-effort user-facing text; the display layer may localise further. */
    public String getUserMessage() {
        if (message != null && !message.isEmpty()) {
            return message;
        }
        switch (kind) {
            case NETWORK:
                return "Network error. Please check your connection and try again.";
            case AUTH:
                return "Your session has expired. Please sign in again.";
            case VALIDATION:
                return "Please check the highlighted fields and try again.";
            case FORBIDDEN:
                return "You are not allowed to perform this action.";
            case NOT_FOUND:
                return "The requested information was not found.";
            case SERVER:
                return "Something went wrong on our servers. Please try again later.";
            default:
                return "Something went wrong. Please try again.";
        }
    }
}
