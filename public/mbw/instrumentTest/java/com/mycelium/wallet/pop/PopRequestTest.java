package com.mycelium.wallet.pop;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.*;

public class PopRequestTest {

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNoParams() {
        new PopRequest("btcpop:?");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateOnlyNonce() {
        new PopRequest("btcpop:?nonce=1");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateOnlyP() {
        new PopRequest("btcpop:?p=a");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateEmptyNonce() {
        new PopRequest("btcpop:?p=a&nonce=");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMalformedNonce() {
        new PopRequest("btcpop:?p=a&nonce=b");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateEmptyP() {
        new PopRequest("btcpop:?p=&nonce=1");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMalformedTxidBadChar() {
        new PopRequest("btcpop:?p=a&nonce=1&txid=1234567890123456789012345678901234567890123456789012345678abcdeg");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMalformedTxidTooShort() {
        new PopRequest("btcpop:?p=a&nonce=1&txid=1234567890123456789012345678901234567890123456789012345678abcde");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMalformedTxidTooLong() {
        new PopRequest("btcpop:?p=a&nonce=1&txid=1234567890123456789012345678901234567890123456789012345678abcdef1");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateMalformedAmount() {
        new PopRequest("btcpop:?p=a&nonce=1&amount=a");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateNegativeAmount() {
        new PopRequest("btcpop:?p=a&nonce=1&amount=-1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateBadCommaInAmount() {
        new PopRequest("btcpop:?p=a&nonce=1&amount=1,1");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateTooSmallFraction() {
        new PopRequest("btcpop:?p=a&nonce=1&amount=1,000000009");
    }
    @Test(expected = IllegalArgumentException.class)
    public void testCreateTooMuchBitcoin() {
        new PopRequest("btcpop:?p=a&nonce=1&amount=" + 21000000.00000001);
    }
    public void testCreateMaxBitcoin() {
        PopRequest uri = new PopRequest("btcpop:?p=a&nonce=1&amount=" + 21000000.00000000);
        assertEquals(2100000000000000L, uri.getAmountSatoshis().longValue());
    }

    @Test
    public void testCreateMinimal() {
        PopRequest uri = new PopRequest("btcpop:?nonce=111&p=a");
        assertEquals(111, uri.getNonce().longValue());
        assertNull(uri.getAmountSatoshis());
        assertNull(uri.getText());
        assertNull(uri.getTxid());
    }

    @Test
    public void testCreateFull() {
        String txid="1234567890123456789012345678901234567890123456789012345678abcdef";
        PopRequest uri = new PopRequest("btcpop:?nonce=111&p=a&text=atext&amount=10&txid=" + txid);
        assertEquals(111, uri.getNonce().longValue());
        assertEquals(1000000000, uri.getAmountSatoshis().longValue());
        assertEquals("atext", uri.getText());
        assertEquals(txid, uri.getTxid().toString());
    }

    @Test
    public void testCreateUrlDecode() throws UnsupportedEncodingException {
        String encoded = URLEncoder.encode("http://a.example.com/Å/ä/ö", "UTF-8");
        PopRequest uri = new PopRequest("btcpop:?text=a+text&nonce=111&p=" + encoded);

        assertEquals("a text", uri.getText());
        assertEquals("http://a.example.com/Å/ä/ö", uri.getUrl());
    }

}