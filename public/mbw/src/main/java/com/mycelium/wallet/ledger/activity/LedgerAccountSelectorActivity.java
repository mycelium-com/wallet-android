package com.mycelium.wallet.ledger.activity;

import android.app.Fragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.activity.HdAccountSelectorActivity;
import com.mycelium.wallet.activity.MasterseedPasswordDialog;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wallet.R;


public abstract class LedgerAccountSelectorActivity extends HdAccountSelectorActivity {

	@Override
	protected AbstractAccountScanManager initMasterseedManager() {
		return MbwManager.getInstance(this).getLedgerManager();
	}

	   @Override
	   protected void onStart() {
	      super.onStart();
	      updateUi();
	   }

	   abstract protected AdapterView.OnItemClickListener accountClickListener();

	   abstract protected void setView();

	   @Override
	   protected void onStop() {
	      super.onStop();
	      masterseedScanManager.setEventHandler(null);
	   }

	   @Override
	   public void finish() {
	      super.finish();
	      masterseedScanManager.stopBackgroundAccountScan();
	   }

	   @Override
	   public void onStatusChanged(AccountScanManager.Status state, AccountScanManager.AccountStatus accountState) {
	      updateUi();
	   }

	   @Override
	   protected void updateUi() {
	      if (masterseedScanManager.currentState != AccountScanManager.Status.initializing) {
	         findViewById(R.id.tvWaitForLedger).setVisibility(View.GONE);
	         findViewById(R.id.ivConnectLedger).setVisibility(View.GONE);
	         txtStatus.setText(getString(R.string.ledger_scanning_status));
	      }else{
	         super.updateUi();
	      }

	      if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.scanning) {
	         findViewById(R.id.llStatus).setVisibility(View.VISIBLE);
	         if (accounts.size()>0) {
	            super.updateUi();
	         }else{
	            txtStatus.setText(getString(R.string.ledger_scanning_status));
	         }

	      }else if (masterseedScanManager.currentAccountState == AccountScanManager.AccountStatus.done) {
	         // DONE
	         findViewById(R.id.llStatus).setVisibility(View.GONE);
	         findViewById(R.id.llSelectAccount).setVisibility(View.VISIBLE);
	         if (accounts.size()==0) {
	            // no accounts found
	            findViewById(R.id.tvNoAccounts).setVisibility(View.VISIBLE);
	            findViewById(R.id.lvAccounts).setVisibility(View.GONE);
	         } else {
	            findViewById(R.id.tvNoAccounts).setVisibility(View.GONE);
	            findViewById(R.id.lvAccounts).setVisibility(View.VISIBLE);
	         }

	         /*
	         // Show the label and version of the connected Trezor
	         findViewById(R.id.llTrezorInfo).setVisibility(View.VISIBLE);
	         TrezorManager trezor = (TrezorManager) masterseedScanManager;
	         ((TextView)findViewById(R.id.tvTrezorName)).setText(trezor.getFeatures().getLabel());

	         String version = String.format("%s, V%d.%d", trezor.getFeatures().getDeviceId(), trezor.getFeatures().getMajorVersion(), trezor.getFeatures().getMinorVersion());
	         ((TextView)findViewById(R.id.tvTrezorSerial)).setText(version);
	         */
	      }

	      accountsAdapter.notifyDataSetChanged();
	   }

	   /*
	   @Override
	   public void onButtonRequest() {   }

	   @Override
	   public String onPinMatrixRequest() {
	      // PIN should never requested here - only for signing.
	      throw new RuntimeException("Unexpected PIN Request");
	   }

	   @Override
	   public void onPassphraseRequest() {
	      MasterseedPasswordDialog pwd = new MasterseedPasswordDialog();
	      pwd.show(getFragmentManager(), PASSPHRASE_FRAGMENT_TAG);
	   }

	   @Override
	   public void setPassphrase(String passphrase){
	      masterseedScanManager.setPassphrase(passphrase);

	      if (passphrase == null){
	         // user choose cancel -> leave this activity
	         finish();
	      } else {
	         // close the dialog fragment
	         Fragment fragPassphrase = getFragmentManager().findFragmentByTag(PASSPHRASE_FRAGMENT_TAG);
	         if (fragPassphrase != null) {
	            getFragmentManager().beginTransaction().remove(fragPassphrase).commit();
	         }
	      }
	   }
	   */

}
