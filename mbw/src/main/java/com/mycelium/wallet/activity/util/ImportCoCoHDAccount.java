package com.mycelium.wallet.activity.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.SyncMode;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.colu.ColuModule;
import com.mycelium.wapi.wallet.colu.PublicColuAccount;
import com.mycelium.wapi.wallet.colu.PrivateColuConfig;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImportCoCoHDAccount extends AsyncTask<Void, Integer, UUID> {
    private final HdKeyNode hdKeyNode;
    private final Context context;
    private ProgressDialog dialog;
    private MbwManager mbwManager;
    private Value mtFound = Value.zeroValue(MTCoin.INSTANCE);
    private Value rmcFound = Value.zeroValue(RMCCoin.INSTANCE);
    private Value massFound = Value.zeroValue(MASSCoin.INSTANCE);
    private int scanned = 0;
    private List<WalletAccount> accountsCreated = new ArrayList<>();
    private FinishListener finishListener;
    private int existingAccountsFound;

    public ImportCoCoHDAccount(Context context, HdKeyNode hdKeyNode) {
        this.hdKeyNode = hdKeyNode;
        this.context = context;
        mbwManager = MbwManager.getInstance(context);
    }

    public void setFinishListener(FinishListener finishListener) {
        this.finishListener = finishListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog = new ProgressDialog(context);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setTitle(context.getString(R.string.digital_assets_retrieve));
        dialog.setMessage(context.getString(R.string.coco_addresses_scanned, 0));
        dialog.show();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        dialog.setMessage(context.getString(R.string.coco_addresses_scanned, values[0]));
    }

    @Override
    protected UUID doInBackground(Void... voids) {
        final int coloredLookAheadHD = 20;
        int emptyHD = 0;
        int accountIndex = 0;
        while (emptyHD < coloredLookAheadHD) {
            emptyHD = processAddressLevel(emptyHD, accountIndex);
            ++accountIndex;
        }

        //Make sure that accounts are up to date
        mbwManager.getWalletManager(false).startSynchronization(SyncMode.FULL_SYNC_ALL_ACCOUNTS);
        for (WalletAccount account : accountsCreated) {
            Value spendableBalance = account.getAccountBalance().confirmed;
            if (account.getCoinType().equals(MASSCoin.INSTANCE)) {
                massFound = massFound.add(spendableBalance);
            } else if (account.getCoinType().equals(RMCCoin.INSTANCE)) {
                rmcFound = rmcFound.add(spendableBalance);
            } else if (account.getCoinType().equals(MTCoin.INSTANCE)) {
                mtFound = mtFound.add(spendableBalance);
            }
        }
        return accountsCreated.isEmpty() ? null : accountsCreated.get(0).getId();
    }

    /**
     * Processes address level for selected account level
     *
     * @return returns new emptyHD value
     */
    private int processAddressLevel(int emptyHD, int accountIndex) {
        final String coCoDerivationPath = "m/44'/0'/%d'/0/%d";
        int empty = 0;
        int addressIndex = 0;
        int coloredLookAhead = 2;
        while (empty < coloredLookAhead) {
            HdKeyNode currentNode = hdKeyNode.createChildNode(HdKeyPath.valueOf(String.format(coCoDerivationPath, accountIndex, addressIndex)));
            Address address = currentNode.getPublicKey().toAddress(mbwManager.getNetwork(), AddressType.P2PKH);
            Optional<UUID> accountId;
            accountId = mbwManager.getAccountId(AddressUtils.fromAddress(address), null);
            if (accountId.isPresent()) {
                existingAccountsFound++;
                addressIndex++;
                empty = 0;
                emptyHD = 0;
                continue;
            }
            WalletManager walletManager = mbwManager.getWalletManager(false);
            if (((ColuModule)walletManager.getModuleById(ColuModule.ID)).getColuApi()
                    .getAddressTransactions(new BtcAddress(null, address)).size() > 0) {
                empty = 0;
                emptyHD = 0;
                try {
                    addCoCoAccount(currentNode, walletManager);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                empty++;
            }

            publishProgress(++scanned);
            if (empty == coloredLookAhead && empty == addressIndex + 1) {
                emptyHD++;
            }
            addressIndex++;
        }
        return emptyHD;
    }

    private void addCoCoAccount(HdKeyNode currentNode, WalletManager walletManager) throws IOException {
        //Check if there were any known assets
        List<UUID> ids = walletManager.createAccounts(new PrivateColuConfig(currentNode.getPrivateKey(), AesKeyCipher.defaultKeyCipher()));
        for (UUID id : ids) {
            WalletAccount account = walletManager.getAccount(id);
            if (account instanceof PublicColuAccount) {
                accountsCreated.add(account);
            }
        }
    }

    @Override
    protected void onPostExecute(UUID account) {
        dialog.dismiss();
        if (accountsCreated.isEmpty() && existingAccountsFound == 0 && finishListener != null) {
            finishListener.finishCoCoNotFound(hdKeyNode);
        } else if (finishListener != null) {
            finishListener.finishCoCoFound(account, accountsCreated.size(), existingAccountsFound, mtFound, massFound, rmcFound);
        }
    }

    public interface FinishListener {
        void finishCoCoNotFound(HdKeyNode hdKeyNode);

        void finishCoCoFound(final UUID firstAddedAccount, int accountsCreated, int existingAccountsFound, Value mtFound,
                             Value massFound, Value rmcFound);
    }
}
