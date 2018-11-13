/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mycelium.wallet;

import android.content.Intent;
import android.net.Uri;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.BipSss;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wallet.activity.BipSsImportActivity;
import com.mycelium.wallet.activity.HandleUrlActivity;
import com.mycelium.wallet.activity.InstantMasterseedActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.AccountListChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StringHandleConfig implements Serializable {
   private static final long serialVersionUID = 0L;

   public static StringHandleConfig returnKeyOrAddressOrUriOrKeynode() {
      StringHandleConfig request = new StringHandleConfig();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.RETURN;
      request.bitcoinUriAction = BitcoinUriAction.RETURN;
      request.hdNodeAction = HdNodeAction.RETURN;
      request.popAction = PopAction.SEND;
      return request;
   }

   public static StringHandleConfig returnKeyOrAddress() {
      StringHandleConfig request = new StringHandleConfig();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.RETURN_ADDRESS;
      request.sssShareAction = SssShareAction.START_COMBINING;
      return request;
   }

   public static StringHandleConfig returnKeyOrAddressOrHdNode() {
      StringHandleConfig request = new StringHandleConfig();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.hdNodeAction = HdNodeAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.RETURN_ADDRESS;
      request.sssShareAction = SssShareAction.START_COMBINING;
      return request;
   }

   public static StringHandleConfig spendFromColdStorage() {
      StringHandleConfig request = new StringHandleConfig();
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.addressAction = AddressAction.CHECK_BALANCE;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.CHECK_BALANCE;
      request.hdNodeAction = HdNodeAction.COLD_SPENDING;
      request.sssShareAction = SssShareAction.START_COMBINING;
      request.wordListAction = WordListAction.COLD_SPENDING;
      return request;
   }

   public static StringHandleConfig getAddressBookScanRequest() {
      StringHandleConfig request = new StringHandleConfig();
      request.privateKeyAction = PrivateKeyAction.RETURN;
      request.addressAction = AddressAction.RETURN;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.RETURN_ADDRESS;
      return request;
   }

   public static StringHandleConfig genericScanRequest() {
      StringHandleConfig request = new StringHandleConfig();
      request.addressAction = AddressAction.SEND;
      request.bitcoinUriWithAddressAction = BitcoinUriWithAddressAction.SEND;
      request.bitcoinUriAction = BitcoinUriAction.SEND;
      request.bitIdAction = BitIdAction.LOGIN;
      request.privateKeyAction = PrivateKeyAction.COLD_SPENDING;
      request.websiteAction = WebsiteAction.HANDLE_URL;
      request.sssShareAction = SssShareAction.START_COMBINING;
      request.wordListAction = WordListAction.COLD_SPENDING;
      request.hdNodeAction = HdNodeAction.SEND_PUB_SPEND_PRIV;
      request.popAction = PopAction.SEND;

      //at the moment, we just support wordlist backups
      //request.masterSeedAction = MasterSeedAction.IMPORT;
      return request;
   }

   public static StringHandleConfig getShare() {
      StringHandleConfig request = new StringHandleConfig();
      request.sssShareAction = SssShareAction.RETURN_SHARE;
      return request;
   }

   public static StringHandleConfig importMasterSeed() {
      StringHandleConfig request = new StringHandleConfig();
      request.masterSeedAction = MasterSeedAction.IMPORT;
      return request;
   }

   private StringHandleConfig() {
   }

   public static StringHandleConfig verifySeedOrKey() {
      StringHandleConfig request = new StringHandleConfig();
      request.masterSeedAction = MasterSeedAction.VERIFY;
      request.privateKeyAction = PrivateKeyAction.VERIFY;
      return request;
   }

   public interface Action extends Serializable {
      /**
       * @return true if it was handled
       */
      boolean handle(StringHandlerActivity handlerActivity, String content);
      boolean canHandle(NetworkParameters network, String content);

      Action NONE = new Action() {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            return false;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return false;
         }
      };
   }

   public enum PrivateKeyAction implements Action {
      IMPORT {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(handlerActivity.getNetwork(), content);
            if (!key.isPresent()) return false;
            try {
               handlerActivity.getWalletManager().createSingleAddressAccount(key.get(), AesKeyCipher.defaultKeyCipher());
               MbwManager.getEventBus().post(new AccountListChanged());
            } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
               throw new RuntimeException(invalidKeyCipher);
            }
            handlerActivity.finishOk();
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isPrivKey(network, content);
         }
      },

      COLD_SPENDING {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(handlerActivity.getNetwork(), content);
            if (!key.isPresent()) return false;
            UUID account = MbwManager.getInstance(handlerActivity).createOnTheFlyAccount(key.get());
            //we dont know yet where at what to send
            BitcoinUri uri = new BitcoinUri(null,null,null);
            SendInitializationActivity.callMeWithResult(handlerActivity, account,uri, true,
                    StringHandlerActivity.SEND_INITIALIZATION_CODE);
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isPrivKey(network, content);
         }
      },

      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(handlerActivity.getNetwork(), content);
            if (!key.isPresent()) return false;
            handlerActivity.finishOk(key.get());
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isPrivKey(network, content);
         }
      },

      VERIFY {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<InMemoryPrivateKey> key = getPrivateKey(handlerActivity.getNetwork(), content);
            if (!key.isPresent()) return false;

            MbwManager mbwManager = MbwManager.getInstance(handlerActivity);
            // Calculate the account ID that this key would have
            boolean success = false;
            UUID account = null;
            UUID bchAccount = null;
            for (Address address : key.get().getPublicKey().getAllSupportedAddresses(mbwManager.getNetwork()).values()) {
               account = SingleAddressAccount.calculateId(address);
               bchAccount = SingleAddressBCHAccount.Companion.calculateId(key.get().getPublicKey().toAddress(mbwManager.getNetwork(), AddressType.P2PKH));
               // Check whether regular wallet contains the account
               success = mbwManager.getWalletManager(false).hasAccount(account)
                       || mbwManager.getColuManager().hasAccount(account);
               for (ColuAccount.ColuAsset coluAsset : ColuAccount.ColuAsset.getAssetMap().values()) {
                  UUID coluUUID = ColuAccount.getGuidForAsset(coluAsset, key.get().getPublicKey().toAddress(mbwManager.getNetwork(), AddressType.P2PKH).getAllAddressBytes());
                  success |= mbwManager.getColuManager().hasAccount(coluUUID);
               }
            }

            if (success) {
               // Mark key as verified
               mbwManager.getMetadataStorage().setOtherAccountBackupState(account, MetadataStorage.BackupState.VERIFIED);
               mbwManager.getMetadataStorage().setOtherAccountBackupState(bchAccount, MetadataStorage.BackupState.VERIFIED);
               for (ColuAccount.ColuAsset coluAsset : ColuAccount.ColuAsset.getAssetMap().values()) {
                  UUID coluUUID = ColuAccount.getGuidForAsset(coluAsset, key.get().getPublicKey().toAddress(mbwManager.getNetwork(), AddressType.P2PKH).getAllAddressBytes());
                  mbwManager.getMetadataStorage().setOtherAccountBackupState(coluUUID, MetadataStorage.BackupState.VERIFIED);
               }
               handlerActivity.finishOk();
            } else {
               handlerActivity.finishError(R.string.verify_backup_no_such_record);
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isPrivKey(network, content);
         }
      };

      static public Optional<InMemoryPrivateKey> getPrivateKey(NetworkParameters network, String content) {
         Optional<InMemoryPrivateKey> key = InMemoryPrivateKey.fromBase58String(content, network);
         if (key.isPresent()) return key;
         key = InMemoryPrivateKey.fromBase58MiniFormat(content, network);
         if (key.isPresent()) return key;

         //no match
         return Optional.absent();
      }

      static private boolean isPrivKey(NetworkParameters network, String content) {
         return getPrivateKey(network, content).isPresent();
      }
   }

   public enum HdNodeAction implements Action {
      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            try {
               HdKeyNode hdKey = HdKeyNode.parse(content, handlerActivity.getNetwork());
               handlerActivity.finishOk(hdKey);
               return true;
            } catch (HdKeyNode.KeyGenerationException ex){
               return false;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isKeyNode(network, content);
         }
      },
      COLD_SPENDING {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            try {
               HdKeyNode hdKey = HdKeyNode.parse(content, handlerActivity.getNetwork());
               final WalletManager tempWalletManager = MbwManager.getInstance(handlerActivity).getWalletManager(true);
               UUID acc = tempWalletManager.createUnrelatedBip44Account(Collections.singletonList(hdKey));
               tempWalletManager.setActiveAccount(acc);
               BitcoinUri uri = new BitcoinUri(null,null,null);
               SendInitializationActivity.callMeWithResult(handlerActivity, acc, uri, true,
                       StringHandlerActivity.SEND_INITIALIZATION_CODE);
               return true;
            } catch (HdKeyNode.KeyGenerationException ex){
               return false;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isKeyNode(network, content);
         }
      },
      SEND_PUB_SPEND_PRIV {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            try {
               HdKeyNode hdKey = HdKeyNode.parse(content, handlerActivity.getNetwork());
               if (hdKey.isPrivateHdKeyNode()) {
                  //its an xPriv, we want to cold-spend from it
                  return COLD_SPENDING.handle(handlerActivity, content);
               } else {
                  //its xPub, we want to send to it
                  return SEND_TO.handle(handlerActivity, content);
               }
            } catch (HdKeyNode.KeyGenerationException ex) {
               return false;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isKeyNode(network, content);
         }
      },
      SEND_TO {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            try {
               HdKeyNode hdKey = HdKeyNode.parse(content, handlerActivity.getNetwork());
               Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), hdKey);
               intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
               return true;
            } catch (HdKeyNode.KeyGenerationException ex){
               return false;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isKeyNode(network, content);
         }
      };

      public static boolean isKeyNode(NetworkParameters network, String content) {
         try {
            HdKeyNode.parse(content, network);
            return true;
         } catch (HdKeyNode.KeyGenerationException ex){
            return false;
         }
      }
   }

   public enum AddressAction implements Action {
      SEND {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<Address> address = getAddress(handlerActivity.getNetwork(), content);
            if (!address.isPresent()) {
               return false;
            }
            Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), null, address.get(), false);
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            handlerActivity.startActivity(intent);
            handlerActivity.finishOk();
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isAddress(network, content);
         }
      },

      CHECK_BALANCE {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<Address> address = getAddress(handlerActivity.getNetwork(), content);
            if (!address.isPresent()) {
               return false;
            }
            UUID account = MbwManager.getInstance(handlerActivity).createOnTheFlyAccount(address.get());
            //we dont know yet where at what to send
            BitcoinUri uri = new BitcoinUri(null,null,null);
            SendInitializationActivity.callMeWithResult(handlerActivity, account, uri, true,
                    StringHandlerActivity.SEND_INITIALIZATION_CODE);
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isAddress(network, content);
         }
      },

      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<Address> address = getAddress(handlerActivity.getNetwork(), content);
            if (!address.isPresent()) {
               return false;
            }
            handlerActivity.finishOk(address.get());
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isAddress(network, content);
         }
      };

      private static Optional<Address> getAddress(NetworkParameters network, String content) {
         //we really just want to know whether its a raw address, URIs are treated separately
         if (content.startsWith("bitcoin:")) return Optional.absent();
         return Utils.addressFromString(content, network);
      }

      private static boolean isAddress(NetworkParameters network, String content) {
         return getAddress(network, content).isPresent();
      }
   }

   public enum BitcoinUriAction implements Action {
      SEND_COLU_ASSET {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            MbwManager manager = MbwManager.getInstance(handlerActivity);
            ColuAccount coluAccount = (ColuAccount)manager.getSelectedAccount();
            if (coluAccount == null) {
               return false;
            }

            if (!content.toLowerCase(Locale.US).startsWith(coluAccount.getColuAsset().name.toLowerCase()))
               return false;

            Optional<? extends ColuAssetUri> uri = getColuAssetUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with rmc: but could not be parsed, was handled
            } else {
               Intent intent = SendMainActivity.getIntent(handlerActivity, manager.getSelectedAccount().getId(), uri.get(), false);
               intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isColuAssetUri(network, content);
         }
      },
      SEND {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            MbwManager manager = MbwManager.getInstance(handlerActivity);
            Optional<? extends BitcoinUri> uri = getUri(manager.getNetwork(), content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), uri.get(), false);
               intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      },
      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            MbwManager manager = MbwManager.getInstance(handlerActivity);
            Optional<? extends BitcoinUri> uri = getUri(manager.getNetwork(), content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               handlerActivity.finishOk(uri.get());
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      };

      private static Optional<? extends BitcoinUri> getUri(NetworkParameters networkParameters, String content) {
         return BitcoinUri.parse(content, networkParameters);
      }

      private static boolean isUri(NetworkParameters network, String content) {
         return content.toLowerCase().startsWith("bitcoin") && getUri(network, content).isPresent();
      }
   }

   public enum BitcoinUriWithAddressAction implements Action {
      SEND_COLU_ASSET {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            MbwManager manager = MbwManager.getInstance(handlerActivity);
            ColuAccount coluAccount = (ColuAccount)manager.getSelectedAccount();
            if (coluAccount == null)
               return false;

            if (!content.toLowerCase(Locale.US).startsWith(coluAccount.getColuAsset().name.toLowerCase()))
               return false;

            Optional<ColuAssetUriWithAddress> uri = getColuAssetUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with rmc: but could not be parsed, was handled
               return false;
            } else {
               Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), uri.get(), false);
               intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
               return true;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isColuAssetUri(network, content);
         }
      },
      SEND {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            Optional<BitcoinUriWithAddress> uri = getUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
               return false;
            } else {
               Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), uri.get(), false);
               intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
               return true;
            }
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      },

      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            Optional<BitcoinUriWithAddress> uri = getUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               handlerActivity.finishOk(uri.get());
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      },
      CHECK_BALANCE {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            Optional<BitcoinUriWithAddress> uri = getUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               UUID account = MbwManager.getInstance(handlerActivity).createOnTheFlyAccount(uri.get().address);
               //we dont know yet where at what to send
               BitcoinUri targeturi = new BitcoinUri(null,null,null);
               SendInitializationActivity.callMeWithResult(handlerActivity, account, targeturi, true,
                       StringHandlerActivity.SEND_INITIALIZATION_CODE);
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            return isUri(network, content);
         }
      },
      RETURN_ADDRESS {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            Optional<BitcoinUriWithAddress> uri = getUri(handlerActivity, content);
            if (!uri.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitcoin: but could not be parsed, was handled
            } else {
               handlerActivity.finishOk(uri.get().address);
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      };

      private static Optional<BitcoinUriWithAddress> getUri(StringHandlerActivity handlerActivity, String content) {
         MbwManager manager = MbwManager.getInstance(handlerActivity);
         return BitcoinUriWithAddress.parseWithAddress(content, manager.getNetwork());
      }

      private static boolean isUri(NetworkParameters network, String content) {
         return BitcoinUriWithAddress.parseWithAddress(content, network).isPresent();
      }
   }

   private static Optional<ColuAssetUriWithAddress> getColuAssetUri(StringHandlerActivity handlerActivity, String content) {
      MbwManager manager = MbwManager.getInstance(handlerActivity);
      return ColuAssetUriWithAddress.parseWithAddress(content, manager.getNetwork());
   }

   private static boolean isColuAssetUri(NetworkParameters network, String content) {
      return ColuAssetUriWithAddress.parseWithAddress(content, network).isPresent();
   }

   public enum BitIdAction implements Action {
      LOGIN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitid:")) {
               return false;
            }
            Optional<BitIDSignRequest> request = BitIDSignRequest.parse(Uri.parse(content));
            if (!request.isPresent()) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with bitid, but unable to parse, so we handled it.
            } else {
               BitIDAuthenticationActivity.callMe(handlerActivity, request.get());
               handlerActivity.finishOk();
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return content.toLowerCase().startsWith("bitid:");
         }
      }
   }

   public enum WebsiteAction implements Action {
      OPEN_BROWSER {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("http")) {
               return false;
            }
            Uri uri = Uri.parse(content);
            if (null == uri) {
               handlerActivity.finishError(R.string.unrecognized_format);
               //started with http/https, but unable to parse, so we handled it.
            } else {
               Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
               if (browserIntent.resolveActivity(handlerActivity.getPackageManager()) != null) {
                  handlerActivity.startActivity(browserIntent);
                  handlerActivity.finishOk();
               } else {
                  handlerActivity.finishError(R.string.error_no_browser);
               }
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return content.toLowerCase().startsWith("http");
         }
      },

      HANDLE_URL {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("http")) {
               return false;
            }

            final Uri uri = Uri.parse(content);
            if (null == uri) {
               //started with http/https, but unable to parse, so we handled it.
               handlerActivity.finishError(R.string.unrecognized_format);
            } else {
               // open HandleUrlActivity and let it decide what to do with this URL (check if its a payment request)
               Intent intent = HandleUrlActivity.getIntent(handlerActivity, uri);
               handlerActivity.startActivity(intent);
               handlerActivity.finishOk();
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return WebsiteAction.OPEN_BROWSER.canHandle(network, content);
         }
      }
   }



   public enum SssShareAction implements Action {
      START_COMBINING {
         @Override
      public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.startsWith(BipSss.Share.SSS_PREFIX)) {
               return false;
            }
            BipSss.Share share = BipSss.Share.fromString(content);
            if (null == share) {
               handlerActivity.finishError(R.string.error_invalid_sss_share);
            } else {
               BipSsImportActivity.callMe(handlerActivity, share, StringHandlerActivity.IMPORT_SSS_CONTENT_CODE);
               //dont finish, we wait for result
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isShare(content);
         }
      },
      RETURN_SHARE {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.startsWith(BipSss.Share.SSS_PREFIX)) {
               return false;
            }
            BipSss.Share share = BipSss.Share.fromString(content);
            if (null == share) {
               handlerActivity.finishError(R.string.error_invalid_sss_share);
            } else {
               handlerActivity.finishOk(share);
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isShare(content);
         }
      };

      private static boolean isShare(String content) {
         return content.startsWith(BipSss.Share.SSS_PREFIX);
      }
   }

   public enum MasterSeedAction implements Action {
      VERIFY {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            WalletManager walletManager = MbwManager.getInstance(handlerActivity).getWalletManager(false);
            if (!walletManager.hasBip32MasterSeed()) {
               return false;
            }
            if (content.length() % 2 != 0) {
               return false;
            }
            byte[] bytes;
            try {
               bytes = HexUtils.toBytes(content);
            }catch (RuntimeException ex){
               return false;
            }
            Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(bytes, false);
            if (masterSeed.isPresent()) {
               try {
                  Bip39.MasterSeed ourSeed = walletManager.getMasterSeed(AesKeyCipher.defaultKeyCipher());
                  if (masterSeed.get().equals(ourSeed)) {
                     MbwManager.getInstance(handlerActivity).getMetadataStorage().setMasterSeedBackupState(MetadataStorage.BackupState.VERIFIED);
                     handlerActivity.finishOk();
                  } else {
                     handlerActivity.finishError(R.string.wrong_seed);
                  }
                  return true;
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  throw new RuntimeException(invalidKeyCipher);
               }
            }
            return false;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isMasterSeed(content);
         }
      },
      IMPORT {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (content.length() % 2 != 0) {
               return false;
            }
            Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(HexUtils.toBytes(content), false);
            if (masterSeed.isPresent()) {
               UUID acc;
               try {
                  WalletManager walletManager = MbwManager.getInstance(handlerActivity).getWalletManager(false);
                  if (walletManager.hasBip32MasterSeed()) {
                     handlerActivity.finishError(R.string.seed_already_configured);
                     return true;
                  }
                  walletManager.configureBip32MasterSeed(masterSeed.get(), AesKeyCipher.defaultKeyCipher());
                  acc = walletManager.createAdditionalBip44Account(AesKeyCipher.defaultKeyCipher());
                  MbwManager.getInstance(handlerActivity).getMetadataStorage().setMasterSeedBackupState(MetadataStorage.BackupState.VERIFIED);
               } catch (KeyCipher.InvalidKeyCipher invalidKeyCipher) {
                  throw new RuntimeException(invalidKeyCipher);
               }
               handlerActivity.finishOk(acc);
               return true;
            }
            return false;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isMasterSeed(content);
         }
      };

      private static boolean isMasterSeed(String content) {
         try {
            byte[] bytes = HexUtils.toBytes(content);
            return Bip39.MasterSeed.fromBytes(bytes, false).isPresent();
         } catch (RuntimeException ex){
            // HexUtils.toBytes will throw a RuntimeException if the string contains invalid characters
            return false;
         }
      }
   }

   public enum WordListAction implements Action {
      COLD_SPENDING {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            String[] words = content.split(" ");
            if (!Bip39.isValidWordList(words)) {
               return false;
            }
            InstantMasterseedActivity.callMe(handlerActivity, words, null);
            handlerActivity.finishOk();
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            String[] words = content.split(" ");
            return Bip39.isValidWordList(words);
         }
      }
   }

   public enum PopAction implements Action {
      SEND {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!isBtcpopURI(content)) {
               return false;
            }
            PopRequest popRequest;
            try {
               popRequest = new PopRequest(content);
            } catch (IllegalArgumentException e) {
               handlerActivity.finishError(R.string.pop_invalid_pop_uri);
               return false;
            }

            Intent intent = new Intent(handlerActivity, PopActivity.class);
            intent.putExtra("popRequest", popRequest);
            intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            handlerActivity.startActivity(intent);
            handlerActivity.finishOk();
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isBtcpopURI(content);
         }

         private boolean isBtcpopURI(String content) {
            return content.startsWith("btcpop:");
         }
      }
   }

   public Action privateKeyAction = Action.NONE;
   public Action bitcoinUriWithAddressAction = Action.NONE;
   public Action bitcoinUriAction = Action.NONE;
   public Action addressAction = Action.NONE;
   public Action bitIdAction = Action.NONE;
   public Action websiteAction = Action.NONE;
   public Action masterSeedAction = Action.NONE;
   public Action sssShareAction = Action.NONE;
   public Action hdNodeAction = Action.NONE;
   public Action wordListAction = Action.NONE;
   public Action popAction = Action.NONE;

   public List<Action> getAllActions() {
      return ImmutableList.of(popAction, privateKeyAction, bitcoinUriWithAddressAction, bitcoinUriAction,
            addressAction, bitIdAction, websiteAction, masterSeedAction, sssShareAction, hdNodeAction, wordListAction);
   }
}
