package com.mycelium.wallet.ledger;

import java.math.BigInteger;

import com.btchip.BTChipKeyRecovery;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.crypto.Signature;
import com.mrd.bitlib.crypto.Signatures;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.Sha256Hash;

public class MyceliumKeyRecovery implements BTChipKeyRecovery {

	@Override
	public byte[] recoverKey(int recId, byte[] signatureParam, byte[] hashValue) {
	      Signature signature = Signatures.decodeSignatureParameters(new ByteReader(signatureParam));
	      Sha256Hash hash = new Sha256Hash(hashValue);
	      PublicKey key = SignedMessage.recoverFromSignature(recId, signature, hash, false);
	      if (key != null) {
	    	  return key.getPublicKeyBytes();
	      }
	      else {
	    	  return null;
	      }
	}
	
}
