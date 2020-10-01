package com.mycelium.wapi.wallet.fio;

import java.util.List;

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent;

public class FioGroup {

    public String status;
    public final List<FIORequestContent> children;

    public FioGroup(String string, List<FIORequestContent> requestContents) {
        this.status = string;
        children = requestContents;
    }


}