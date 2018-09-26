package com.mycelium.wapi.wallet.colu;

import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;

import java.nio.ByteBuffer;
import java.util.UUID;

public class ColuUtils {
    public static UUID getGuidForAsset(ColuMain coluType, byte[] addressBytes) {
        ByteWriter byteWriter = new ByteWriter(36);
        byteWriter.putBytes(addressBytes);
        byteWriter.putRawStringUtf8(coluType.getId());
        Sha256Hash accountId = HashUtils.sha256(byteWriter.toBytes());
        return getGuidFromByteArray(accountId.getBytes());
    }

    private static UUID getGuidFromByteArray(byte[] bytes) {
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}
