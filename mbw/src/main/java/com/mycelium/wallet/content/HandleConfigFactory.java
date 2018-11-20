package com.mycelium.wallet.content;


public class HandleConfigFactory {
    public static StringHandleConfig returnKeyOrAddressOrUriOrKeynode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction();
        request.hdNodeAction = new HdNodeAction();
        request.popAction = StringHandleConfig.PopAction.SEND;
        return request;
    }

    public static StringHandleConfig returnKeyOrAddressOrHdNode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.hdNodeAction = new HdNodeAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction(true);
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        return request;
    }

    public static StringHandleConfig spendFromColdStorage() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction(true);
        request.hdNodeAction = new HdNodeAction();
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        request.wordListAction = StringHandleConfig.WordListAction.COLD_SPENDING;
        return request;
    }

    public static StringHandleConfig getAddressBookScanRequest() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction(true);
        return request;
    }

    public static StringHandleConfig genericScanRequest() {
        StringHandleConfig request = new StringHandleConfig();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction();
        request.bitIdAction = StringHandleConfig.BitIdAction.LOGIN;
        request.privateKeyAction = new PrivateKeyAction();
        request.websiteAction = StringHandleConfig.WebsiteAction.HANDLE_URL;
        request.sssShareAction = StringHandleConfig.SssShareAction.START_COMBINING;
        request.wordListAction = StringHandleConfig.WordListAction.COLD_SPENDING;
        request.hdNodeAction = new HdNodeAction();
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
