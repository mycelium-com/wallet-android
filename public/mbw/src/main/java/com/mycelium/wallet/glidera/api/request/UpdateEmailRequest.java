package com.mycelium.wallet.glidera.api.request;

import android.support.annotation.NonNull;

public class UpdateEmailRequest {
    private final String email;

    /**
     * @param email User's new email address
     */
    public UpdateEmailRequest(@NonNull String email) {
        this.email = email;
    }


    public String getEmail() {
        return email;
    }

}
