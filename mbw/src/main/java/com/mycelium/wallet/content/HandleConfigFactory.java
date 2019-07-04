package com.mycelium.wallet.content;


import com.mycelium.wallet.content.actions.AddressAction;
import com.mycelium.wallet.content.actions.BitIdAction;
import com.mycelium.wallet.content.actions.HdNodeAction;
import com.mycelium.wallet.content.actions.MasterSeedAction;
import com.mycelium.wallet.content.actions.PopAction;
import com.mycelium.wallet.content.actions.PrivateKeyAction;
import com.mycelium.wallet.content.actions.SssShareAction;
import com.mycelium.wallet.content.actions.UriAction;
import com.mycelium.wallet.content.actions.WebsiteAction;
import com.mycelium.wallet.content.actions.WordListAction;

public class HandleConfigFactory {
    public static StringHandleConfig returnKeyOrAddressOrUriOrKeynode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction();
        request.hdNodeAction = new HdNodeAction();
        request.popAction = new PopAction();
        return request;
    }

    public static StringHandleConfig returnKeyOrAddressOrHdNode() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.hdNodeAction = new HdNodeAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction(true);
        request.sssShareAction = new SssShareAction();
        return request;
    }

    public static StringHandleConfig spendFromColdStorage() {
        StringHandleConfig request = new StringHandleConfig();
        request.privateKeyAction = new PrivateKeyAction();
        request.addressAction = new AddressAction();
        request.bitcoinUriAction = new UriAction(true);
        request.hdNodeAction = new HdNodeAction();
        request.sssShareAction = new SssShareAction();
        request.wordListAction = new WordListAction();
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
        request.bitIdAction = new BitIdAction();
        request.privateKeyAction = new PrivateKeyAction();
        request.websiteAction = new WebsiteAction();
        request.sssShareAction = new SssShareAction();
        request.wordListAction = new WordListAction();
        request.hdNodeAction = new HdNodeAction();
        request.popAction = new PopAction();

        //at the moment, we just support wordlist backups
        //request.masterSeedAction = MasterSeedAction.IMPORT;
        return request;
    }

    public static StringHandleConfig getShare() {
        StringHandleConfig request = new StringHandleConfig();
        request.sssShareAction = new SssShareAction();
        return request;
    }

    public static StringHandleConfig verifySeedOrKey() {
        StringHandleConfig request = new StringHandleConfig();
        request.masterSeedAction = new MasterSeedAction();
        request.privateKeyAction = new PrivateKeyAction();
        return request;
    }
}
