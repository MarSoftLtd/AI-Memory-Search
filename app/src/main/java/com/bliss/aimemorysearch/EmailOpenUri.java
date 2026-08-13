package com.bliss.aimemorysearch;

import android.net.Uri;

import com.bliss.aimemorysearch.db.EmailEntity;

import java.math.BigInteger;

/** Provider-aware deep-link/web fallback built only from persisted provenance. */
public final class EmailOpenUri {
    private EmailOpenUri() {}

    public static Uri from(EmailEntity email) {
        if (email == null) return null;
        String provider = email.provider == null ? "" : email.provider.trim();
        String id = email.threadId == null || email.threadId.trim().isEmpty()
                ? email.messageId : email.threadId;
        if (id == null || id.trim().isEmpty()) return null;
        if ("gmail".equalsIgnoreCase(provider)) {
            String account = email.account == null ? "0" : email.account;
            return Uri.parse("https://mail.google.com/mail/u/"
                    + Uri.encode(account) + "/#all/" + Uri.encode(id));
        }
        if ("microsoft".equalsIgnoreCase(provider)
                || "outlook".equalsIgnoreCase(provider)) {
            return Uri.parse("https://outlook.office.com/mail/deeplink/read/"
                    + Uri.encode(id));
        }
        return null;
    }

    public static Uri gmailConversation(EmailEntity email) {
        if (email == null) return Uri.EMPTY;
        String id = email.threadId == null || email.threadId.trim().isEmpty()
                ? email.messageId : email.threadId;
        String account = email.account == null ? "" : email.account.trim();
        return Uri.parse("content://gmail-ls/conversations/"
                + Uri.encode(account) + "/" + Uri.encode(gmailConversationId(id)));
    }

    static String gmailConversationId(String apiId) {
        if (apiId == null || apiId.trim().isEmpty()) return "";
        String value = apiId.trim();
        try {
            return new BigInteger(value, 16).toString(10);
        } catch (NumberFormatException ignored) {
            return value;
        }
    }
}
