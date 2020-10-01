package com.mycelium.wallet.activity.main.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent;

public class FioGroup {

    public String status;
    public final List<FIORequestContent> children;

    public FioGroup(String string, List<FIORequestContent> requestContents) {
        this.status = string;
        children = requestContents;
    }


}