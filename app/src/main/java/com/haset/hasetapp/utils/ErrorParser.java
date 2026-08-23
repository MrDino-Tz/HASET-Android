package com.haset.hasetapp.utils;

import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.haset.hasetapp.models.ApiError;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Response;

/**
 * Single place that turns a Retrofit {@link Response} or a transport
 * {@link Throwable} into a typed {@link ApiError}.
 *
 * Two gap-closing behaviours vs. the previous per-repository duplication:
 *  1. It inspects the JSON body even on HTTP 200, because the backend may
 *     signal failure with {@code status:"error"} and a 2xx status code.
 *  2. It always extracts the stable {@code code} field so the UI can branch on
 *     the failure category instead of parsing English messages.
 */
public final class ErrorParser {

    private ErrorParser() {
    }

    public static ApiError fromResponse(Response<?> response) {
        int httpCode = response.code();

        // On a 2xx the backend may still report a logical failure inline.
        if (response.isSuccessful() && response.body() instanceof JsonObject) {
            JsonObject body = (JsonObject) response.body();
            String status = jsonString(body, "status", "success");
            if (isFailureStatus(status)) {
                String code = jsonString(body, "code", "business_rule");
                String message = firstString(body, "message", "error", "detail", "reason");
                return new ApiError(httpCode, code, message, null, classify(httpCode, code, message));
            }
            // Genuine success (or an accepted business status such as "received").
            return new ApiError(httpCode, null, null, null, ApiError.Kind.UNKNOWN);
        }

        String raw = null;
        if (response.errorBody() != null) {
            try {
                raw = response.errorBody().string();
            } catch (IOException ignored) {
                // fall through to a generic error
            }
        }

        JsonObject parsed = parse(raw);
        if (parsed != null) {
            String code = jsonString(parsed, "code", null);
            String message = firstString(parsed, "message", "error", "detail", "reason");
            Map<String, String> fieldErrors = parseFieldErrors(jsonObject(parsed, "errors"));
            return new ApiError(httpCode, code, message, fieldErrors, classify(httpCode, code, message));
        }

        return new ApiError(httpCode, null, null, null, classify(httpCode, null, null));
    }

    public static ApiError fromThrowable(Throwable throwable) {
        if (throwable instanceof IOException) {
            return new ApiError(0, "network_error", throwable.getMessage(),
                    null, ApiError.Kind.NETWORK);
        }
        return new ApiError(0, "unknown_error", throwable.getMessage(),
                null, ApiError.Kind.UNKNOWN);
    }

    public static ApiError success() {
        return new ApiError(200, null, null, null, ApiError.Kind.UNKNOWN);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Statuses that are NOT failures even when delivered on a 2xx response.
     *
     * These are accepted business outcomes from the payment webhook
     * (Snippe -> backend, server-to-server) and from withdrawal processing:
     *   - received:  webhook accepted and queued for processing
     *   - duplicate: webhook already handled (idempotent replay)
     *   - rejected:  webhook failed server-side validation (logged + audited,
     *                never surfaced to the app as an error)
     *   - ignored:   webhook not relevant to this system
     *
     * The Android app never calls the webhook endpoint, so these are tolerated
     * defensively rather than treated as errors. This is intentional and not a gap.
     */
    private static final List<String> NON_ERROR_STATUSES =
            Arrays.asList("received", "duplicate", "rejected", "ignored");

    private static boolean isFailureStatus(String status) {
        if (status == null) return false;
        if ("success".equals(status)) return false;
        return !NON_ERROR_STATUSES.contains(status);
    }

    private static ApiError.Kind classify(int httpCode, @Nullable String code, @Nullable String message) {
        if (code != null) {
            switch (code) {
                case "unauthenticated":
                    return ApiError.Kind.AUTH;
                case "validation_failed":
                    return ApiError.Kind.VALIDATION;
                case "forbidden":
                    return ApiError.Kind.FORBIDDEN;
                case "not_found":
                    return ApiError.Kind.NOT_FOUND;
                case "server_error":
                    return ApiError.Kind.SERVER;
                case "business_rule":
                case "payout_rejected":
                case "mfa_setup_failed":
                case "invalid_mfa_code":
                case "invalid_user_id":
                case "mfa_not_found":
                    return ApiError.Kind.BUSINESS;
                default:
                    break;
            }
        }
        if (httpCode == 401) return ApiError.Kind.AUTH;
        if (httpCode == 403) return ApiError.Kind.FORBIDDEN;
        if (httpCode == 404) return ApiError.Kind.NOT_FOUND;
        if (httpCode == 422) return ApiError.Kind.VALIDATION;
        if (httpCode >= 500) return ApiError.Kind.SERVER;
        if (httpCode > 0) return ApiError.Kind.BUSINESS;

        // No HTTP code (transport failure already handled) — infer from text.
        String text = (message == null ? "" : message).toLowerCase();
        if (text.contains("cooling-off") || text.contains("cooling off") || text.contains("cooldown")) {
            return ApiError.Kind.BUSINESS;
        }
        if (text.contains("security") || text.contains("hold") || text.contains("payout destination")) {
            return ApiError.Kind.BUSINESS;
        }
        return ApiError.Kind.UNKNOWN;
    }

    @Nullable
    private static JsonObject parse(@Nullable String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            JsonElement element = JsonParser.parseString(raw);
            if (element != null && element.isJsonObject()) {
                return element.getAsJsonObject();
            }
        } catch (RuntimeException ignored) {
            // not JSON
        }
        return null;
    }

    @Nullable
    private static Map<String, String> parseFieldErrors(@Nullable JsonObject errors) {
        if (errors == null) return null;
        Map<String, String> map = new HashMap<>();
        for (String key : errors.keySet()) {
            JsonElement value = errors.get(key);
            if (value == null || value.isJsonNull()) continue;
            if (value.isJsonArray() && value.getAsJsonArray().size() > 0) {
                map.put(key, value.getAsJsonArray().get(0).getAsString());
            } else {
                map.put(key, value.getAsString());
            }
        }
        return map.isEmpty() ? null : map;
    }

    private static String jsonString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return fallback;
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String firstString(JsonObject object, String... keys) {
        if (object == null) return "";
        for (String key : keys) {
            if (object.has(key) && !object.get(key).isJsonNull()) {
                try {
                    return object.get(key).getAsString();
                } catch (RuntimeException ignored) {
                    // try next key
                }
            }
        }
        return "";
    }

    @Nullable
    private static JsonObject jsonObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) return null;
        try {
            JsonElement element = object.get(key);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
