package com.mycelium.wallet.ledger;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.Vector;

import android.content.Context;

import com.btchip.BTChipDongle;
import com.btchip.BTChipException;
import com.btchip.BitcoinTransaction;
import com.btchip.comm.android.BTChipTransportAndroid;
import com.btchip.utils.KeyUtils;
import com.google.common.base.Optional;
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
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.AccountScanManager.Status;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44AccountContext;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.ExternalSignatureProvider;

public class LedgerManager extends AbstractAccountScanManager implements
		ExternalSignatureProvider {
	
	private BTChipTransportAndroid transport;
	private BTChipDongle dongle;
	protected Events handler=null;
	
	private static final int PAUSE_RESCAN = 4000;
	private static final int SW_PIN_NEEDED = 0x6982;
	
	public interface Events extends AccountScanManager.Events {
		public String onPinRequest();
		public String onUserConfirmationRequest(BTChipDongle.BTChipOutput output);
	}
		
	public LedgerManager(Context context, NetworkParameters network){
		super(context, network);
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
	
	private BTChipTransportAndroid getTransport() {
		if (transport == null) {
			transport = new BTChipTransportAndroid(_context);
		}
		return transport;
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
			// Check pin
			if ((i == 0) && (handler != null)) {
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
		String keyPath = "44'/" + getNetwork().getBip44CoinType().getLastIndex() + "'/" + accountIndex + "'";
		try {
			BTChipDongle.BTChipPublicKey publicKey = null;
			try {
				publicKey = dongle.getWalletPublicKey(keyPath);
			}
			catch(BTChipException e) {
				if (e.getSW() == SW_PIN_NEEDED) {
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

}
