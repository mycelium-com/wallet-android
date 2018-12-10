/*
 * Copyright 2015 Megion Research and Development GmbH
 * Copyright 2015 Ledger
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.extsig.ledger;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.support.annotation.NonNull;
import android.util.Log;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.utils.BufferUtils;
import com.btchip.utils.Dump;
import com.btchip.utils.KeyUtils;
import com.btchip.utils.SignatureUtils;
import com.google.common.base.Optional;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.SigningRequest;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.crypto.BipDerivationType;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider;
import com.mycelium.wapi.wallet.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.bip44.HDAccountExternalSignature;
import com.squareup.otto.Bus;
import nordpol.android.OnDiscoveredTagListener;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class LedgerManager extends AbstractAccountScanManager implements
      ExternalSignatureProvider, OnDiscoveredTagListener {

   private BTChipTransportFactory transportFactory;
   private BTChipDongle dongle;
   private boolean disableTee;
   private byte[] aid;
   protected final LinkedBlockingQueue<String> pinRequestEntry = new LinkedBlockingQueue<String>(1);
   protected final LinkedBlockingQueue<String> tx2FaEntry = new LinkedBlockingQueue<String>(1);

   private static final String LOG_TAG = "LedgerManager";

   private static final int CONNECT_TIMEOUT = 2000;

   private static final int PAUSE_RESCAN = 4000;
   private static final int SW_PIN_NEEDED = 0x6982;
   private static final int SW_CONDITIONS_NOT_SATISFIED = 0x6985;
   private static final int SW_INVALID_PIN = 0x63C0;
   private static final int SW_HALTED = 0x6faa;

   private static final String NVM_IMAGE = "nvm.bin";

   private static final byte ACTIVATE_ALT_2FA[] = {(byte) 0xE0, (byte) 0x26, (byte) 0x01, (byte) 0x00, (byte) 0x01, (byte) 0x01};

   private static final String DUMMY_PIN = "0000";

   private static final String DEFAULT_UNPLUGGED_AID = "a0000006170054bf6aa94901";

   // EventBus classes
   public static class OnPinRequest {
   }

   public static class OnShowTransactionVerification {
   }

   public static class On2FaRequest {
      public final BTChipDongle.BTChipOutput output;

      public On2FaRequest(BTChipDongle.BTChipOutput output) {
         this.output = output;
      }
   }

   public LedgerManager(Context context, NetworkParameters network, Bus eventBus) {
      super(context, network, eventBus);
      SharedPreferences preferences = this.getContext().getSharedPreferences(Constants.LEDGER_SETTINGS_NAME,
            Activity.MODE_PRIVATE);
      disableTee = preferences.getBoolean(Constants.LEDGER_DISABLE_TEE_SETTING, false);
      aid = Dump.hexToBin(preferences.getString(Constants.LEDGER_UNPLUGGED_AID_SETTING, DEFAULT_UNPLUGGED_AID));
   }


   public void setTransportFactory(BTChipTransportFactory transportFactory) {
      this.transportFactory = transportFactory;
   }

   private boolean isTee() {
      if (!(getTransport().getTransport() instanceof LedgerTransportTEEProxy)) {
         return false;
      }
      return ((LedgerTransportTEEProxy) getTransport().getTransport()).hasTeeImplementation();
   }

   public BTChipTransportFactory getTransport() {
      // Simple demo mode
      // If the device has the Trustlet, and it's not disabled, use it. Otherwise revert to the usual transport
      // For the full integration, bind this to accounts
      if (transportFactory == null) {
         boolean initialized = false;
         if (!disableTee) {
            transportFactory = new LedgerTransportTEEProxyFactory(getContext());
            LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) transportFactory.getTransport();
            byte[] nvm = proxy.loadNVM(NVM_IMAGE);
            if (nvm != null) {
               proxy.setNVM(nvm);
            }
            // Check if the TEE can be connected
            final LinkedBlockingQueue<Boolean> waitConnected = new LinkedBlockingQueue<Boolean>(1);
            boolean result = transportFactory.connect(getContext(), new BTChipTransportFactoryCallback() {

               @Override
               public void onConnected(boolean success) {
                  try {
                     waitConnected.put(success);
                  } catch (InterruptedException ignore) {
                  }
               }

            });
            if (result) {
               try {
                  initialized = waitConnected.poll(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
               } catch (InterruptedException ignore) {
               }
               if (initialized) {
                  initialized = proxy.init();
               }
            }
         }
         if (!initialized) {
            transportFactory = new BTChipTransportAndroid(getContext());
            ((BTChipTransportAndroid) transportFactory).setAID(aid);
         }
         Log.d(LOG_TAG, "Using transport " + transportFactory.getClass());
      }
      return transportFactory;
   }

   @Override
   public Transaction getSignedTransaction(UnsignedTransaction unsigned,
                                           HDAccountExternalSignature forAccount) {
      try {
         return signInternal(unsigned, forAccount);
      } catch (Exception e) {
         postErrorMessage(e.getMessage());
         return null;
      }
   }

   public void enterPin(String pin) {
      pinRequestEntry.clear();
      pinRequestEntry.offer(pin);
   }

   public void enterTransaction2FaPin(String tx2FaPin) {
      tx2FaEntry.clear();
      tx2FaEntry.offer(tx2FaPin);
   }

   private Transaction signInternal(final UnsignedTransaction unsigned, final HDAccountExternalSignature forAccount)
           throws BTChipException, TransactionOutputParsingException {
      if (!initialize()) {
         postErrorMessage("Failed to connect to Ledger device");
         return null;
      }
      setState(Status.readyToScan, getCurrentAccountState());

      if (isTeePinLocked(isTee())) {
         return null;
      }

      ByteWriter rawOutputsWriter = new ByteWriter(1024);
      writeOutputs(unsigned, rawOutputsWriter);
      byte[] rawOutputs = rawOutputsWriter.toBytes();

      String commonPath = "%s" + "'/" + getNetwork().getBip44CoinType() + "'/" + forAccount.getAccountIndex() + "'/";
      String changePath = getChangePath(unsigned, forAccount, commonPath);

      // Legacy firmwares only

      class LegacyParams {
         private String outputAddress;
         private String amount;
         private String fees;

         private LegacyParams(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount) {
            outputAddress = getOutputAddressString(unsigned, forAccount);
            amount = CoinUtil.valueString(calculateTotalSending(unsigned, forAccount), false);
            fees = CoinUtil.valueString(unsigned.calculateFee(), false);
         }
      }

      LegacyParams legacyParams = new LegacyParams(unsigned, forAccount);

      // Prepare for creating inputs
      Transaction unsignedTx = Transaction.fromUnsignedTransaction(unsigned);
      int inputsNumber = unsignedTx.inputs.length;
      BTChipDongle.BTChipInput[] inputs = new BTChipDongle.BTChipInput[inputsNumber];

      String txPin = "";
      BTChipDongle.BTChipOutput outputData = null;
      List<byte[]> signatures = new ArrayList<>(inputsNumber);


      // Fetch trusted inputs
      boolean isSegwit = unsigned.isSegwit();
      for (int i = 0; i < inputsNumber; i++) {
         // In case of SegWit transaction inputs must be created manually
         if (isSegwit) {
            final TransactionInput currentInput = unsignedTx.inputs[i];
            final OutPoint txOutpoint = currentInput.outPoint;
            final byte[] inputHash = txOutpoint.txid.reverse().getBytes();
            final UnspentTransactionOutput prevOut = unsigned.getFundingOutputs()[i];
            final ByteArrayOutputStream inputBuf = new ByteArrayOutputStream();
            inputBuf.write(inputHash, 0, inputHash.length);
            final long index = txOutpoint.index;
            BufferUtils.writeUint32LE(inputBuf, index);
            BufferUtils.writeUint64LE(inputBuf, prevOut.value);

            inputs[i] = dongle.createInput(inputBuf.toByteArray(), currentInput.sequence,false, true);
         } else {
            TransactionInput currentInput = unsignedTx.inputs[i];
            Transaction currentTransaction = TransactionEx.toTransaction(forAccount.getTransaction(currentInput.outPoint.txid));
            ByteArrayInputStream bis = new ByteArrayInputStream(currentTransaction.toBytes());
            inputs[i] = dongle.getTrustedInput(new BitcoinTransaction(bis), currentInput.outPoint.index, currentInput.sequence);
         }
      }


      if (isSegwit) {
         // Sending first input is kind of mark of p2sh/SegWit transaction
         TransactionInput currentInput = unsignedTx.inputs[0];
         if (!tryStartingUntrustedTransaction(inputs, 0, currentInput, isSegwit)) {
            return null;
         }

         // notify the activity to show the transaction details on screen
         getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
               getEventBus().post(new OnShowTransactionVerification());
            }
         });

         ByteWriter byteStream = new ByteWriter(1024);
         byteStream.putCompactInt(unsigned.getOutputs().length);
         for (final TransactionOutput out : unsigned.getOutputs()) {
            out.toByteWriter(byteStream);
         }
         outputData = dongle.finalizeInputFull(byteStream.toBytes(), changePath, false);

         final BTChipDongle.BTChipOutput output = outputData;
         // Check OTP confirmation
         if (outputData.isConfirmationNeeded()) {
            txPin = requestConfirmation(output);

            dongle.startUntrustedTransction(true, 0, inputs, currentInput.getScriptCode());

            byteStream = new ByteWriter(1024);
            byteStream.putCompactInt(unsigned.getOutputs().length);
            for (final TransactionOutput out : unsigned.getOutputs()) {
               out.toByteWriter(byteStream);
            }
            dongle.finalizeInputFull(byteStream.toBytes(), changePath, false);
         }

         for (int i = 0; i < inputsNumber; i++) {
            currentInput = unsignedTx.inputs[i];
            BTChipDongle.BTChipInput[] singleInput = {inputs[i]};
            dongle.startUntrustedTransction(false, 0, singleInput, currentInput.getScriptCode());

            byte[] signature = signOutput(unsigned, forAccount, commonPath, txPin, i);
            // Java Card does not canonicalize, could be enforced per platform
            signatures.add(SignatureUtils.canonicalize(signature, true, 0x01));
         }
      } else {
         for (int i = 0; i < unsignedTx.inputs.length; i++) {
            TransactionInput currentInput = unsignedTx.inputs[i];
            if (!tryStartingUntrustedTransaction(inputs, i, currentInput, isSegwit)) {
               return null;
            }

            // notify the activity to show the transaction details on screen
            getMainThreadHandler().post(new Runnable() {
               @Override
               public void run() {
                  getEventBus().post(new OnShowTransactionVerification());
               }
            });

            outputData = dongle.finalizeInput(rawOutputs, legacyParams.outputAddress, legacyParams.amount,
                    legacyParams.fees, changePath);
            final BTChipDongle.BTChipOutput output = outputData;
            // Check OTP confirmation
            if ((i == 0) && outputData.isConfirmationNeeded()) {
               txPin = requestConfirmation(output);
               dongle.startUntrustedTransction(false, i, inputs, currentInput.script.getScriptBytes());
               dongle.finalizeInput(rawOutputs, legacyParams.outputAddress, legacyParams.amount, legacyParams.fees, changePath);
            }

            byte[] signature = signOutput(unsigned, forAccount, commonPath, txPin, i);
            // Java Card does not canonicalize, could be enforced per platform
            signatures.add(SignatureUtils.canonicalize(signature, true, 0x01));
         }
      }

      // Check if the randomized change output position was swapped compared to the one provided
      // Fully rebuilding the transaction might also be better ...
      // (kept for compatibility with the old API only)
      if ((unsigned.getOutputs().length == 2) && (outputData.getValue() != null) && (outputData.getValue().length != 0)) {
         TransactionOutput firstOutput = unsigned.getOutputs()[0];
         ByteReader byteReader = new ByteReader(outputData.getValue(), 1);
         TransactionOutput dongleOutput = TransactionOutput.fromByteReader(byteReader);
         if ((firstOutput.value != dongleOutput.value) ||
               (!Arrays.equals(firstOutput.script.getScriptBytes(), dongleOutput.script.getScriptBytes()))) {
            unsigned.getOutputs()[0] = unsigned.getOutputs()[1];
            unsigned.getOutputs()[1] = firstOutput;
         }
      }
      // Add signatures
      return StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);
   }

   private byte[] signOutput(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount, String commonPath,
                             String txPin, int outputIndex) throws BTChipException {
      // Sign
      SigningRequest[] signatureInfo = unsigned.getSigningRequests();
      SigningRequest signingRequest = signatureInfo[outputIndex];
      ScriptOutput fundingUtxoScript = unsigned.getFundingOutputs()[outputIndex].script;
      BipDerivationType derivationType = getBipDerivationType(fundingUtxoScript);

      if (derivationType == null) {
         throw new IllegalArgumentException("Can't sign one of the inputs");
      }
      Address toSignWith = signingRequest.getPublicKey().toAddress(getNetwork(), derivationType.getAddressType());
      Optional<Integer[]> addressId = forAccount.getAddressId(toSignWith);
      String keyPath = String.format(commonPath + addressId.get()[0] + "/" + addressId.get()[1],
              ((Byte) derivationType.getPurpose()).toString());
      return dongle.untrustedHashSign(keyPath, txPin);
   }

   private String requestConfirmation(final BTChipDongle.BTChipOutput output) {
      String txPin;
      getMainThreadHandler().post(new Runnable() {
         @Override
         public void run() {
            getEventBus().post(new On2FaRequest(output));
         }
      });
      try {
         // wait for the user to enter the pin
         txPin = tx2FaEntry.take();
      } catch (InterruptedException e1) {
         txPin = "";
      }
      Log.d(LOG_TAG, "Reinitialize transport");
      initialize();
      Log.d(LOG_TAG, "Reinitialize transport done");
      return txPin;
   }

   private boolean tryStartingUntrustedTransaction(BTChipDongle.BTChipInput[] inputs, int i, TransactionInput currentInput,
                                                   boolean isSegwit) throws BTChipException {
      byte[] scriptBytes;
      if (isSegwit) {
         scriptBytes = currentInput.getScriptCode();
      } else {
         scriptBytes = currentInput.getScript().getScriptBytes();
      }
      try {
         dongle.startUntrustedTransction(i == 0, i, inputs, scriptBytes);
      } catch (BTChipException e) {
         // If pin was not entered wait for pin being entered and try again.
         if (e.getSW() == SW_PIN_NEEDED) {
            if (isTee()) {
               //if (dongle.hasScreenSupport()) {
               // PIN request is prompted on screen
               if (!waitForTeePin()){
                  return false;
               }
               dongle.startUntrustedTransction(i == 0, i, inputs, scriptBytes);
            } else {
               String pin = waitForPin();
               try {
                  Log.d(LOG_TAG, "Reinitialize transport");
                  initialize();
                  Log.d(LOG_TAG, "Reinitialize transport done");
                  dongle.verifyPin(pin.getBytes());
                  dongle.startUntrustedTransction(i == 0, i, inputs, scriptBytes);
               } catch (BTChipException e1) {
                  Log.d(LOG_TAG, "2fa error", e1);
                  postErrorMessage("Invalid second factor");
                  return false;
               }
            }
         }
      }
      return true;
   }

   private String waitForPin() {
      getMainThreadHandler().post(new Runnable() {
         @Override
         public void run() {
            getEventBus().post(new OnPinRequest());
         }
      });
      String pin;
      try {
         // wait for the user to enter the pin
         pin = pinRequestEntry.take();
      } catch (InterruptedException e1) {
         pin = "";
      }
      return pin;
   }

   private boolean waitForTeePin() {
      try {
         dongle.verifyPin(DUMMY_PIN.getBytes());
      } catch (BTChipException e1) {
         if ((e1.getSW() & 0xfff0) == SW_INVALID_PIN) {
            postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining");
            return false;
         }
      } finally {
         // Poor man counter
         LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) getTransport().getTransport();
         try {
            byte[] updatedNvm = proxy.requestNVM().get();
            proxy.writeNVM(NVM_IMAGE, updatedNvm);
            proxy.setNVM(updatedNvm);
         } catch (Exception ignore) {
         }
      }
      return true;
   }

   private BipDerivationType getBipDerivationType(ScriptOutput fundingUtxoScript) {
      BipDerivationType derivationType;
      if (fundingUtxoScript instanceof ScriptOutputP2SH) {
         derivationType = BipDerivationType.BIP49;
      } else if (fundingUtxoScript instanceof ScriptOutputP2WPKH) {
         derivationType = BipDerivationType.BIP84;
      } else if (fundingUtxoScript instanceof ScriptOutputP2PKH) {
         derivationType = BipDerivationType.BIP44;
      } else {
         postErrorMessage("Unhandled funding " + fundingUtxoScript);
         return null;
      }
      return derivationType;
   }

   private String getChangePath(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount, String commonPath) {
      String changePath = "";
      for (TransactionOutput o : unsigned.getOutputs()) {
         Address toAddress = o.script.getAddress(getNetwork());
         String purpose = ((Byte) BipDerivationType.Companion.getDerivationTypeByAddress(toAddress).getPurpose()).toString();
         Optional<Integer[]> addressId = forAccount.getAddressId(toAddress);
         if (addressId.isPresent() && addressId.get()[0] == 1) {
            changePath = String.format(String.format("%s%d/%d", commonPath, addressId.get()[0], addressId.get()[1]), purpose);
         }
      }
      return changePath;
   }

   private String getOutputAddressString(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount) {
      String outputAddress = null;
      for (TransactionOutput output : unsigned.getOutputs()) {
         Address toAddress = output.script.getAddress(getNetwork());
         Optional<Integer[]> addressId = forAccount.getAddressId(toAddress);
         if (!(addressId.isPresent() && addressId.get()[0] == 1)) {
            // this output goes to a foreign address (addressId[0]==1 means its internal change)
            outputAddress = toAddress.toString();
         }
      }
      return outputAddress;
   }

   private void writeOutputs(UnsignedTransaction unsigned, ByteWriter rawOutputsWriter) {
      rawOutputsWriter.putCompactInt(unsigned.getOutputs().length);
      for (TransactionOutput output : unsigned.getOutputs()) {
         output.toByteWriter(rawOutputsWriter);
      }
   }

   private long calculateTotalSending(UnsignedTransaction unsigned, HDAccountExternalSignature forAccount) {
      long totalSending = 0;
      for (TransactionOutput output : unsigned.getOutputs()) {
         Address toAddress = output.script.getAddress(getNetwork());
         Optional<Integer[]> addressId = forAccount.getAddressId(toAddress);
         if (!(addressId.isPresent() && addressId.get()[0] == 1)) {
            // this output goes to a foreign address (addressId[0]==1 means its internal change)
            totalSending += output.value;
         }
      }
      return totalSending;
   }

   private boolean isTeePinLocked(boolean isTEE) {
      if (isTEE) {
         final String PIN_IS_TERMINATED = "PIN is terminated";
         // Check that the TEE PIN is not blocked
         try {
            int attempts = dongle.getVerifyPinRemainingAttempts();
            if (attempts == 0) {
               postErrorMessage(PIN_IS_TERMINATED);
               return true;
            }
         } catch (BTChipException e) {
            if (conditionsAreNotSatisfied(e)){
               return true;
            }
            if (e.getSW() == SW_HALTED) {
               LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) getTransport().getTransport();
               try {
                  proxy.close();
                  proxy.init();
                  int attempts = dongle.getVerifyPinRemainingAttempts();
                  if (attempts == 0) {
                     postErrorMessage(PIN_IS_TERMINATED);
                     return true;
                  }
               } catch (BTChipException e1) {
                  if (conditionsAreNotSatisfied(e1)){
                     return true;
                  }
               } catch (Exception ignore) {
               }
            }
         }
      }
      return false;
   }

   private boolean conditionsAreNotSatisfied(BTChipException e) {
      if (e.getSW() == SW_CONDITIONS_NOT_SATISFIED) {
         postErrorMessage("PIN is terminated");
         return true;
      }
      return false;
   }

   @Override
   protected boolean onBeforeScan() {
      boolean initialized = initialize();
      if (!initialized) {
         postErrorMessage("Failed to connect to Ledger device");
         return false;
      }
      // we have found a device with ledger USB ID, but some devices (eg NanoS) have more than one
      // application, the call to getFirmwareVersion will fail, if it isn't in the bitcoin app
      try {
         dongle.getFirmwareVersion();
      } catch (BTChipException e) {
         // this error is expected for ledger unplugged, just continue
         if (e.getSW() != 0x6700) {
            postErrorMessage("Unable to get firmware version - if your ledger supports multiple applications please open the bitcoin app");
            return false;
         }
      }
      return true;
   }

   private boolean initialize() {
      Log.d(LOG_TAG, "Initialize");
      final LinkedBlockingQueue<Boolean> waitConnected = new LinkedBlockingQueue<Boolean>(1);
      while (!getTransport().isPluggedIn()) {
         dongle = null;
         try {
            setState(Status.unableToScan, getCurrentAccountState());
            Thread.sleep(PAUSE_RESCAN);
         } catch (InterruptedException e) {
            break;
         }
      }
      boolean connectResult = getTransport().connect(getContext(), new BTChipTransportFactoryCallback() {
         @Override
         public void onConnected(boolean success) {
            try {
               waitConnected.put(success);
            } catch (InterruptedException ignore) {
            }
         }
      });
      if (!connectResult) {
         return false;
      }
      boolean connected;
      try {
         connected = waitConnected.poll(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
         connected = false;
      }
      if (connected) {
         Log.d(LOG_TAG, "Connected");
         getTransport().getTransport().setDebug(true);
         dongle = new BTChipDongle(getTransport().getTransport());
         dongle.setKeyRecovery(new MyceliumKeyRecovery());
      }
      Log.d(LOG_TAG, "Initialized " + connected);
      return connected;
   }

   public boolean isPluggedIn() {
      return getTransport().isPluggedIn();
   }

   @Override
   public boolean upgradeAccount(@NonNull List<? extends HdKeyNode> accountRoots, @NonNull WalletManager walletManager,
                                 @NonNull UUID uuid) {
      WalletAccount account = walletManager.getAccount(uuid);
      if (account instanceof HDAccountExternalSignature) {
         return walletManager.upgradeExtSigAccount(accountRoots, (HDAccountExternalSignature) account);
      }
      return false;
   }

   @Override
   public UUID createOnTheFlyAccount(List<? extends HdKeyNode> accountRoots,
                                     WalletManager walletManager, int accountIndex) {
      UUID account = null;
      for (HdKeyNode accountRoot : accountRoots) {
         if (walletManager.hasAccount(accountRoot.getUuid())) {
            // Account already exists
            account = accountRoot.getUuid();
         }
      }
      if (account == null) {
         account = walletManager.createExternalSignatureAccount(accountRoots, this, accountIndex);
      }
      return account;
   }

   @Override
   public Optional<HdKeyNode> getAccountPubKeyNode(HdKeyPath keyPath, BipDerivationType derivationType) {
      boolean isTEE = isTee();
      // ledger needs it in the format "/44'/0'/0'" - our default toString format
      // is with leading "m/" -> replace the "m" away
      String keyPathString = keyPath.toString().replace("m/", "");
      if (isTEE) {
         // Verify that the TEE is set up properly - PIN cannot be locked here
         // as this is called directly after the account creation
         try {
            int attempts = dongle.getVerifyPinRemainingAttempts();
         } catch (BTChipException e) {
            if (e.getSW() == SW_HALTED) {
               LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) getTransport().getTransport();
               try {
                  proxy.close();
                  proxy.init();
               } catch (Exception ignore) {
               }
            }
         }
      }
      byte addressByte = getAddressByte(derivationType);
      try {
         BTChipDongle.BTChipPublicKey publicKey;
         try {
            publicKey = dongle.getWalletPublicKey(keyPathString, addressByte);
         } catch (BTChipException e) {
            if (isTEE && (e.getSW() == SW_CONDITIONS_NOT_SATISFIED)) {
               LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) getTransport().getTransport();
               // Not setup ? We can do it on the fly
               dongle.setup(new BTChipDongle.OperationMode[]{BTChipDongle.OperationMode.WALLET},
                     new BTChipDongle.Feature[]{BTChipDongle.Feature.RFC6979}, // TEE doesn't need NO_2FA_P2SH
                     getNetwork().getStandardAddressHeader(),
                     getNetwork().getMultisigAddressHeader(),
                     new byte[4], null,
                     null,
                     null, null);
               try {
                  byte[] updatedNvm = proxy.requestNVM().get();
                  proxy.writeNVM(NVM_IMAGE, updatedNvm);
                  proxy.setNVM(updatedNvm);
               } catch (Exception ignore) {
               }
               try {
                  dongle.verifyPin(DUMMY_PIN.getBytes());
               } catch (BTChipException e1) {
                  if ((e1.getSW() & 0xfff0) == SW_INVALID_PIN) {
                     postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining");
                     return Optional.absent();
                  }
               } finally {
                  try {
                     byte[] updatedNvm = proxy.requestNVM().get();
                     proxy.writeNVM(NVM_IMAGE, updatedNvm);
                     proxy.setNVM(updatedNvm);
                  } catch (Exception ignore) {
                  }
               }
               publicKey = dongle.getWalletPublicKey(keyPathString, addressByte);
            } else if (e.getSW() == SW_PIN_NEEDED) {
               //if (dongle.hasScreenSupport()) {
               if (isTEE) {
                  if (!waitForTeePin()) {
                     return Optional.absent();
                  }
                  publicKey = dongle.getWalletPublicKey(keyPathString, addressByte);
               } else {
                  String pin = waitForPin();
                  try {
                     Log.d(LOG_TAG, "Reinitialize transport");
                     initialize();
                     Log.d(LOG_TAG, "Reinitialize transport done");
                     dongle.verifyPin(pin.getBytes());
                     publicKey = dongle.getWalletPublicKey(keyPathString, addressByte);
                  } catch (BTChipException e1) {
                     if ((e1.getSW() & 0xfff0) == SW_INVALID_PIN) {
                        postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining");
                        return Optional.absent();
                     } else {
                        Log.d(LOG_TAG, "Connect error", e1);
                        postErrorMessage("Error connecting to Ledger device");
                        return Optional.absent();
                     }
                  }
               }
            } else {
               postErrorMessage("Internal error " + e);
               return Optional.absent();
            }
         }
         PublicKey pubKey = new PublicKey(KeyUtils.compressPublicKey(publicKey.getPublicKey()));
         HdKeyNode accountRootNode = new HdKeyNode(pubKey, publicKey.getChainCode(), 3, 0, keyPath.getLastIndex(), derivationType);
         return Optional.of(accountRootNode);
      } catch (Exception e) {
         Log.d(LOG_TAG, "Generic error", e);
         postErrorMessage(e.getMessage());
         return Optional.absent();
      }
   }

   private byte getAddressByte(BipDerivationType derivationType) {
      byte addressByte = 0x00;
      switch (derivationType.getPurpose()) {
         case 44:
            addressByte = 0x00;
            break;
         case 49:
            addressByte = 0x01;
            break;
         case 84:
            addressByte = 0x02;
            break;
      }
      return addressByte;
   }

   @Override
   public int getBIP44AccountType() {
      return HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER;
   }

   public String getLabelOrDefault() {
      return getContext().getString(R.string.ledger);
   }

   public boolean getDisableTEE() {
      return disableTee;
   }

   public void setDisableTEE(boolean disabled) {
      SharedPreferences.Editor editor = getEditor();
      disableTee = disabled;
      editor.putBoolean(Constants.LEDGER_DISABLE_TEE_SETTING, disabled);
      editor.commit();
   }

   public String getUnpluggedAID() {
      return Dump.dump(aid);
   }

   public void setUnpluggedAID(String aid) {
      SharedPreferences.Editor editor = getEditor();
      this.aid = Dump.hexToBin(aid);
      editor.putString(Constants.LEDGER_UNPLUGGED_AID_SETTING, aid);
      editor.commit();
   }

   private SharedPreferences.Editor getEditor() {
      return getContext().getSharedPreferences(Constants.LEDGER_SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
   }

   @Override
   public void tagDiscovered(Tag tag) {
      Log.d(LOG_TAG, "NFC Card detected");
      if (getTransport() instanceof BTChipTransportAndroid) {
         BTChipTransportAndroid transport = (BTChipTransportAndroid) getTransport();
         transport.setDetectedTag(tag);
      }
   }

}
