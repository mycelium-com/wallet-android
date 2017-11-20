package com.mycelium.wallet.activity.send.helper;

import android.support.annotation.NonNull;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.api.lib.FeeEstimation;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;

import java.util.ArrayList;
import java.util.List;

public class FeeItemsBuilder {
    private static final int MIN_NON_ZIRO_FEE_PER_KB = 3000;
    private static final int HALF_FEE_ITEMS_COUNT = 5;

    private MbwManager _mbwManager;
    private FeeEstimation feeEstimation;

    public FeeItemsBuilder(MbwManager _mbwManager) {
        this._mbwManager = _mbwManager;
        this.feeEstimation = _mbwManager.getWalletManager(false).getLastFeeEstimations();
    }

    public List<FeeItem> getFeeItemList(MinerFee feeLvl, int txSize) {
        long min = MIN_NON_ZIRO_FEE_PER_KB;
        long current = feeLvl.getFeePerKb(feeEstimation).getLongValue();

        if (feeLvl != MinerFee.LOWPRIO) {
            long prevValue = feeLvl.getPrevious().getFeePerKb(feeEstimation).getLongValue();
            prevValue = prevValue == current ? prevValue / 2 : prevValue;
            min = (current + prevValue) / 2;
        }
        long max = 3 * MinerFee.PRIORITY.getFeePerKb(feeEstimation).getLongValue() / 2;
        if (feeLvl != MinerFee.PRIORITY) {
            max = (feeLvl.getNext().getFeePerKb(feeEstimation).getLongValue() + current) / 2;
        }

        FeeItemsAlgorithm algorithmLower = new LinearAlgorithm(min, 0, current, 4);
        FeeItemsAlgorithm algorithmUpper = new LinearAlgorithm(current, 4, max, 9);
        if (feeLvl == MinerFee.LOWPRIO) {
            algorithmLower = new CubicAlgorithm(min, 1, current, 9);
            algorithmUpper = new LinearAlgorithm(current, 9, max, 12);
        }

        List<FeeItem> feeItems = new ArrayList<>();
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));
        if (feeLvl == MinerFee.LOWPRIO) {
            feeItems.add(createFeeItem(txSize, 0));
        }
        addItemsInRange(feeItems, algorithmLower, txSize);
        addItemsInRange(feeItems, algorithmUpper, txSize);
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));

        return feeItems;
    }

    private void addItemsInRange(List<FeeItem> feeItems, FeeItemsAlgorithm algorithm, int txSize) {
        for (int i = algorithm.getMinPosition(); i < algorithm.getMaxPosition(); i++) {
            feeItems.add(createFeeItem(txSize, algorithm.computeValue(i)));
        }
    }

    @NonNull
    private FeeItem createFeeItem(int txSize, long feePerKb) {
        ExactBitcoinValue bitcoinValue;
        if (_mbwManager.getSelectedAccount() instanceof ColuAccount) {
            long fundingAmountToSend = _mbwManager.getColuManager().getColuTransactionFee(feePerKb);
            bitcoinValue = ExactBitcoinValue.from(fundingAmountToSend);
        } else {
            bitcoinValue = ExactBitcoinValue.from(txSize * feePerKb / 1000);
        }
        CurrencyValue fiatFee = CurrencyValue.fromValue(bitcoinValue,
                _mbwManager.getFiatCurrency(), _mbwManager.getExchangeRateManager());
        return new FeeItem(feePerKb, bitcoinValue.getAsBitcoin(), fiatFee, FeeViewAdapter.VIEW_TYPE_ITEM);
    }
}
