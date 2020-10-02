package com.mycelium.wapi.wallet.fio;

import java.util.List;

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent;

public class FioGroup {

    enum Type {sent, pending}

    public Type status;
    public final List<FIORequestContent> children;

    public FioGroup(Type string, List<FIORequestContent> requestContents) {
        this.status = string;
        children = requestContents;
    }


}