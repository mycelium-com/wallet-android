package com.mycelium.wallet;

import android.content.Intent;
import android.net.Uri;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wallet.activity.ScanActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class ScanRequest implements Serializable {
   private static final long serialVersionUID = 0L;

   public static ScanRequest returnKeyOrAddress() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriAction = BitcoinUriAction.RETURN_ADDRESS;
      return request;
   }

   public static ScanRequest spendFromColdStorage() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.addressAction = AddressAction.CHECK_BALANCE;
      request.bitcoinUriAction = BitcoinUriAction.CHECK_BALANCE;
      return request;
   }

   public static ScanRequest getAddressBookScanRequest() {
      ScanRequest request = new ScanRequest();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriAction = BitcoinUriAction.RETURN_ADDRESS;
      return request;
   }

   public static ScanRequest genericScanRequest() {
      ScanRequest request = new ScanRequest();
      request.addressAction = AddressAction.SEND;
      request.bitcoinUriAction = BitcoinUriAction.SEND;
      request.bitIdAction = BitIdAction.LOGIN;
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.websiteAction = WebsiteAction.OPEN_BROWSER;
      //at the moment, we just support wordlist backups
      //request.masterSeedAction = MasterSeedAction.IMPORT;
      return request;
   }

   public static ScanRequest importMasterSeed() {
      ScanRequest request = new ScanRequest();
      request.masterSeedAction = MasterSeedAction.IMPORT;
      return request;
   }

   private ScanRequest() {
   }

   public static ScanRequest verifySeedOrKey() {
      ScanRequest request = new ScanRequest();
      request.masterSeedAction = MasterSeedAction.VERIFY;
      request.privateKeyAction = PrivateKeyAction.VERIFY;
      return request;
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
            Optional<InMemoryPrivateKey> key = getPrivateKey(scanActivity, content);
            if (!key.isPresent()) return false;
            try {
               scanActivity.getWalletManager().createSingleAddressAccount(key.get(), AesKeyCipher.defaultKeyCipher());
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
               throw new RuntimeException(invalidKeyCipher);
            }
            scanActivity.finishOk();
            return true;
         }
      },

      COLD_SPENDING {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(scanActivity, content);
            if (!key.isPresent()) return false;
            UUID account = MbwManager.getInstance(scanActivity).createOnTheFlyAccount(key.get());
            SendInitializationActivity.callMe(scanActivity, account, null, null, true);
            scanActivity.finishOk();
            return true;
         }
      },

      RETURN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(scanActivity, content);
            if (!key.isPresent()) return false;
            scanActivity.finishOk(key.get());
            return true;
         }
      },

      VERIFY {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(scanActivity, content);
            if (!key.isPresent()) return false;

            MbwManager mbwManager = MbwManager.getInstance(scanActivity);
            // Calculate the account ID that this key would have
            UUID account = SingleAddressAccount.calculateId(key.get().getPublicKey().toAddress(mbwManager.getNetwork()));
            // Check whether regular wallet contains the account
            boolean success = mbwManager.getWalletManager(false).hasAccount(account);
            if (success) {
               scanActivity.finishOk();
            } else {
               scanActivity.finishError(R.string.verify_backup_no_such_record, "");
            }
            return true;
         }
      };

      static private Optional<InMemoryPrivateKey> getPrivateKey(ScanActivity scanActivity, String content) {
         Optional<InMemoryPrivateKey> key = InMemoryPrivateKey.fromBase58String(content, scanActivity.getNetwork());
         if (key.isPresent()) return key;
         key = InMemoryPrivateKey.fromBase58MiniFormat(content, scanActivity.getNetwork());
         if (key.isPresent()) return key;

         //no match
         return Optional.absent();
      }
   }

   public enum AddressAction implements Action {
      SEND {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) {
               return false;
            }
            SendMainActivity.callMe(scanActivity, MbwManager.getInstance(scanActivity).getSelectedAccount().getId(), null, address.get(), false);
            scanActivity.finishOk();
            return true;
         }
      },

      CHECK_BALANCE {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) {
               return false;
            }
            UUID account = MbwManager.getInstance(scanActivity).createOnTheFlyAccount(address.get());
            SendInitializationActivity.callMe(scanActivity, account, null, null, true);
            scanActivity.finishOk();
            return true;
         }
      },

      RETURN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            Optional<Address> address = getAddress(scanActivity, content);
            if (!address.isPresent()) {
               return false;
            }
            scanActivity.finishOk(address.get());
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
               SendMainActivity.callMe(scanActivity, MbwManager.getInstance(scanActivity).getSelectedAccount().getId(), uri.get(), false);
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
      CHECK_BALANCE {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (!content.toLowerCase().startsWith("bitcoin")) return false;
            Optional<BitcoinUri> uri = getUri(scanActivity, content);
            if (!uri.isPresent()) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               UUID account = MbwManager.getInstance(scanActivity).createOnTheFlyAccount(uri.get().address);
               SendInitializationActivity.callMe(scanActivity, account, null, null, true);
               scanActivity.finishOk();
            }
            return true;
         }
      },
      RETURN_ADDRESS {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (!content.toLowerCase().startsWith("bitcoin")) return false;
            Optional<BitcoinUri> uri = getUri(scanActivity, content);
            if (!uri.isPresent()) {
               scanActivity.finishError(R.string.unrecognized_format, content);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               Long amount = uri.get().amount;
               scanActivity.finishOk(uri.get().address, amount);
            }
            return true;
         }
      };

      private static Optional<BitcoinUri> getUri(ScanActivity scanActivity, String content) {
         MbwManager manager = MbwManager.getInstance(scanActivity);
         return BitcoinUri.parse(content, manager.getNetwork());
      }
   }

   public enum BitIdAction implements Action {
      LOGIN {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            //if bitid is disabled, dont to anything
            if (!MbwManager.getInstance(scanActivity).isBitidEnabled()) {
               return false;
            }
            if (!content.toLowerCase().startsWith("bitid:")) {
               return false;
            }
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
            if (!content.toLowerCase().startsWith("http")) {
               return false;
            }
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

   public enum MasterSeedAction implements Action {
      VERIFY {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            WalletManager walletManager = MbwManager.getInstance(scanActivity).getWalletManager(false);
            if (!walletManager.hasBip32MasterSeed()) {
               return false;
            }
            if (content.length() % 2 != 0) {
               return false;
            }
            Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(HexUtils.toBytes(content), false);
            if (masterSeed.isPresent()) {
               try {
                  Bip39.MasterSeed ourSeed = walletManager.getMasterSeed(AesKeyCipher.defaultKeyCipher());
                  if (masterSeed.get().equals(ourSeed)) {
                     MbwManager.getInstance(scanActivity).getMetadataStorage().setMasterKeyBackupState(MetadataStorage.BackupState.VERIFIED);
                     scanActivity.finishOk();
                  } else {
                     scanActivity.finishError(R.string.wrong_seed, "");
                  }
                  return true;
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  throw new RuntimeException(invalidKeyCipher);
               }
            }
            return false;
         }
      },
      IMPORT {
         @Override
         public boolean handle(ScanActivity scanActivity, String content) {
            if (content.length() % 2 != 0) {
               return false;
            }
            Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(HexUtils.toBytes(content), false);
            if (masterSeed.isPresent()) {
               UUID acc;
               try {
                  WalletManager walletManager = MbwManager.getInstance(scanActivity).getWalletManager(false);
                  if (walletManager.hasBip32MasterSeed()) {
                     scanActivity.finishError(R.string.seed_already_configured, "");
                     return true;
                  }
                  walletManager.configureBip32MasterSeed(masterSeed.get(), AesKeyCipher.defaultKeyCipher());
                  acc = walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
                  MbwManager.getInstance(scanActivity).getMetadataStorage().setMasterKeyBackupState(MetadataStorage.BackupState.VERIFIED);
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  throw new RuntimeException(invalidKeyCipher);
               }
               scanActivity.finishOk(acc);
               return true;
            }
            return false;
         }
      }
   }

   public Action privateKeyAction = Action.NONE;
   public Action bitcoinUriAction = Action.NONE;
   public Action addressAction = Action.NONE;
   public Action bitIdAction = Action.NONE;
   public Action websiteAction = Action.NONE;
   public Action masterSeedAction = Action.NONE;

   public List<Action> getAllActions() {
      return ImmutableList.of(privateKeyAction, bitcoinUriAction, addressAction, bitIdAction, websiteAction, masterSeedAction);
   }

}
