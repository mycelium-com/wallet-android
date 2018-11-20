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

package com.mycelium.wallet.content;

import android.content.Intent;
import android.net.Uri;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.mrd.bitlib.crypto.Bip39;
import com.mrd.bitlib.crypto.BipSss;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.wallet.BitcoinUriWithAddress;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.BipSsImportActivity;
import com.mycelium.wallet.activity.HandleUrlActivity;
import com.mycelium.wallet.activity.InstantMasterseedActivity;
import com.mycelium.wallet.activity.StringHandlerActivity;
import com.mycelium.wallet.activity.pop.PopActivity;
import com.mycelium.wallet.activity.send.SendInitializationActivity;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.bitid.BitIDAuthenticationActivity;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.pop.PopRequest;
import com.mycelium.wapi.content.GenericAssetUri;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.bip44.UnrelatedHDAccountConfig;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class StringHandleConfig implements Serializable {
   private static final long serialVersionUID = 0L;

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
               UUID acc = tempWalletManager.createAccounts(new UnrelatedHDAccountConfig(Collections.singletonList(hdKey))).get(0);
               tempWalletManager.setActiveAccount(acc);
               SendInitializationActivity.callMeWithResult(handlerActivity, acc, true,
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
      CHECK_BALANCE {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            Optional<Address> address = getAddress(handlerActivity.getNetwork(), content);
            if (!address.isPresent()) {
               return false;
            }
            UUID account = MbwManager.getInstance(handlerActivity).createOnTheFlyAccount(address.get());
            SendInitializationActivity.callMeWithResult(handlerActivity, account, true,
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
            GenericAddress address = MbwManager.getInstance(handlerActivity).getContentResolver().resovleAddress(content);
            if (address == null) {
               return false;
            }
            handlerActivity.finishOk(address);
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

   public enum BitcoinUriWithAddressAction implements Action {
      SEND {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            GenericAssetUri uri = getUri(handlerActivity, content);
            if (uri != null && uri.getAddress() != null) {
                Intent intent = SendMainActivity.getIntent(handlerActivity, MbwManager.getInstance(handlerActivity).getSelectedAccount().getId(), uri, false);
                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
                handlerActivity.startActivity(intent);
                handlerActivity.finishOk();
                return true;
            } else {
                handlerActivity.finishError(R.string.unrecognized_format);
                //started with bitcoin: but could not be parsed, was handled
                return false;
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
            GenericAssetUri uri = getUri(handlerActivity, content);
            if (uri != null && uri.getAddress() != null) {
                handlerActivity.finishOk(uri);
            } else {
                handlerActivity.finishError(R.string.unrecognized_format);
                //started with bitcoin: but could not be parsed, was handled
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
            GenericAssetUri uri = getUri(handlerActivity, content);
            if (uri != null && uri.getAddress() != null) {
                UUID account = MbwManager.getInstance(handlerActivity).createOnTheFlyAccount(uri.getAddress());
                //we dont know yet where at what to send
               SendInitializationActivity.callMeWithResult(handlerActivity, account, true,
                       StringHandlerActivity.SEND_INITIALIZATION_CODE);

            } else {
                handlerActivity.finishError(R.string.unrecognized_format);
                //started with bitcoin: but could not be parsed, was handled
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      },
      RETURN_ADDRESS {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (!content.toLowerCase(Locale.US).startsWith("bitcoin")) return false;
            GenericAssetUri uri = getUri(handlerActivity, content);
            if (uri != null && uri.getAddress() != null) {
                handlerActivity.finishOk(uri.getAddress());
            } else {
                handlerActivity.finishError(R.string.unrecognized_format);
                //started with bitcoin: but could not be parsed, was handled
            }
            return true;
         }

         @Override
         public boolean canHandle(NetworkParameters network, String content) {
            return isUri(network, content);
         }
      };

      private static GenericAssetUri getUri(StringHandlerActivity handlerActivity, String content) {
         MbwManager manager = MbwManager.getInstance(handlerActivity);
         return manager.getContentResolver().resolveUri(content);
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
      RETURN {
         @Override
         public boolean handle(StringHandlerActivity handlerActivity, String content) {
            if (content.length() % 2 != 0) {
               return false;
            }
            try {
               Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(HexUtils.toBytes(content), false);
               if (masterSeed.isPresent()) {
                  handlerActivity.finishOk(masterSeed.get());
               }
            } catch (RuntimeException ignore) {
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

   public Action privateKeyAction = NONE.INSTANCE;
   public Action bitcoinUriWithAddressAction = NONE.INSTANCE;
   public Action bitcoinUriAction = NONE.INSTANCE;
   public Action addressAction = NONE.INSTANCE;
   public Action bitIdAction = NONE.INSTANCE;
   public Action websiteAction = NONE.INSTANCE;
   public Action masterSeedAction = NONE.INSTANCE; // no coin
   public Action sssShareAction = NONE.INSTANCE;
   public Action hdNodeAction = NONE.INSTANCE;
   public Action wordListAction = NONE.INSTANCE; // no coin
   public Action popAction = NONE.INSTANCE;

   public List<Action> getAllActions() {
      return ImmutableList.of(popAction, privateKeyAction, bitcoinUriWithAddressAction, bitcoinUriAction,
            addressAction, bitIdAction, websiteAction, masterSeedAction, sssShareAction, hdNodeAction, wordListAction);
   }
}
