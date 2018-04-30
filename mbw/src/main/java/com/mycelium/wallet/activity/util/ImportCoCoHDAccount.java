package com.mycelium.wallet.activity.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.HdKeyNode;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.colu.ColuManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImportCoCoHDAccount extends AsyncTask<Void, Integer, UUID> {
    private final HdKeyNode hdKeyNode;
    private final Context context;
    private ProgressDialog dialog;
    private MbwManager mbwManager;
    private int accountsCreated;
    private int scanned = 0;
    private UUID firstCoCoUUID = null;
    private FinishListener finishListener;

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
        accountsCreated = 0;
        ColuManager coluManager = mbwManager.getColuManager();
        int accountIndex = 0;
        while (emptyHD < coloredLookAheadHD) {
            emptyHD = processAddressLevel(emptyHD, coluManager, accountIndex);
            ++accountIndex;
        }

        //Make sure that accounts are up to date
        coluManager.scanForAccounts();
        return firstCoCoUUID;
    }

    /**
     * Processes address level for selected account level
     *
     * @return returns new emptyHD value
     */
    private int processAddressLevel(int emptyHD, ColuManager coluManager, int accountIndex) {
        final String coCoDerivationPath = "m/44'/0'/%d'/0/%d";
        int empty = 0;
        int addressIndex = 0;
        int coloredLookAhead = 2;
        while (empty < coloredLookAhead) {
            HdKeyNode currentNode = hdKeyNode.createChildNode(HdKeyPath.valueOf(String.format(coCoDerivationPath, accountIndex, addressIndex)));
            Address address = currentNode.getPublicKey().toAddress(mbwManager.getNetwork());
            Optional<UUID> accountId = mbwManager.getAccountId(address, null);
            if (accountId.isPresent()) {
                addressIndex++;
                empty = 0;
                emptyHD = 0;
                continue;
            }
            try {
                if (coluManager.isColoredAddress(address)) {
                    empty = 0;
                    emptyHD = 0;
                } else {
                    empty++;
                }
                addCoCoAccount(coluManager, currentNode, address);
            } catch (IOException e) {
                e.printStackTrace();
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

    private void addCoCoAccount(ColuManager coluManager, HdKeyNode currentNode, Address address) throws IOException {
        List<ColuAccount.ColuAsset> assetList = new ArrayList<>(coluManager.getColuAddressAssets(address));
        //Check if there were any known assets
        if (!assetList.isEmpty()) {
            accountsCreated++;
            if (firstCoCoUUID == null) {
                firstCoCoUUID = coluManager.enableAsset(assetList.get(0), currentNode.getPrivateKey());
            } else {
                coluManager.enableAsset(assetList.get(0), currentNode.getPrivateKey());
            }
        }
    }

    @Override
    protected void onPostExecute(UUID account) {
        dialog.dismiss();
        if (account != null && finishListener != null) {
            finishListener.finishCoCoFound(account, accountsCreated);
        } else if (finishListener != null) {
            finishListener.finishCoCoNotFound(hdKeyNode);
        }
    }

    public interface FinishListener {
        void finishCoCoNotFound(HdKeyNode hdKeyNode);

        void finishCoCoFound(UUID account, int accountsCreated);
    }
}
