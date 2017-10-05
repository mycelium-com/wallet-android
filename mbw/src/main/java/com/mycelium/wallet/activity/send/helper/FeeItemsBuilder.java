package com.mycelium.wallet.activity.send.helper;

import android.support.annotation.NonNull;

import com.mrd.bitlib.StandardTransactionBuilder;
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

import static com.mrd.bitlib.StandardTransactionBuilder.estimateTransactionSize;

public class FeeItemsBuilder {
    private static final int MIN_NO_ZIRO_FEE = 1000;
    private static final int HALF_FEE_ITEMS_COUNT = 5;

    private MbwManager _mbwManager;
    private FeeEstimation feeEstimation;

    public FeeItemsBuilder(MbwManager _mbwManager) {
        this._mbwManager = _mbwManager;
        this.feeEstimation = _mbwManager.getWalletManager(false).getLastFeeEstimations();
    }

    public List<FeeItem> getFeeItemList(MinerFee feeLvl, StandardTransactionBuilder.UnsignedTransaction unsignedTransaction) {
        long min = MIN_NO_ZIRO_FEE;
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

        List<FeeItem> feeItems = new ArrayList<>();
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));
        addItemsInRange(feeItems, min, current, unsignedTransaction);
        addItemsInRange(feeItems, current, max, unsignedTransaction);
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));

        return feeItems;
    }

    private void addItemsInRange(List<FeeItem> feeItems, long from, long to
            , StandardTransactionBuilder.UnsignedTransaction _unsigned) {
        int inCount = _unsigned != null ? _unsigned.getFundingOutputs().length : 1;
        int outCount = _unsigned != null ? _unsigned.getOutputs().length : 2;
        int txSize = estimateTransactionSize(inCount, outCount);
        long step = Math.max((to - from) / HALF_FEE_ITEMS_COUNT, 1);
        if (from == MIN_NO_ZIRO_FEE) {
            feeItems.add(createFeeItem(txSize, 0));
        }
        for (long i = from, j = 0; i < to && j < HALF_FEE_ITEMS_COUNT; i += step, j++) {
            feeItems.add(createFeeItem(txSize, i));
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
