package com.mycelium.wallet;

import android.content.Intent;
import android.net.Uri;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;

import java.io.Serializable;
import java.util.List;

public class ScanRequest implements Serializable {
   private static final long serialVersionUID = 0L;

   public static ScanRequest returnKeyOrAddress() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN_AS_RECORD;
      request.bitcoinUriAction = BitcoinUriAction.RETURN_AS_RECORD;
      return request;
   }

   public static ScanRequest returnPrivateKey() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      return request;
   }

   public static ScanRequest spendFromColdStorage() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.addressAction = AddressAction.CHECK_BALANCE;
      request.bitcoinUriAction = BitcoinUriAction.CHECK_BALANCE;
      return request;
   }

   public static ScanRequest sendCoins() {
      ScanRequest request = new ScanRequest();
      request.addressAction = AddressAction.SEND;
      request.bitcoinUriAction = BitcoinUriAction.SEND;
      return request;
   }

   public static ScanRequest getAddressBookScanRequest() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      return request;
   }

   public static ScanRequest genericScanRequest() {
      ScanRequest request = new ScanRequest();
      request.addressAction = AddressAction.SEND;
      request.bitcoinUriAction = BitcoinUriAction.SEND;
      request.bitIdAction = BitIdAction.LOGIN;
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.websiteAction = WebsiteAction.OPEN_BROWSER;
      return request;
   }

   private ScanRequest() {
   }

   public interface Action extends Serializable {
      /**
      * @return true if it was handled
      */
      boolean handle(ScanActivity scanActivity, String content);

      Action NONE = new Action() {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            return false;
         }
      };
   }

   public enum PrivateKeyAction implements Action {
      IMPORT {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Record> record = getPrivateKey(scanActivity, content);
            if (!record.isPresent()) return false;
            scanActivity.getRecordManager().addRecord(record.get());
            scanActivity.finishOk();
            return true;
         }
      },

      COLD_SPENDING {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Record> record = getPrivateKey(scanActivity, content);
            if (!record.isPresent()) return false;
            SendInitializationActivity.callMe(scanActivity, new Wallet(record.get()), null, null, true);
            scanActivity.finishOk();
            return true;
         }
      },

      RETURN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Record> record = getPrivateKey(scanActivity, content);
            if (!record.isPresent()) return false;
            scanActivity.finishOk(record.get());
            return true;
         }
      };

      static private Optional<Record> getPrivateKey(ScanActivity scanActivity, String content) {
         Optional<Record> record = Record.recordFromBase58Key(content, scanActivity.getNetwork());
         if (record.isPresent()) return record;
         record = Record.recordFromBase58KeyMiniFormat(content, scanActivity.getNetwork());
         if (record.isPresent()) return record;
         return Optional.absent();
      }
   }

   public enum AddressAction implements Action {
      SEND {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) { return false; }
            SendMainActivity.callMe(scanActivity, scanActivity.getWallet(), scanActivity.getWallet().getLocalSpendableOutputs(scanActivity.getBlockChainAddressTracker()), scanActivity.getPrice(), null, address.get(), false);
            scanActivity.finishOk();
            return true;
         }
      },

      CHECK_BALANCE {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) { return false; }
            SendInitializationActivity.callMe(scanActivity, new Wallet(Record.recordFromAddress(address.get())), null, null, true);
            scanActivity.finishOk();
            return true;
         }
      },

      RETURN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) { return false; }
            scanActivity.finishOk(address.get());
            return true;
         }
      },

      RETURN_AS_RECORD {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) { return false; }
            scanActivity.finishOk(Record.recordFromAddress(address.get()));
            return true;
         }
      };

      private static Optional<Address> getAddress(ScanActivity scanActivity, String content) {
         //we really just want to know whether its a raw address, URIs are treated separately
         if (content.startsWith("bitcoin:")) return Optional.absent();
         MbwManager manager = MbwManager.getInstance(scanActivity);
         return Utils.addressFromString(content, manager.getNetwork());
      }
   }

   public enum BitcoinUriAction implements Action {
         SEND {
            @Override
            public boolean handle(ScanActivity scanActivity, String content) {
               if (!content.toLowerCase().startsWith("bitcoin")) return false;
               Optional<BitcoinUri> uri = getUri(scanActivity, content);
               if (!uri.isPresent()) {
                  scanActivity.finishError(R.string.unrecognized_format, content);
                  //started with bitcoin: but could not be parsed, was handled
               } else {
                  SendMainActivity.callMe(scanActivity, scanActivity.getWallet(), scanActivity.getWallet().getLocalSpendableOutputs(scanActivity.getBlockChainAddressTracker()), scanActivity.getPrice(), uri.get(), false);
                  scanActivity.finishOk();
               }
               return true;
            }
         },

         RETURN {
            @Override
            public boolean handle(ScanActivity scanActivity, String content) {
               if (!content.toLowerCase().startsWith("bitcoin")) return false;
               Optional<BitcoinUri> uri = getUri(scanActivity, content);
               if (!uri.isPresent()) {
                  scanActivity.finishError(R.string.unrecognized_format, content);
                  //started with bitcoin: but could not be parsed, was handled
               } else {
                  scanActivity.finishOk(uri.get());
               }
               return true;
            }
         },

      RETURN_AS_RECORD {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (!content.toLowerCase().startsWith("bitcoin")) return false;
            Optional<BitcoinUri> uri = getUri(scanActivity, content);
            if (!uri.isPresent()) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               Record record = Record.recordFromAddress(uri.get().address);
               Long amount = uri.get().amount;
               scanActivity.finishOk(record, amount);
            }
            return true;
         }
      }, CHECK_BALANCE {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (!content.toLowerCase().startsWith("bitcoin")) return false;
            Optional<BitcoinUri> uri = getUri(scanActivity, content);
            if (!uri.isPresent()) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               SendInitializationActivity.callMe(scanActivity, new Wallet(Record.recordFromAddress(uri.get().address)), null, null, true);
               scanActivity.finishOk();
            }
            return true;
         }
      };

         private static Optional<BitcoinUri> getUri(ScanActivity scanActivity,String content) {
            MbwManager manager = MbwManager.getInstance(scanActivity);
            return BitcoinUri.parse(content, manager.getNetwork());
         }
      }

   public enum BitIdAction implements Action {
      LOGIN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            //if bitid is disabled, dont to anything
            if (!MbwManager.getInstance(scanActivity).isBitidEnabled()) { return false; }
            if (!content.toLowerCase().startsWith("bitid:")) { return false; }
            Optional<BitIDSignRequest> request = BitIDSignRequest.parse(Uri.parse(content));
            if (!request.isPresent()) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with bitid, but unable to parse, so we handled it.
            } else {
               BitIDAuthenticationActivity.callMe(scanActivity, request.get());
               scanActivity.finishOk();
            }
            return true;
         }
      }
   }

   public enum WebsiteAction implements Action {
      OPEN_BROWSER {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (!content.toLowerCase().startsWith("http")) { return false; }
            Uri uri = (Uri.parse(content));
            if (null == uri) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with http/https, but unable to parse, so we handled it.
            } else {
               Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
               scanActivity.startActivity(browserIntent);
               scanActivity.finishOk();
            }
            return true;
         }
      }
   }

   public Action privateKeyAction = Action.NONE;
   public Action bitcoinUriAction = Action.NONE;
   public Action addressAction = Action.NONE;
   public Action bitIdAction = Action.NONE;
   public Action websiteAction = Action.NONE;

   public List<Action> getAllActions() {
      return ImmutableList.of(privateKeyAction, bitcoinUriAction, addressAction, bitIdAction, websiteAction);
   }

}
