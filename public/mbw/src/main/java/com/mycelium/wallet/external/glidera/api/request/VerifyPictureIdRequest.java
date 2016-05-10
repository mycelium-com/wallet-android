package com.mycelium.wallet.external.glidera.api.request;

import android.support.annotation.NonNull;

public class VerifyPictureIdRequest {
    private final String data;

    /**
     * @param data The data must be in the form data:image/png;base64,<file data>. Replace image/png with the actual content type of the
     *             identity document file. The <file data> is the padded base64 representation of the id document bytes.
     */
    public VerifyPictureIdRequest(@NonNull String data) {
        this.data = data;
    }

    public String getData() {
        return data;
    }
}
