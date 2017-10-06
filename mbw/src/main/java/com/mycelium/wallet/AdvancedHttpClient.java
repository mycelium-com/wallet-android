package com.mycelium.wallet;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.security.Security;


public class AdvancedHttpClient {

    private int DEFAULT_CONNECTION_TIMEOUT = 3000;
    private int DEFAULT_READ_TIMEOUT = 3000;

    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;
    private String[] hostsList;
    private HttpRequestFactory requestFactory;

    public AdvancedHttpClient(String[] hostsList) {
        this.hostsList = hostsList;
        initialize();
    }

    public void initialize() {
        Security.addProvider(new BouncyCastleProvider());

        this.requestFactory = new NetHttpTransport()
                .createRequestFactory(new HttpRequestInitializer() {
                    @Override
                    public void initialize(HttpRequest request) {
                        request.setParser(new JsonObjectParser(new JacksonFactory()));
                    }
                });
    }

    public void setFailureRestrictions(HttpRequest request) {
        request.setConnectTimeout(this.connectionTimeout);
        request.setReadTimeout(this.readTimeout);
        request.setUnsuccessfulResponseHandler(new HttpUnsuccessfulResponseHandler() {
            @Override
            public boolean handleResponse(HttpRequest request, HttpResponse response, boolean supportsRetry) throws IOException {
                if (response.getStatusCode() == 500) {
                    throw new BadResponseException();
                }
                return false;
            }
        });
    }


    public <T> T sendPostRequest(Class<T> t, String endpoint,
                                 HttpHeaders headers, Object data) throws IOException {
        for (String host : hostsList) {
            try {
                GenericUrl url = new GenericUrl(host + endpoint);
                T result = makePostRequest(t, url, headers, data);
                return result;
            } catch (Exception ex) {
            }
        }
        throw new IOException("Cannot connect to servers");
    }

    public <T> T sendGetRequest(Class<T> t, String endpoint) throws IOException {
        for (String host : hostsList) {
            try {
                GenericUrl url = new GenericUrl(host + endpoint);
                T result = makeGetRequest(t, url);
                return result;
            } catch (Exception ex) {
            }
        }
        throw new IOException("Cannot connect to servers");
    }

    private <T> T makeGetRequest(Class<T> t, GenericUrl url) throws Exception {
        HttpRequest request = requestFactory.buildGetRequest(url);
        setFailureRestrictions(request);

        HttpResponse response = request.execute();
        return response.parseAs(t);
    }

    private <T> T makePostRequest(Class<T> t, GenericUrl url, HttpHeaders headers,
                                  Object data) throws Exception {

        HttpContent content = new JsonHttpContent(new JacksonFactory(), data);
        HttpRequest request = requestFactory.buildPostRequest(url, content);

        setFailureRestrictions(request);
        if (headers != null) {
            request.setHeaders(headers);
        }

        HttpResponse response = request.execute();
        return response.parseAs(t);
    }

    private class BadResponseException extends RuntimeException {

    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
