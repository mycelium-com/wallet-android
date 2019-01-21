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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.KeyCipher;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

/*
todo HD: root seeds will for now not support signing directly. only support single addresses.

todo HD: instead, there can be two possibilities. select address for signing from transaction, or selecting an address from a huge list. this sucks, so lets discuss it further.

*/
public class MessageSigningActivity extends Activity {


    public static final String PRIVATE_KEY = "privateKey";
    public static final String ADDRESS_TYPE = "addressType";
    private String base64Signature;
    private String messageText;
    private NetworkParameters network;
    public static final String TEMPLATE =
           /**/"-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
           /**/"%s\n" +
           /**/"-----BEGIN BITCOIN SIGNATURE-----\n" +
           /**/"Version: Bitcoin-qt (1.0)\n" +
           /**/"Address: %s\n" +
           /**/"\n" +
           /**/"%s\n" +
           /**/"-----END BITCOIN SIGNATURE-----";

    public static void callMe(Context currentActivity, SingleAddressAccount account, AddressType addressType) {
       InMemoryPrivateKey privateKey;
       try {
          privateKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher());
       } catch (KeyCipher.InvalidKeyCipher e) {
          throw new RuntimeException(e);
       }
       callMe(currentActivity, privateKey, addressType);
    }

   public static void callMe(Context currentActivity, InMemoryPrivateKey key, AddressType addressType) {
      Intent intent = new Intent(currentActivity, MessageSigningActivity.class);
      String privKey = key.getBase58EncodedPrivateKey(MbwManager.getInstance(currentActivity).getNetwork());
      intent.putExtra(PRIVATE_KEY, privKey);
      intent.putExtra(ADDRESS_TYPE, addressType);
      currentActivity.startActivity(intent);
   }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.sign_message);
        String encoded = getIntent().getStringExtra(PRIVATE_KEY);
        final AddressType addressType = (AddressType) getIntent().getSerializableExtra(ADDRESS_TYPE);
        network = MbwManager.getInstance(this).getNetwork();
        final InMemoryPrivateKey privateKey = new InMemoryPrivateKey(encoded, network);

        setContentView(R.layout.message_signing);
        final View signButton = findViewById(R.id.btSign);
        final View copyButton = findViewById(R.id.btCopyToClipboard);
        final View shareButton = findViewById(R.id.btShare);
        final TextView signature = findViewById(R.id.signature);

        final EditText messageToSign = findViewById(R.id.etMessageToSign);
        copyButton.setVisibility(View.GONE);
        shareButton.setVisibility(View.GONE);

        signButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signButton.setEnabled(false);
                messageToSign.setEnabled(false);
                messageToSign.setHint("");
                final ProgressDialog pd = new ProgressDialog(MessageSigningActivity.this);
                pd.setTitle(getString(R.string.signing_inprogress));
                pd.setCancelable(false);
                pd.setIndeterminate(true);
                pd.show();

                new Thread() {
                    @Override
                    public void run() {
                        messageText = messageToSign.getText().toString();
                        SignedMessage signedMessage = privateKey.signMessage(messageText);
                        base64Signature = signedMessage.getBase64Signature();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pd.dismiss();
                                signature.setText(base64Signature);
                                signButton.setVisibility(View.GONE);
                                copyButton.setVisibility(View.VISIBLE);
                                shareButton.setVisibility(View.VISIBLE);
                            }
                        });
                    }
                }.start();
            }
        });

        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utils.setClipboardString(base64Signature, MessageSigningActivity.this);
                new Toaster(MessageSigningActivity.this).toast(R.string.sig_copied, false);
            }
        });
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("text/plain");
                Address address = privateKey.getPublicKey().toAddress(network, addressType);
                String body = String.format(TEMPLATE, messageText, address, base64Signature);

                sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, body);
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getString(R.string.signed_message_subject));
                startActivity(Intent.createChooser(sharingIntent, getString(R.string.signed_message_share)));
            }
        });
    }
}
