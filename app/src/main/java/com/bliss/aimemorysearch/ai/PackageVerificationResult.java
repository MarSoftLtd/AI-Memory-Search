package com.bliss.aimemorysearch.ai;

public final class PackageVerificationResult {

    private final boolean success;
    private final String expectedChecksumSha256;
    private final String actualChecksumSha256;
    private final boolean signatureVerified;
    private final String message;
    private final Throwable error;

    public PackageVerificationResult(
            boolean success,
            String expectedChecksumSha256,
            String actualChecksumSha256,
            boolean signatureVerified,
            String message,
            Throwable error
    ) {
        this.success =
                success;
        this.expectedChecksumSha256 =
                expectedChecksumSha256;
        this.actualChecksumSha256 =
                actualChecksumSha256;
        this.signatureVerified =
                signatureVerified;
        this.message =
                message;
        this.error =
                error;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getExpectedChecksumSha256() {
        return expectedChecksumSha256;
    }

    public String getActualChecksumSha256() {
        return actualChecksumSha256;
    }

    public boolean isSignatureVerified() {
        return signatureVerified;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getError() {
        return error;
    }
}
