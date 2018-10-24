package com.mycelium.wapi.wallet.colu;

import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ColuUtils {
    public static UUID getGuidForAsset(CryptoCurrency coluType, byte[] addressBytes) {
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

    public static ColuMain getColuCoin(String coinId) {
        ColuMain coinType = null;
        if (MTCoin.INSTANCE.getId().equals(coinId)) {
            coinType = MTCoin.INSTANCE;
        } else if (MASSCoin.INSTANCE.getId().equals(coinId)) {
            coinType = MASSCoin.INSTANCE;
        } else if (RMCCoin.INSTANCE.getId().equals(coinId)) {
            coinType = RMCCoin.INSTANCE;
        }
        return coinType;
    }

    public static List<ColuMain> allColuCoins() {
        return Arrays.asList(MTCoin.INSTANCE, MASSCoin.INSTANCE, RMCCoin.INSTANCE);
    }
}
