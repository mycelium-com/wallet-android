package com.mycelium.wallet.glidera.api.response;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
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
