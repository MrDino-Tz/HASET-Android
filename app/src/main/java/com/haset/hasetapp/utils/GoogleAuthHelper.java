package com.haset.hasetapp.utils;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.haset.hasetapp.R;

public final class GoogleAuthHelper {
    private GoogleAuthHelper() {
    }

    public static GoogleSignInClient getClient(Context context) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context.getApplicationContext(), gso);
    }

    public static void signOut(Context context) {
        try {
            getClient(context).signOut();
        } catch (Exception ignored) {
        }
    }

    public static void signOutThen(Context context, Runnable next) {
        try {
            getClient(context).signOut().addOnCompleteListener(task -> next.run());
        } catch (Exception ignored) {
            next.run();
        }
    }
}
