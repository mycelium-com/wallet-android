package com.mycelium.net;

import com.squareup.okhttp.OkHttpClient;

import java.net.URI;

public class HttpEndpoint {
    private final String baseUrlString;

    public HttpEndpoint(String baseUrlString) {
        this.baseUrlString = baseUrlString;
    }

    @Override
    public String toString() {
        return getBaseUrl();
    }

    public String getBaseUrl() {
        return baseUrlString;
    }

    public URI getUri(String basePath, String function) {
        return URI.create(this.getBaseUrl() + basePath + '/' + function);
    }

    public URI getUri(String function) {
        return URI.create(this.getBaseUrl() + '/' + function);
    }

    public OkHttpClient getClient() {
        return new OkHttpClient();
    }

}
