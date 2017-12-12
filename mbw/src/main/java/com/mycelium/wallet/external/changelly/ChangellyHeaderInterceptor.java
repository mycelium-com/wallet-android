package com.mycelium.wallet.external.changelly;

import android.support.annotation.NonNull;

import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.crypto.Hmac;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This Interceptor is necessary to comply with changelly's authentication scheme and follows
 * roughly their example implementation in JS:
 * https://github.com/changelly/api-changelly#authentication
 *
 * It wraps the parameters passed in, in a params object and signs the request with the api key secret.
 */
public class ChangellyHeaderInterceptor implements Interceptor {
    private static final String apiKeyData = "8fb168fe8b6b4656867c846be47dccce";
    private static final String apiSecret = "ec97042bcfba5d43f4741dbb3da9861cc59fb7c8d6123333d7823e4c7810d6c0";
    private static final byte[] apiSecretBytes = apiSecret.getBytes(Charset.forName("US-ASCII"));

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        byte[] messageBytes;
        try {
            JSONObject requestBodyJson = new JSONObject()
                    .put("id", "test")
                    .put("jsonrpc", "2.0")
                    .put("method", getMethodFromRequest(request))
                    .put("params", getParamsFromRequest(request));
            messageBytes = requestBodyJson.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        byte[] sha512bytes = Hmac.hmacSha512(apiSecretBytes, messageBytes);
        String signData = HexUtils.toHex(sha512bytes);
        request = request.newBuilder()
                .delete()
                .addHeader("api-key", apiKeyData)
                .addHeader("sign", signData)
                .post(RequestBody.create(MediaType.parse("application/json; charset=UTF-8"), messageBytes))
                .build();
        return chain.proceed(request);
    }

    private String getMethodFromRequest(Request request) {
        List<String> pathSegments = request.url().pathSegments();
        return pathSegments.get(pathSegments.size() - 1);
    }

    @NonNull
    private JSONObject getParamsFromRequest(Request request) throws JSONException {
        JSONObject params = new JSONObject();
        for(String name : request.url().queryParameterNames()) {
            String value = request.url().queryParameter(name);
            params.put(name, value);
        }
        return params;
    }
}
