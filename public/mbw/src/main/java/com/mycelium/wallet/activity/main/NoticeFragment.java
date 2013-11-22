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

package com.mycelium.wallet.activity.main;

import java.util.List;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.BackupState;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.export.VerifyBackupActivity;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.RecordSetChanged;
import com.mycelium.wallet.event.SelectedRecordChanged;
import com.squareup.otto.Subscribe;

public class NoticeFragment extends Fragment {

   private enum Notice {
      VERIFICATION_MISSING, NONE
   }

   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private View _root;
   private Notice _notice;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_notice_fragment, container, false));
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(false);
      setRetainInstance(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(activity);
      _recordManager = _mbwManager.getRecordManager();
      super.onAttach(activity);
   }

   @Override
   public void onDetach() {
      super.onDetach();
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      _notice = determineNotice();
      _root.findViewById(R.id.btHelp).setOnClickListener(noticeClickListener);
      _root.findViewById(R.id.btWarning).setOnClickListener(noticeClickListener);
      updateUi();
      super.onResume();
   }
   
   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   private OnClickListener noticeClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         switch (_notice) {
         case VERIFICATION_MISSING:
            showVerificationWarning();
            break;
         case NONE:
            openMyceliumHelp();
            break;
         default:
            break;
         }
      }

      private void showVerificationWarning() {
         if (!isAdded()) {
            return;
         }
         VerifyBackupDialog dialog = new VerifyBackupDialog(getActivity());
         dialog.show();
      }

      private void openMyceliumHelp() {
         Intent intent = new Intent(Intent.ACTION_VIEW);
         intent.setData(Uri.parse(Constants.MYCELIUM_WALLET_HELP_URL));
         startActivity(intent);
         Toast.makeText(getActivity(), R.string.going_to_mycelium_com_help, Toast.LENGTH_LONG).show();
      }
   };

   private Notice determineNotice() {
      List<Record> records = _recordManager.getAllRecords();

      // Check for missing backup verifications
      for (Record record : records) {
         if (record.hasPrivateKey() && record.backupState != BackupState.VERIFIED) {
            return Notice.VERIFICATION_MISSING;
         }
      }

      return Notice.NONE;
   }

   private class VerifyBackupDialog extends Dialog {

      public VerifyBackupDialog(final Activity activity) {
         super(activity);
         this.setContentView(R.layout.backup_verification_warning_dialog);
         this.setTitle(R.string.verify_backup_title);

         findViewById(R.id.btBackup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               VerifyBackupDialog.this.dismiss();
               Utils.pinProtectedBackup(activity);
            }

         });

         findViewById(R.id.btVerifyBackup).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
               VerifyBackupDialog.this.dismiss();
               VerifyBackupActivity.callMe(activity);
            }

         });

      }
   }

   private void updateUi() {
      if (!isAdded()) {
         return;
      }
      switch (_notice) {
      case VERIFICATION_MISSING:
         _root.findViewById(R.id.btWarning).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.btHelp).setVisibility(View.GONE);
         break;
      default:
         _root.findViewById(R.id.btWarning).setVisibility(View.GONE);
         // XXX disabled the help button for now
         _root.findViewById(R.id.btHelp).setVisibility(View.GONE);
         break;
      }

   }

   /**
    * Fires when record set changed
    */
   @Subscribe
   public void recordSetChanged(RecordSetChanged event) {
      _notice = determineNotice();
      updateUi();
   }

   /**
    * Fires when the selected record changes
    */
   @Subscribe
   public void selectedRecordChanged(SelectedRecordChanged event) {
      _notice = determineNotice();
      updateUi();
   }
   
   @Subscribe
   public void addressBookChanged(AddressBookChanged event) {
      _notice = determineNotice();
      updateUi();
   }

}
