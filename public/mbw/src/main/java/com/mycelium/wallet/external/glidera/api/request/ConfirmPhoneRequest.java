package com.mycelium.wallet.external.glidera.api.request;

import android.support.annotation.NonNull;

public class ConfirmPhoneRequest {
    private final String newVerificationCode;

    /**
     * @param newVerificationCode Verification code sent to newly added phone number.
     */
    public ConfirmPhoneRequest(@NonNull String newVerificationCode) {
        this.newVerificationCode = newVerificationCode;
    }

    public String getNewVerificationCode() {
        return newVerificationCode;
    }
}
