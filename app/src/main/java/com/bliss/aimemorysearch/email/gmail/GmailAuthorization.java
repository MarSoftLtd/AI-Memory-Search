package com.bliss.aimemorysearch.email.gmail;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.api.identity.AuthorizationClient;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import java.util.Collections;

public final class GmailAuthorization {
    public static final String READ_ONLY_SCOPE =
            "https://www.googleapis.com/auth/gmail.readonly";

    private GmailAuthorization() {}

    public static AuthorizationClient client(Activity activity) {
        return Identity.getAuthorizationClient(activity);
    }

    public static AuthorizationClient client(Context context) {
        return Identity.getAuthorizationClient(context);
    }

    public static Task<AuthorizationResult> authorize(AuthorizationClient client) {
        AuthorizationRequest request = AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(READ_ONLY_SCOPE)))
                .build();
        return client.authorize(request);
    }
}
