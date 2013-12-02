package com.mycelium.wallet.service;

import android.content.Context;

import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mycelium.wallet.R;

public class MrdKeyStretchingTask extends ServiceTask<EncryptionParameters> {
   private static final long serialVersionUID = 1L;

   private KdfParameters _kdfParameters;
   private String _statusMessage;

   public MrdKeyStretchingTask(KdfParameters kdfParameters, Context context) {
      _kdfParameters = kdfParameters;
      _statusMessage = context.getResources().getString(R.string.import_decrypt_stretching);
   }

   @Override
   protected EncryptionParameters doTask(Context context) throws Exception {

      // Generate Encryption parameters by doing key stretching
      try {
         return EncryptionParameters.generate(_kdfParameters);
      } catch (InterruptedException e) {
         return null;
      }
   }

   @Override
   protected void terminate() {
      // Tell scrypt to stop
      _kdfParameters.terminate();
   }

   @Override
   protected ServiceTaskStatus getStatus() {
      return new ServiceTaskStatus(_statusMessage, _kdfParameters.getProgress());
   }

}
