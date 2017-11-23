package com.mycelium.wallet.external.changelly;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ChangellyTransactionOffer implements Serializable {
    public String id;
    public double apiExtraFee;
    public double changellyFee;
    public String payinExtraId;
    public String status;
    public String currencyFrom;
    public String currencyTo;
    public double amountTo;
    public double amountFrom; // not set by changelly (!) but we set it on the way back in ChangellyService
    public String payinAddress;
    public String payoutAddress;
    public String createdAt;

}
