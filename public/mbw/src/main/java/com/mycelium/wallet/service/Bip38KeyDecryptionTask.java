package com.mycelium.wallet.service;

import android.content.Context;

import com.lambdaworks.crypto.SCryptProgress;
import com.mrd.bitlib.crypto.Bip38;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.R;

public class Bip38KeyDecryptionTask extends ServiceTask<String> {
   private static final long serialVersionUID = 1L;

   private String _bip38PrivateKeyString;
   private String _passphrase;
   private NetworkParameters _network;
   private String _statusMessage;
   private SCryptProgress _progress;

   public Bip38KeyDecryptionTask(String bip38PrivateKeyString, String passphrase, Context context,
         NetworkParameters network) {
      _bip38PrivateKeyString = bip38PrivateKeyString;
      _passphrase = passphrase;
      _network = network;
      _statusMessage = context.getResources().getString(R.string.import_decrypt_stretching);
   }

   @Override
   protected String doTask(Context context) throws Exception {
      _progress = Bip38.getScryptProgressTracker();
      // Do BIP38 decryption
      String result = Bip38.decrypt(_bip38PrivateKeyString, _passphrase, _progress, _network);
      // The result may be null
      return result;
   }

   @Override
   protected void terminate() {
      // Tell scrypt to stop
      _progress.terminate();
   }

   @Override
   protected ServiceTaskStatus getStatus() {
      return new ServiceTaskStatus(_statusMessage, _progress == null ? 0 : _progress.getProgress());
   }

}
