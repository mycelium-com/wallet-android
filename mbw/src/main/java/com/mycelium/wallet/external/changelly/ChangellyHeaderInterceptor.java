package com.mycelium.wallet.external.changelly;

import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.crypto.Hmac;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

public class ChangellyHeaderInterceptor
implements Interceptor {

    private static final String apiKeyData = "8fb168fe8b6b4656867c846be47dccce";
    private static final String apiSecret = "ec97042bcfba5d43f4741dbb3da9861cc59fb7c8d6123333d7823e4c7810d6c0";
    private static final byte[] apiSecretBytes = apiSecret.getBytes(StandardCharsets.US_ASCII);

    @Override
    public Response intercept(Chain chain)
            throws IOException {
        Request request = chain.request();
        RequestBody requestBody = request.body();
        Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        buffer.toString(); // TODO: sanity check that body is not null and has expected fields
        // byte[] messageBytes = apiSecret.getBytes(StandardCharsets.US_ASCII);
        byte[] messageBytes = buffer.readByteArray(); // .getBytes(StandardCharsets.US_ASCII);
        byte[] sha512bytes = Hmac.hmacSha512(apiSecretBytes, messageBytes);
        String signData = HexUtils.toHex(sha512bytes);
        request = request.newBuilder()
                .addHeader("api-key",apiKeyData)
                .addHeader("sign", signData)
                .build();
        Response response = chain.proceed(request);
        return response;
    }
}
