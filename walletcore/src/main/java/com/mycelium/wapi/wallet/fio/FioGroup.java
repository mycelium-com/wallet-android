package com.mycelium.wapi.wallet.fio;

import java.util.List;

import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent;

public class FioGroup {

    enum Type {sent("PAID FIO REQUESTS"), pending("PENDING FOR REQUESTS");

        private String s;

        Type(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }

    public Type status;
    public final List<FIORequestContent> children;

    public FioGroup(Type string, List<FIORequestContent> requestContents) {
        this.status = string;
        children = requestContents;
    }


}