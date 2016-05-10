package com.mycelium.wallet.external.glidera.api.response;

import com.google.gson.GsonBuilder;

import java.util.List;

public class GlideraError {
    private Integer code;
    private String message;
    private String details;
    private List<String> invalidParameters;

    public String toString() {
        return new GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(this).toString();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public List<String> getInvalidParameters() {
        return invalidParameters;
    }

    public void setInvalidParameters(List<String> invalidParameters) {
        this.invalidParameters = invalidParameters;
    }
}
