package com.coinapult.api.httpclient.example;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;

import com.coinapult.api.httpclient.ECC_SC;
import org.spongycastle.jce.provider.BouncyCastleProvider;

public class ECCutilExample {
	public static void main(String[] args) {
      Security.insertProviderAt(new BouncyCastleProvider(),1);

		try {
         ECC_SC ecc = new ECC_SC();
			KeyPair keypair = ecc
					.importFromPEM("-----BEGIN EC PRIVATE KEY-----\n"
                     + "MHQCAQEEILwFOaXcyi0OezaRV+zuV/oQd/ygmBXA8PqboFqKzwq/oAcGBSuBBAAK\n"
                     + "oUQDQgAEgSzId2pbYTgQtBMfx9w4SkD4fDt5Es2VVSzt2MXuYIgTgrJ8k4eAjWKl\n"
                     + "k9BB4csn8R25OOtEwa05bVtOq2qr6g==\n"
                     + "-----END EC PRIVATE KEY-----");
			// KeyPair keypair = ECC.generateKeypair();
			PrivateKey priv = keypair.getPrivate();
			PublicKey pub = keypair.getPublic();

			String privPEM = ecc.exportToPEM(priv);
			System.out.println(privPEM);
			System.out.println(ecc.exportToPEM(pub));

			String somesign = ecc.generateSign("sign this", priv);
			System.out.println(somesign);
			System.out.println(ecc.verifySign(somesign, "sign this", pub));

			// KeyPair kp2 = ECC.importFromPEM(privPEM);
			// System.out.println(ECC.exportToPEM(kp2.getPrivate()));
			// System.out.println(ECC.exportToPEM(kp2.getPublic()));
		} catch (Throwable err) {
			err.printStackTrace();
		}
		System.exit(0);
	}
}
