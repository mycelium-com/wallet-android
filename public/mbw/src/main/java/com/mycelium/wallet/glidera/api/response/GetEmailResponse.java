package com.mycelium.wallet.glidera.api.response;

public class GetEmailResponse extends GlideraResponse {
    private String email;
    private boolean userEmailIsSetup;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isUserEmailIsSetup() {
        return userEmailIsSetup;
    }

    public void setUserEmailIsSetup(boolean userEmailIsSetup) {
        this.userEmailIsSetup = userEmailIsSetup;
    }
}
