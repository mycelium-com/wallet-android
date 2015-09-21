package com.coinapult.api.httpclient;

import java.security.Key;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
* Created by Andreas on 12.11.2014.
*/
public interface EccUtil {

   String exportToPEM(Key eccPub);

   KeyPair importFromPEM(String privateKeyPay);

   String generateSign(String signdata, PrivateKey eccPriv);

   boolean verifySign(String recvSign, String recvData, PublicKey coinapult_pubkey);
}
