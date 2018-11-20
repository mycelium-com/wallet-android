package com.mycelium.wallet.content;


public class HandleConfigFactory {
    public static StringHandleConfig returnKeyOrAddressOrUriOrKeynode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = StringHandleConfig.AddressAction.RETURN;
        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.RETURN;
        request.bitcoinUriAction = new UriAction();
        request.hdNodeAction = StringHandleConfig.HdNodeAction.RETURN;
        request.popAction = StringHandleConfig.PopAction.SEND;
        return request;
    }

//    public static StringHandleConfig returnKeyOrAddress() {
//        StringHandleConfig request = new StringHandleConfig();
//        request.privateKeyAction = new PrivateKeyAction();
//        request.addressAction = StringHandleConfig.AddressAction.RETURN;
//        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.RETURN_ADDRESS;
//        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
//        return request;
//    }

    public static StringHandleConfig returnKeyOrAddressOrHdNode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.hdNodeAction = StringHandleConfig.HdNodeAction.RETURN;
        request.addressAction = StringHandleConfig.AddressAction.RETURN;
        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.RETURN_ADDRESS;
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        return request;
    }

    public static StringHandleConfig spendFromColdStorage() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = StringHandleConfig.AddressAction.CHECK_BALANCE;
        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.CHECK_BALANCE;
        request.hdNodeAction = StringHandleConfig.HdNodeAction.COLD_SPENDING;
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        request.wordListAction = StringHandleConfig.WordListAction.COLD_SPENDING;
        return request;
    }

    public static StringHandleConfig getAddressBookScanRequest() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = StringHandleConfig.AddressAction.RETURN;
        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.RETURN_ADDRESS;
        return request;
    }

    public static StringHandleConfig genericScanRequest() {
        StringHandleConfig request = new StringHandleConfig();
        request.addressAction = StringHandleConfig.AddressAction.RETURN;
        request.bitcoinUriWithAddressAction = StringHandleConfig.BitcoinUriWithAddressAction.SEND;
        request.bitcoinUriAction = new UriAction();
        request.bitIdAction = StringHandleConfig.BitIdAction.LOGIN;
        request.privateKeyAction = new PrivateKeyAction();
        request.websiteAction = StringHandleConfig.WebsiteAction.HANDLE_URL;
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        request.wordListAction = StringHandleConfig.WordListAction.COLD_SPENDING;
        request.hdNodeAction = StringHandleConfig.HdNodeAction.SEND_PUB_SPEND_PRIV;
        request.popAction = StringHandleConfig.PopAction.SEND;

        //at the moment, we just support wordlist backups
        //request.masterSeedAction = MasterSeedAction.IMPORT;
        return request;
    }

    public static StringHandleConfig getShare() {
        StringHandleConfig request = new StringHandleConfig();
        request.sssShareAction = StringHandleConfig.SssShareAction.RETURN_SHARE;
        return request;
    }

    public static StringHandleConfig verifySeedOrKey() {
        StringHandleConfig request = new StringHandleConfig();
        request.masterSeedAction = StringHandleConfig.MasterSeedAction.RETURN;
        request.privateKeyAction = new PrivateKeyAction();
        return request;
    }
}
