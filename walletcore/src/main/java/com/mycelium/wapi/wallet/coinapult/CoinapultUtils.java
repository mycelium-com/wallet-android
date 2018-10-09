package com.mycelium.wapi.wallet.coinapult;

import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.nio.ByteBuffer;
import java.util.UUID;

public class CoinapultUtils {
    public static UUID getGuidForAsset(Currency currency, byte[] addressBytes) {
        ByteWriter byteWriter = new ByteWriter(36);
        byteWriter.putBytes(addressBytes);
        byteWriter.putRawStringUtf8(currency.name);
        Sha256Hash accountId = HashUtils.sha256(byteWriter.toBytes());
        return CoinapultUtils.getGuidFromByteArray(accountId.getBytes());
    }

    private static UUID getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}
