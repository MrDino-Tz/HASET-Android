package com.haset.hasetapp.utils;

import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.api.CommonStatusCodes;

/** Maps Google Sign-In ApiException status codes to user-facing messages. */
public final class GoogleSignInErrors {
    private GoogleSignInErrors() {}

    /**
     * @return a user-facing message, or null when the failure should stay silent
     *         (e.g. the user backed out of the account chooser).
     */
    public static String message(int statusCode) {
        switch (statusCode) {
            case CommonStatusCodes.DEVELOPER_ERROR:
                return "Google Sign-In configuration error (code 10). "
                        + "Ask the developer to verify the SHA-1 fingerprint in Firebase.";
            case CommonStatusCodes.NETWORK_ERROR:
                return "Network error during Google Sign-In. Please try again.";
            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                return "Google Sign-In failed (code 12500). Please try again.";
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                return null;
            default:
                return "Google Sign-In failed (code " + statusCode + "). Please try again.";
        }
    }
}
