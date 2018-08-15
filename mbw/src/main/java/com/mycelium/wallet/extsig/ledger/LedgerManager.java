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
import android.util.Log;
import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.BTChipTransportFactoryCallback;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.utils.Dump;
import com.btchip.utils.KeyUtils;
import com.btchip.utils.SignatureUtils;
import com.google.common.base.Optional;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.SigningRequest;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.UnsignedTransaction;
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
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.btc.bip44.Bip44BtcAccountExternalSignature;
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProvider;
import com.squareup.otto.Bus;
import nordpol.android.OnDiscoveredTagListener;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;
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
      SharedPreferences preferences = this.context.getSharedPreferences(Constants.LEDGER_SETTINGS_NAME,
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
            transportFactory = new LedgerTransportTEEProxyFactory(context);
            LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) transportFactory.getTransport();
            byte[] nvm = proxy.loadNVM(NVM_IMAGE);
            if (nvm != null) {
               proxy.setNVM(nvm);
            }
            // Check if the TEE can be connected
            final LinkedBlockingQueue<Boolean> waitConnected = new LinkedBlockingQueue<Boolean>(1);
            boolean result = transportFactory.connect(context, new BTChipTransportFactoryCallback() {

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
            transportFactory = new BTChipTransportAndroid(context);
            ((BTChipTransportAndroid) transportFactory).setAID(aid);
         }
         Log.d(LOG_TAG, "Using transport " + transportFactory.getClass());
      }
      return transportFactory;
   }

   @Override
   public Transaction getSignedTransaction(UnsignedTransaction unsigned,
                                           Bip44BtcAccountExternalSignature forAccount) {
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

   private Transaction signInternal(UnsignedTransaction unsigned,
                                    Bip44BtcAccountExternalSignature forAccount) throws BTChipException, TransactionOutputParsingException {

      Transaction unsignedtx;
      BTChipDongle.BTChipInput inputs[];
      Vector<byte[]> signatures;
      String outputAddress = null, amount, fees, commonPath, changePath = "";
      long totalSending = 0;
      SigningRequest[] signatureInfo;
      String txpin = "";
      BTChipDongle.BTChipOutput outputData = null;
      ByteWriter rawOutputsWriter = new ByteWriter(1024);
      byte[] rawOutputs;

      if (!initialize()) {
         postErrorMessage("Failed to connect to Ledger device");
         return null;
      }
      boolean isTEE = isTee();

      setState(Status.readyToScan, currentAccountState);

      if (isTEE) {
         // Check that the TEE PIN is not blocked
         try {
            int attempts = dongle.getVerifyPinRemainingAttempts();
            if (attempts == 0) {
               postErrorMessage("PIN is terminated");
               return null;
            }
         } catch (BTChipException e) {
            if (e.getSW() == SW_CONDITIONS_NOT_SATISFIED) {
               postErrorMessage("PIN is terminated");
               return null;
            }
            if (e.getSW() == SW_HALTED) {
               LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy) getTransport().getTransport();
               try {
                  proxy.close();
                  proxy.init();
                  int attempts = dongle.getVerifyPinRemainingAttempts();
                  if (attempts == 0) {
                     postErrorMessage("PIN is terminated");
                     return null;
                  }
               } catch (BTChipException e1) {
                  if (e1.getSW() == SW_CONDITIONS_NOT_SATISFIED) {
                     postErrorMessage("PIN is terminated");
                     return null;
                  }
               } catch (Exception ignore) {
               }
            }
         }
      }

      signatureInfo = unsigned.getSigningRequests();
      unsignedtx = Transaction.fromUnsignedTransaction(unsigned);
      inputs = new BTChipDongle.BTChipInput[unsignedtx.inputs.length];
      signatures = new Vector<byte[]>(unsignedtx.inputs.length);

      rawOutputsWriter.putCompactInt(unsigned.getOutputs().length);
      // Format destination
      commonPath = "44'/" + getNetwork().getBip44CoinType().getLastIndex() + "'/" + forAccount.getAccountIndex() + "'/";
      for (TransactionOutput o : unsigned.getOutputs()) {
         Address toAddress;
         o.toByteWriter(rawOutputsWriter);
         toAddress = o.script.getAddress(getNetwork());
         Optional<Integer[]> addressId = forAccount.getAddressId(toAddress);
         if (!(addressId.isPresent() && addressId.get()[0] == 1)) {
            // this output goes to a foreign address (addressId[0]==1 means its internal change)
            totalSending += o.value;
            outputAddress = toAddress.toString();
         } else {
            changePath = commonPath + addressId.get()[0] + "/" + addressId.get()[1];
         }
      }
      rawOutputs = rawOutputsWriter.toBytes();
      amount = CoinUtil.valueString(totalSending, false);
      fees = CoinUtil.valueString(unsigned.calculateFee(), false);
      // Fetch trusted inputs
      for (int i = 0; i < unsignedtx.inputs.length; i++) {
         TransactionInput currentInput = unsignedtx.inputs[i];
         Transaction currentTransaction = TransactionEx.toTransaction(forAccount.getTransaction(currentInput.outPoint.txid));
         ByteArrayInputStream bis = new ByteArrayInputStream(currentTransaction.toBytes());
         inputs[i] = dongle.getTrustedInput(new BitcoinTransaction(bis), currentInput.outPoint.index);
      }
      // Sign all inputs
      for (int i = 0; i < unsignedtx.inputs.length; i++) {
         TransactionInput currentInput = unsignedtx.inputs[i];
         try {
            dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
         } catch (BTChipException e) {
            if (e.getSW() == SW_PIN_NEEDED) {
               if (isTEE) {
                  //if (dongle.hasScreenSupport()) {
                  // PIN request is prompted on screen
                  try {
                     dongle.verifyPin(DUMMY_PIN.getBytes());
                  } catch (BTChipException e1) {
                     if ((e1.getSW() & 0xfff0) == SW_INVALID_PIN) {
                        postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining");
                        return null;
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
                  dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
               } else {
                  mainThreadHandler.post(new Runnable() {
                     @Override
                     public void run() {
                        eventBus.post(new OnPinRequest());
                     }
                  });
                  String pin;
                  try {
                     // wait for the user to enter the pin
                     pin = pinRequestEntry.take();
                  } catch (InterruptedException e1) {
                     pin = "";
                  }

                  try {
                     Log.d(LOG_TAG, "Reinitialize transport");
                     initialize();
                     Log.d(LOG_TAG, "Reinitialize transport done");
                     dongle.verifyPin(pin.getBytes());
                     dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
                  } catch (BTChipException e1) {
                     Log.d(LOG_TAG, "2fa error", e1);
                     postErrorMessage("Invalid second factor");
                     return null;
                  }
               }
            }
         }

         // notify the activity to show the transaction details on screen
         mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new OnShowTransactionVerification());
            }
         });

         outputData = dongle.finalizeInput(rawOutputs, outputAddress, amount, fees, changePath);
         final BTChipDongle.BTChipOutput output = outputData;
         // Check OTP confirmation
         if ((i == 0) && outputData.isConfirmationNeeded()) {
            mainThreadHandler.post(new Runnable() {
               @Override
               public void run() {
                  eventBus.post(new On2FaRequest(output));
               }
            });
            try {
               // wait for the user to enter the pin
               txpin = tx2FaEntry.take();
            } catch (InterruptedException e1) {
               txpin = "";
            }
            Log.d(LOG_TAG, "Reinitialize transport");
            initialize();
            Log.d(LOG_TAG, "Reinitialize transport done");
            dongle.startUntrustedTransction(false, i, inputs, currentInput.script.getScriptBytes());
            dongle.finalizeInput(rawOutputs, outputAddress, amount, fees, changePath);
         }

         // Sign
         SigningRequest signingRequest = signatureInfo[i];
         Address toSignWith = signingRequest.getPublicKey().toAddress(getNetwork(), AddressType.P2PKH);
         Optional<Integer[]> addressId = forAccount.getAddressId(toSignWith);
         String keyPath = commonPath + addressId.get()[0] + "/" + addressId.get()[1];
         byte[] signature = dongle.untrustedHashSign(keyPath, txpin);
         // Java Card does not canonicalize, could be enforced per platform
         signatures.add(SignatureUtils.canonicalize(signature, true, 0x01));
      }
      // Check if the randomized change output position was swapped compared to the one provided
      // Fully rebuilding the transaction might also be better ...
      // (kept for compatibility with the old API only)
      if ((unsignedtx.outputs.length == 2) && (outputData.getValue() != null) && (outputData.getValue().length != 0)) {
         TransactionOutput firstOutput = unsignedtx.outputs[0];
         ByteReader byteReader = new ByteReader(outputData.getValue(), 1);
         TransactionOutput dongleOutput = TransactionOutput.fromByteReader(byteReader);
         if ((firstOutput.value != dongleOutput.value) ||
               (!Arrays.equals(firstOutput.script.getScriptBytes(), dongleOutput.script.getScriptBytes()))) {
            unsignedtx.outputs[0] = unsignedtx.outputs[1];
            unsignedtx.outputs[1] = firstOutput;
         }
      }
      // Add signatures
      return StandardTransactionBuilder.finalizeTransaction(unsigned, signatures);
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
            setState(Status.unableToScan, currentAccountState);
            Thread.sleep(PAUSE_RESCAN);
         } catch (InterruptedException e) {
            break;
         }
      }
      boolean connectResult = getTransport().connect(context, new BTChipTransportFactoryCallback() {
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
   public UUID createOnTheFlyAccount(HdKeyNode accountRoot,
                                     WalletManager walletManager, int accountIndex) {
      UUID account;
      if (walletManager.hasAccount(accountRoot.getUuid())) {
         // Account already exists
         account = accountRoot.getUuid();
      } else {
         account = walletManager.createExternalSignatureAccount(accountRoot, this, accountIndex);
      }
      return account;
   }

   @Override
   public Optional<HdKeyNode> getAccountPubKeyNode(HdKeyPath keyPath) {
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
      try {
         BTChipDongle.BTChipPublicKey publicKey;
         try {
            publicKey = dongle.getWalletPublicKey(keyPathString);
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
               publicKey = dongle.getWalletPublicKey(keyPathString);
            } else if (e.getSW() == SW_PIN_NEEDED) {
               //if (dongle.hasScreenSupport()) {
               if (isTEE) {
                  try {
                     // PIN request is prompted on screen
                     dongle.verifyPin(DUMMY_PIN.getBytes());
                  } catch (BTChipException e1) {
                     if ((e1.getSW() & 0xfff0) == SW_INVALID_PIN) {
                        postErrorMessage("Invalid PIN - " + (e1.getSW() - SW_INVALID_PIN) + " attempts remaining");
                        return Optional.absent();
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
                  publicKey = dongle.getWalletPublicKey(keyPathString);
               } else {
                  mainThreadHandler.post(new Runnable() {
                     @Override
                     public void run() {
                        eventBus.post(new OnPinRequest());
                     }
                  });
                  String pin;
                  try {
                     // wait for the user to enter the pin
                     pin = pinRequestEntry.take();
                  } catch (InterruptedException e1) {
                     pin = "";
                  }
                  try {
                     Log.d(LOG_TAG, "Reinitialize transport");
                     initialize();
                     Log.d(LOG_TAG, "Reinitialize transport done");
                     dongle.verifyPin(pin.getBytes());
                     publicKey = dongle.getWalletPublicKey(keyPathString);
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
         HdKeyNode accountRootNode = new HdKeyNode(pubKey, publicKey.getChainCode(), 3, 0, keyPath.getLastIndex());
         return Optional.of(accountRootNode);
      } catch (Exception e) {
         Log.d(LOG_TAG, "Generic error", e);
         postErrorMessage(e.getMessage());
         return Optional.absent();
      }
   }

   @Override
   public int getBIP44AccountType() {
      return Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER;
   }

   public String getLabelOrDefault() {
      return context.getString(R.string.ledger);
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
      return context.getSharedPreferences(Constants.LEDGER_SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
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
