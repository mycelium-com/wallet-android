package com.mycelium.wallet.external.glidera.api.response;

public class SetPersonalInfoResponse extends GlideraResponse {
    private State basicInfoState;

    public State getBasicInfoState() {
        return basicInfoState;
    }

    public void setBasicInfoState(State basicInfoState) {
        this.basicInfoState = basicInfoState;
    }
}
