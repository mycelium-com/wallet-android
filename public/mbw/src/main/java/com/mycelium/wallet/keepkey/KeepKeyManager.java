/*
 * Copyright 2013 Megion Research and Development GmbH
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

package com.mycelium.wallet.keepkey;

import android.content.Context;
import android.util.Log;
import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.*;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider;
import com.keepkey.KeepKey;
import com.keepkey.KeepKeyConnectionException;
import com.keepkey.protobuf.KeepKeyMessage;
import com.keepkey.protobuf.KeepKeyType;
import com.squareup.otto.Bus;

import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class KeepKeyManager extends AbstractAccountScanManager implements ExternalSignatureProvider {
   private static final int MOST_RECENT_VERSION_MAJOR = 1;
   private static final int MOST_RECENT_VERSION_MINOR = 3;
   private static final int MOST_RECENT_VERSION_PATCH = 4;

   protected final int PRIME_DERIVATION_FLAG = 0x80000000;
   private static final String DEFAULT_LABEL = "KeepKey";

   private KeepKey _keepkey = null;
   private KeepKeyMessage.Features features;

   protected final LinkedBlockingQueue<String> pinMatrixEntry = new LinkedBlockingQueue<String>(1);

   public static class OnButtonRequest {
      public OnButtonRequest() {
      }
   }

   public static class OnPinMatrixRequest {
      public OnPinMatrixRequest() {
      }
   }

   public KeepKeyManager(Context context, NetworkParameters network, Bus eventBus) {
      super(context, network, eventBus);
   }

   private KeepKey getKeepKey() {
      if (_keepkey == null) {
         _keepkey = new KeepKey(context);
      }
      return _keepkey;
   }

   public String getLabelOrDefault() {
      if (features != null && !features.getLabel().isEmpty()) {
         return features.getLabel();
      }
      return DEFAULT_LABEL;
   }


   public boolean isMostRecentVersion() {
      if (features != null) {
         if (features.getMajorVersion() < MOST_RECENT_VERSION_MAJOR) {
            return false;
         }
         if (features.getMinorVersion() < MOST_RECENT_VERSION_MINOR) {
            return false;
         }
         if (features.getPatchVersion() < MOST_RECENT_VERSION_PATCH) {
            return false;
         }
         return true;
      } else {
         // we dont know...
         return true;
      }
   }

   @Override
   protected boolean onBeforeScan() {
      return initialize();
   }

   public boolean initialize() {
      // check if a keepkey is attached and connect to it, otherwise loop and check periodically

      // wait until a device is connected
      while (!getKeepKey().isKeepKeyPluggedIn(context)) {
         try {
            setState(Status.unableToScan, currentAccountState);
            Thread.sleep(4000);
         } catch (InterruptedException e) {
            break;
         }
      }

      // set up the connection and afterwards send a Features-Request
      if (getKeepKey().connect(context)) {
         KeepKeyMessage.Initialize req = KeepKeyMessage.Initialize.newBuilder().build();
         Message resp = getKeepKey().send(req);
         if (resp != null && resp instanceof KeepKeyMessage.Features) {
            final KeepKeyMessage.Features f = (KeepKeyMessage.Features) resp;

            // remember the features
            mainThreadHandler.post(new Runnable() {
               @Override
               public void run() {
                  features = f;
               }
            });

            return true;
         } else if (resp == null) {
            Log.e("keepkey", "Got null-response from keepkey");
         } else {
            Log.e("keepkey", "Got wrong response from keepkey " + resp.getClass().toString());
         }
      }
      return false;
   }


   // based on https://github.com/keepkey/python-keepkey/blob/a2a5b6a4601c6912166ef7f85f04fa1101c2afd4/keepkeylib/client.py
   @Override
   public Transaction sign(StandardTransactionBuilder.UnsignedTransaction unsigned, Bip44AccountExternalSignature forAccount) {

      if (!initialize()) {
         return null;
      }

      setState(Status.readyToScan, currentAccountState);

      // send initial signing-request
      KeepKeyMessage.SignTx signTx = KeepKeyMessage.SignTx.newBuilder()
            .setCoinName(getNetwork().getCoinName())
            .setInputsCount(unsigned.getFundingOutputs().length)
            .setOutputsCount(unsigned.getOutputs().length)
            .build();


      Message response;
      try {
         response = getKeepKey().send(signTx);
      } catch (KeepKeyConnectionException ex) {
         postErrorMessage(ex.getMessage());
         return null;
      }

      StandardTransactionBuilder.SigningRequest[] signatureInfo = unsigned.getSignatureInfo();

      ByteWriter signedTx = new ByteWriter(1024);

      while (true) {

         // check for common response and handle them
         try {
            response = filterMessages(response);
         } catch (KeepKeyConnectionException ex) {
            postErrorMessage(ex.getMessage());
            return null;
         }

         if (response == null) {
            // Something went wrong while talking with keepkey - get out of here
            return null;
         }

         if (!(response instanceof KeepKeyMessage.TxRequest)) {
            Log.e("keepkey", "KeepKey: Unexpected Response " + response.getClass().toString());
            return null;
         }

         KeepKeyMessage.TxRequest txRequest = (KeepKeyMessage.TxRequest) response;

         // response had a part of the signed tx - write it to our buffer
         if (txRequest.hasSerialized() && txRequest.getSerialized().hasSerializedTx()) {
            signedTx.putBytes(txRequest.getSerialized().getSerializedTx().toByteArray());
         }

         if (txRequest.getRequestType() == KeepKeyType.RequestType.TXFINISHED) {
            // We are done here...
            break;
         }

         // Device asked for more information, let's process it.
         KeepKeyType.TxRequestDetailsType txRequestDetailsType = txRequest.getDetails();
         Log.d("keepkey", "RequestTyp: " + txRequest.getRequestType().toString());


         Transaction currentTx;
         if (txRequestDetailsType.hasTxHash()) {
            // keepkey requested information about a related tx - get it from the account backing
            Sha256Hash requestHash = Sha256Hash.of(txRequestDetailsType.getTxHash().toByteArray());
            currentTx = TransactionEx.toTransaction(forAccount.getTransaction(requestHash));
         } else {
            // keepkey requested information about the to-be-signed tx
            currentTx = Transaction.fromUnsignedTransaction(unsigned);
         }

         // Lets see, what keepkey wants to know
         if (txRequest.getRequestType() == KeepKeyType.RequestType.TXMETA) {
            // Send transaction metadata

            KeepKeyType.TransactionType txType = KeepKeyType.TransactionType.newBuilder()
                  .setInputsCnt(currentTx.inputs.length)
                  .setOutputsCnt(currentTx.outputs.length)
                  .setVersion(currentTx.version)
                  .setLockTime(currentTx.lockTime)
                  .build();

            KeepKeyMessage.TxAck txAck = KeepKeyMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getKeepKey().send(txAck);

         } else if (txRequest.getRequestType() == KeepKeyType.RequestType.TXINPUT) {
            TransactionInput ak_input = currentTx.inputs[txRequestDetailsType.getRequestIndex()];


            ByteString prevHash = ByteString.copyFrom(ak_input.outPoint.hash.getBytes());
            ByteString scriptSig = ByteString.copyFrom(ak_input.script.getScriptBytes());
            KeepKeyType.TxInputType.Builder txInputBuilder = KeepKeyType.TxInputType.newBuilder()
                  .setPrevHash(prevHash)
                  .setPrevIndex(ak_input.outPoint.index)
                  .setSequence(ak_input.sequence)
                  .setScriptSig(scriptSig);

            // get the bip32 path for the address, so that keepkey knows with what key to sign it
            // only for the unsigned txin
            if (!txRequestDetailsType.hasTxHash()) {
               StandardTransactionBuilder.SigningRequest signingRequest = signatureInfo[txRequestDetailsType.getRequestIndex()];
               Address toSignWith = signingRequest.publicKey.toAddress(getNetwork());

               if (toSignWith != null) {
                  Optional<Integer[]> addId = forAccount.getAddressId(toSignWith);
                  if (addId.isPresent()) {
                     new InputAddressSetter(txInputBuilder).setAddressN(forAccount.getAccountIndex(), addId.get());
                  }
               } else {
                  Log.w("keepkey", "no address found for signing InputIDX " + txRequestDetailsType.getRequestIndex());
               }
            }

            KeepKeyType.TxInputType txInput = txInputBuilder.build();

            KeepKeyType.TransactionType txType = KeepKeyType.TransactionType.newBuilder()
                  .addInputs(txInput)
                  .build();

            KeepKeyMessage.TxAck txAck = KeepKeyMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getKeepKey().send(txAck);

         } else if (txRequest.getRequestType() == KeepKeyType.RequestType.TXOUTPUT) {
            TransactionOutput ak_output = currentTx.outputs[txRequestDetailsType.getRequestIndex()];

            KeepKeyType.TransactionType txType;

            if (txRequestDetailsType.hasTxHash()) {
               // request has an hash -> requests data for an existing output
               ByteString scriptPubKey = ByteString.copyFrom(ak_output.script.getScriptBytes());
               KeepKeyType.TxOutputBinType txOutput = KeepKeyType.TxOutputBinType.newBuilder()
                     .setScriptPubkey(scriptPubKey)
                     .setAmount(ak_output.value)
                     .build();

               txType = KeepKeyType.TransactionType.newBuilder()
                     .addBinOutputs(txOutput)
                     .build();

            } else {
               // request has no hash -> keepkey wants informations about the
               // outputs of the new tx
               Address address = ak_output.script.getAddress(getNetwork());
               KeepKeyType.TxOutputType.Builder txOutput = KeepKeyType.TxOutputType.newBuilder()
                     .setAddress(address.toString())
                     .setAmount(ak_output.value)
                     .setScriptType(mapScriptType(ak_output.script));

               Optional<Integer[]> addId = forAccount.getAddressId(address);
               if (addId.isPresent() && addId.get()[0] == 1) {
                  // If it is one of our internal change addresses, add the HD-PathID
                  // so that keepkey knows, this is the change txout and can calculate the value of the tx correctly
                  new OutputAddressSetter(txOutput).setAddressN(forAccount.getAccountIndex(), addId.get());
               }

               txType = KeepKeyType.TransactionType.newBuilder()
                     .addOutputs(txOutput.build())
                     .build();
            }

            KeepKeyMessage.TxAck txAck = KeepKeyMessage.TxAck.newBuilder()
                  .setTx(txType)
                  .build();

            response = getKeepKey().send(txAck);
         }
      }


      Transaction ret;
      try {
         ret = Transaction.fromByteReader(new ByteReader(signedTx.toBytes()));
      } catch (Transaction.TransactionParsingException e) {
         Log.e("keepkey", "KeepKey TX not valid " + e.getMessage(), e);
         return null;
      }
      return ret;

   }

   private KeepKeyType.OutputScriptType mapScriptType(ScriptOutput script) {
      if (script instanceof ScriptOutputStandard) {
         return KeepKeyType.OutputScriptType.PAYTOADDRESS;
      } else if (script instanceof ScriptOutputP2SH) {
         return KeepKeyType.OutputScriptType.PAYTOSCRIPTHASH;
      } else {
         throw new RuntimeException("unknown script type");
      }
   }

   @Override
   public Optional<HdKeyNode> getAccountPubKeyNode(HdKeyPath keyPath) {
      KeepKeyMessage.GetPublicKey msgGetPubKey = KeepKeyMessage.GetPublicKey.newBuilder()
            .addAllAddressN(keyPath.getAddressN())
            .build();

      try {
         Message resp = filterMessages(getKeepKey().send(msgGetPubKey));
         if (resp != null && resp instanceof KeepKeyMessage.PublicKey) {
            KeepKeyMessage.PublicKey pubKeyNode = (KeepKeyMessage.PublicKey) resp;
            PublicKey pubKey = new PublicKey(pubKeyNode.getNode().getPublicKey().toByteArray());
            HdKeyNode accountRootNode = new HdKeyNode(
                  pubKey,
                  pubKeyNode.getNode().getChainCode().toByteArray(),
                  3, 0,
                  keyPath.getLastIndex()
            );
            return Optional.of(accountRootNode);
         } else {
            return Optional.absent();
         }
      } catch (final KeepKeyConnectionException ex) {
         postErrorMessage(ex.getMessage());
         return Optional.absent();
      }
   }

   @Override
   public UUID createOnTheFlyAccount(HdKeyNode accountRoot, WalletManager walletManager, int accountIndex) {
      UUID account;
      if (walletManager.hasAccount(accountRoot.getUuid())) {
         // Account already exists
         account = accountRoot.getUuid();
      } else {
         account = walletManager.createExternalSignatureAccount(accountRoot, this, accountIndex);
      }
      return account;
   }

   public void enterPin(String pin) {
      pinMatrixEntry.clear();
      pinMatrixEntry.offer(pin);
   }

   private Message filterMessages(final Message msg) {
      if (msg instanceof KeepKeyMessage.ButtonRequest) {
         mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new OnButtonRequest());
            }
         });

         KeepKeyMessage.ButtonAck txButtonAck = KeepKeyMessage.ButtonAck.newBuilder()
               .build();
         return filterMessages(getKeepKey().send(txButtonAck));

      } else if (msg instanceof KeepKeyMessage.PinMatrixRequest) {
         mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
               eventBus.post(new OnPinMatrixRequest());
            }
         });
         String pin;
         try {
            // wait for the user to enter the pin
            pin = pinMatrixEntry.take();
         } catch (InterruptedException e) {
            pin = "";
         }

         KeepKeyMessage.PinMatrixAck txPinAck = KeepKeyMessage.PinMatrixAck.newBuilder()
               .setPin(pin)
               .build();

         // send the Pin Response and (if everything is okay) get the response for the
         // previous requested action
         return filterMessages(getKeepKey().send(txPinAck));
      } else if (msg instanceof KeepKeyMessage.PassphraseRequest) {
         // get the user to enter a passphrase
         Optional<String> passphrase = waitForPassphrase();

         GeneratedMessage response;
         if (!passphrase.isPresent()) {
            // user has not provided a password - reset session on keepkey and cancel
            response = KeepKeyMessage.ClearSession.newBuilder().build();
            getKeepKey().send(response);
            return null;
         } else {
            response = KeepKeyMessage.PassphraseAck.newBuilder()
                  .setPassphrase(passphrase.get())
                  .build();

            // send the Passphrase Response and get the response for the
            // previous requested action
            return filterMessages(getKeepKey().send(response));
         }
      } else if (msg instanceof KeepKeyMessage.Failure) {
         if (postErrorMessage(((KeepKeyMessage.Failure) msg).getMessage())) {
            return null;
         } else {
            throw new RuntimeException("KeepKey error:" + ((KeepKeyMessage.Failure) msg).getCode().toString() + "; " + ((KeepKeyMessage.Failure) msg).getMessage());
         }
      }

      return msg;
   }

   public KeepKeyMessage.Features getFeatures() {
      return features;
   }

   @Override
   public int getBIP44AccountType() {
      return Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY;
   }

   private abstract class AddressSetter {
      public abstract void addAddressN(Integer addressPath);

      public void setAddressN(Integer accountNumber, Integer[] addId) {
         // build the full bip32 path
         Integer[] addressPath = new Integer[]{44 | PRIME_DERIVATION_FLAG, getNetwork().getBip44CoinType().getLastIndex() | PRIME_DERIVATION_FLAG, accountNumber | PRIME_DERIVATION_FLAG, addId[0], addId[1]};
         for (Integer b : addressPath) {
            this.addAddressN(b);
         }
      }
   }

   private class OutputAddressSetter extends AddressSetter {
      final private KeepKeyType.TxOutputType.Builder txOutput;

      private OutputAddressSetter(KeepKeyType.TxOutputType.Builder txOutput) {
         this.txOutput = txOutput;
      }

      @Override
      public void addAddressN(Integer addressPath) {
         txOutput.addAddressN(addressPath);
      }
   }

   private class InputAddressSetter extends AddressSetter {
      final private KeepKeyType.TxInputType.Builder txInput;

      private InputAddressSetter(KeepKeyType.TxInputType.Builder txInput) {
         this.txInput = txInput;
      }

      @Override
      public void addAddressN(Integer addressPath) {
         txInput.addAddressN(addressPath);
      }
   }
}
