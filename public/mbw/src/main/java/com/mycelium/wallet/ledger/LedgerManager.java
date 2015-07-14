package com.mycelium.wallet.ledger;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.BTChipTransportFactory;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.utils.KeyUtils;
import com.google.common.base.Optional;
import com.ledger.tbase.comm.LedgerTransportTEEProxy;
import com.ledger.tbase.comm.LedgerTransportTEEProxyFactory;
import com.mrd.bitlib.StandardTransactionBuilder;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.model.TransactionInput;
import com.mrd.bitlib.model.TransactionOutput;
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.CoinUtil;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.activity.util.AbstractAccountScanManager;
import com.mycelium.wallet.trezor.TrezorManager.Events;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.wallet.AccountScanManager;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.AccountScanManager.Status;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider;

public class LedgerManager extends AbstractAccountScanManager implements
		ExternalSignatureProvider {
	
	private BTChipTransportFactory transportFactory;
	private BTChipDongle dongle;
	protected Events handler=null;
	private boolean disableTee;
	
	private static final int PAUSE_RESCAN = 4000;
	private static final int SW_PIN_NEEDED = 0x6982;
	private static final int SW_CONDITIONS_NOT_SATISFIED = 0x6985;
	
	private static final byte ACTIVATE_ALT_2FA[] = { (byte)0xE0, (byte)0x26, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x01 };
	
	private static final String DUMMY_PIN = "0000";
	
	public interface Events extends AccountScanManager.Events {
		public String onPinRequest();
		public String onUserConfirmationRequest(BTChipDongle.BTChipOutput output);
	}
		
	public LedgerManager(Context context, NetworkParameters network){
		super(context, network);
		SharedPreferences preferences = _context.getSharedPreferences(Constants.LEDGER_SETTINGS_NAME,
	              Activity.MODE_PRIVATE);
		disableTee = preferences.getBoolean(Constants.LEDGER_DISABLE_TEE_SETTING, false);
	}
	
	@Override
	public void setEventHandler(AccountScanManager.Events handler){
		if (handler instanceof Events) {
			this.handler = (Events) handler;
		}
		// pass handler down to superclass, and also use own handler, because we have some
		// additional events
		super.setEventHandler(handler);
	}
	
	public void setTransportFactory(BTChipTransportFactory transportFactory) {
		this.transportFactory = transportFactory;
	}
	
	public BTChipTransportFactory getTransport() {
		// Simple demo mode 
		// If the device has the Trustlet, and it's not disabled, use it. Otherwise revert to the usual transport
		// For the full integration, bind this to accounts
		if (transportFactory == null) {
			boolean initialized = false;
			if (!disableTee) {
				transportFactory = new LedgerTransportTEEProxyFactory(_context);
				LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy)transportFactory.getTransport();
				byte[] nvm = proxy.loadNVM("nvm.bin");
				if (nvm != null) {
					proxy.setNVM(nvm);
				}
				initialized = proxy.init();
			}
			if (!initialized) {
				transportFactory = new BTChipTransportAndroid(_context);
			}
			Log.d("LedgerManager", "Using transport " + transportFactory.getClass());
		}
		return transportFactory;
	}
	
	@Override
	public Transaction sign(UnsignedTransaction unsigned,
			Bip44AccountExternalSignature forAccount) {
		try {
			return signInternal(unsigned, forAccount);
		}
		catch(Exception e) {
			postErrorMessage(e.getMessage());
			return null;			
		}
	}
		
	private Transaction signInternal(UnsignedTransaction unsigned,
			Bip44AccountExternalSignature forAccount) throws BTChipException, TransactionOutputParsingException {

		Transaction unsignedtx;
		BTChipDongle.BTChipInput inputs[];
		Vector<byte[]> signatures;
		String outputAddress = null, amount, fees, commonPath, changePath = "";
        long totalSending = 0;
        StandardTransactionBuilder.SigningRequest[] signatureInfo;
        String txpin = "";
        BTChipDongle.BTChipOutput outputData = null;
		
		if (!initialize()) {
			return null;
		}
		
		setState(Status.readyToScan, currentAccountState);
		
		signatureInfo = unsigned.getSignatureInfo();
		unsignedtx = Transaction.fromUnsignedTransaction(unsigned);
		inputs = new BTChipDongle.BTChipInput[unsignedtx.inputs.length];
		signatures = new Vector<byte[]>(unsignedtx.inputs.length);
		
		// Format destination
		commonPath = "44'/" + getNetwork().getBip44CoinType().getLastIndex() + "'/" + forAccount.getAccountIndex() + "'/"; 
        for (TransactionOutput o : unsigned.getOutputs()){
           Address toAddress;
           toAddress = o.script.getAddress(getNetwork());
           Optional<Integer[]> addressId = forAccount.getAddressId(toAddress);
           if (! (addressId.isPresent() && addressId.get()[0]==1) ){
              // this output goes to a foreign address (addressId[0]==1 means its internal change)
              totalSending += o.value;
              outputAddress = toAddress.toString();
           }
           else {
        	   changePath = commonPath + addressId.get()[0] + "/" + addressId.get()[1];  
           }
        }
        amount = CoinUtil.valueString(totalSending, false);
        fees = CoinUtil.valueString(unsigned.calculateFee(), false);
		// Fetch trusted inputs
		for (int i=0; i<unsignedtx.inputs.length; i++) {
			TransactionInput currentInput = unsignedtx.inputs[i];
			Transaction currentTransaction = TransactionEx.toTransaction(forAccount.getTransaction(currentInput.outPoint.hash));
			ByteArrayInputStream bis = new ByteArrayInputStream(currentTransaction.toBytes());
			inputs[i] = dongle.getTrustedInput(new BitcoinTransaction(bis), currentInput.outPoint.index);
		}
		// Sign all inputs
		for (int i=0; i<unsignedtx.inputs.length; i++) {			
			TransactionInput currentInput = unsignedtx.inputs[i];
			try {
				dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
			}
			catch(BTChipException e) {
				if (e.getSW() == SW_PIN_NEEDED) {
					boolean isTEE = getTransport().getTransport() instanceof LedgerTransportTEEProxy;
					if (isTEE) {
					//if (dongle.hasScreenSupport()) {
						// PIN request is prompted on screen
						dongle.verifyPin(DUMMY_PIN.getBytes());
						if (getTransport().getTransport() instanceof LedgerTransportTEEProxy) {
							// Poor man counter
							LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy)getTransport().getTransport();
							try {
								proxy.writeNVM("nvm.bin", proxy.requestNVM().get());
							}
							catch(Exception e1) {								
							}
						}
						dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
					}
					else
					if (handler != null) {
						String pin = handler.onPinRequest();
						dongle.verifyPin(pin.getBytes());
						dongle.startUntrustedTransction(i == 0, i, inputs, currentInput.script.getScriptBytes());
					}
					else {
						throw e;
					}
				}
			}
			outputData = dongle.finalizeInput(outputAddress, amount, fees, changePath);
			// Check OTP confirmation
			if ((i == 0) && (handler != null) && outputData.isConfirmationNeeded()) {
				txpin = handler.onUserConfirmationRequest(outputData);
				initialize();
				dongle.startUntrustedTransction(false, i, inputs, currentInput.script.getScriptBytes());
				dongle.finalizeInput(outputAddress, amount, fees, changePath);
			}
			// Sign
			StandardTransactionBuilder.SigningRequest signingRequest = signatureInfo[i];
			Address toSignWith = signingRequest.publicKey.toAddress(getNetwork());
			Optional<Integer[]> addressId = forAccount.getAddressId(toSignWith);
			String keyPath = commonPath + addressId.get()[0] + "/" + addressId.get()[1];
			signatures.add(dongle.untrustedHashSign(keyPath, txpin));
		}
		// Check if the randomized change output position was swapped compared to the one provided
		// Fully rebuilding the transaction might also be better ... 
		if (unsignedtx.outputs.length == 2) {
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
		return initialize();
	}
	
	private boolean initialize() {
	    // wait until a device is connected		
		while (!getTransport().isPluggedIn()) {
			dongle = null;
			try {
				setState(Status.unableToScan, currentAccountState);
				Thread.sleep(PAUSE_RESCAN);
			} catch (InterruptedException e) {
				break;
			}			
		}
		if (getTransport().connect(_context)) {
			getTransport().getTransport().setDebug(true);
			dongle = new BTChipDongle(getTransport().getTransport());
			if (!(getTransport().getTransport() instanceof LedgerTransportTEEProxy)) {
				// Try to activate the Security Card (until done in the Ledger Wallet application)
				try {
					getTransport().getTransport().exchange(ACTIVATE_ALT_2FA);
				}
				catch(Exception e) {				
				}
			}
		}
		return (dongle != null);
	}
	
	public boolean isPluggedIn() {
		return getTransport().isPluggedIn();
	}

	@Override
	public UUID createOnTheFlyAccount(HdKeyNode accountRoot,
			WalletManager walletManager, int accountIndex) {
		UUID account;
		if (walletManager.hasAccount(accountRoot.getUuid())){
	         // Account already exists
	         account = accountRoot.getUuid();
		}else {
			account = walletManager.createExternalSignatureAccount(accountRoot, this, accountIndex);
		}
		return account;
	}

	@Override
	public Optional<HdKeyNode> getAccountPubKeyNode(int accountIndex) {
		boolean isTEE = getTransport().getTransport() instanceof LedgerTransportTEEProxy; 
		String keyPath = "44'/" + getNetwork().getBip44CoinType().getLastIndex() + "'/" + accountIndex + "'";
		try {
			BTChipDongle.BTChipPublicKey publicKey = null;
			try {
				publicKey = dongle.getWalletPublicKey(keyPath);				
			}
			catch(BTChipException e) {
				if (isTEE && (e.getSW() == SW_CONDITIONS_NOT_SATISFIED)) {
					LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy)getTransport().getTransport();
					// Not setup ? We can do it on the fly
					byte header, headerP2SH;
        			dongle.setup(new BTChipDongle.OperationMode[] { BTChipDongle.OperationMode.WALLET}, 
        					new BTChipDongle.Feature[] { BTChipDongle.Feature.RFC6979 }, // TEE doesn't need NO_2FA_P2SH
        					getNetwork().getStandardAddressHeader(), 
        					getNetwork().getMultisigAddressHeader(), 
        					new byte[4], null,
        					null, 
        					null, null);        			
					try {
						proxy.writeNVM("nvm.bin", proxy.requestNVM().get());
					}
					catch(Exception e1) {								
					}
        			dongle.verifyPin(DUMMY_PIN.getBytes());
					try {
						proxy.writeNVM("nvm.bin", proxy.requestNVM().get());
					}
					catch(Exception e1) {								
					}
					publicKey = dongle.getWalletPublicKey(keyPath);
				}
				else
				if (e.getSW() == SW_PIN_NEEDED) {
					//if (dongle.hasScreenSupport()) {
					if (isTEE) {
						// PIN request is prompted on screen
						dongle.verifyPin(DUMMY_PIN.getBytes());
						if (isTEE) {
							// Poor man counter
							LedgerTransportTEEProxy proxy = (LedgerTransportTEEProxy)getTransport().getTransport();
							try {
								proxy.writeNVM("nvm.bin", proxy.requestNVM().get());
							}
							catch(Exception e1) {								
							}
						}						
						publicKey = dongle.getWalletPublicKey(keyPath);
					}
					else
					if (handler != null) {
						String pin = handler.onPinRequest();
						dongle.verifyPin(pin.getBytes());
						publicKey = dongle.getWalletPublicKey(keyPath);
					}
					else {
						throw e;
					}
				}
			}			
			PublicKey pubKey = new PublicKey(KeyUtils.compressPublicKey(publicKey.getPublicKey()));
			HdKeyNode accountRootNode = new HdKeyNode(pubKey, publicKey.getChainCode(), 3, 0, accountIndex);
			return Optional.of(accountRootNode);
		}
		catch(Exception e) {
			postErrorMessage(e.getMessage());
			return Optional.absent();			
		}
	}

	@Override
	public int getBIP44AccountType() {
		return Bip44AccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER;
	}
	
	public String getLabelOrDefault() {
		return _context.getString(R.string.ledger);
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
	
	private SharedPreferences.Editor getEditor() {
		return _context.getSharedPreferences(Constants.LEDGER_SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
	}
	
	
	

}
