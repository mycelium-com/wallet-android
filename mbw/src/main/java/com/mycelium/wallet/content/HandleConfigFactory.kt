package com.mycelium.wallet.content


import com.mycelium.wallet.content.actions.AddressAction
import com.mycelium.wallet.content.actions.BitIdAction
import com.mycelium.wallet.content.actions.HdNodeAction
import com.mycelium.wallet.content.actions.MasterSeedAction
import com.mycelium.wallet.content.actions.PopAction
import com.mycelium.wallet.content.actions.PrivateKeyAction
import com.mycelium.wallet.content.actions.SssShareAction
import com.mycelium.wallet.content.actions.UriAction
import com.mycelium.wallet.content.actions.WebsiteAction
import com.mycelium.wallet.content.actions.WordListAction

object HandleConfigFactory {
    val addressBookScanRequest = StringHandleConfig().apply {
        privateKeyAction = PrivateKeyAction()
        addressAction = AddressAction()
        bitcoinUriAction = UriAction(true)
    }

    val share = StringHandleConfig().apply {
        sssShareAction = SssShareAction()
    }

    fun returnKeyOrAddressOrUriOrKeynode() = StringHandleConfig().apply {
        privateKeyAction = PrivateKeyAction()
        addressAction = AddressAction()
        bitcoinUriAction = UriAction()
        hdNodeAction = HdNodeAction()
        popAction = PopAction()
    }

    fun returnKeyOrAddressOrHdNode() = StringHandleConfig().apply {
        privateKeyAction = PrivateKeyAction()
        hdNodeAction = HdNodeAction()
        addressAction = AddressAction()
        bitcoinUriAction = UriAction(true)
        sssShareAction = SssShareAction()
    }

    fun spendFromColdStorage() = StringHandleConfig().apply {
        privateKeyAction = PrivateKeyAction()
        addressAction = AddressAction()
        bitcoinUriAction = UriAction(true)
        hdNodeAction = HdNodeAction()
        sssShareAction = SssShareAction()
        wordListAction = WordListAction()
    }

    fun genericScanRequest() = StringHandleConfig().apply {
        addressAction = AddressAction()
        bitcoinUriAction = UriAction()
        bitIdAction = BitIdAction()
        privateKeyAction = PrivateKeyAction()
        websiteAction = WebsiteAction()
        sssShareAction = SssShareAction()
        wordListAction = WordListAction()
        hdNodeAction = HdNodeAction()
        popAction = PopAction()
        //at the moment, we just support wordlist backups
        //masterSeedAction = MasterSeedAction.IMPORT;
    }

    fun verifySeedOrKey() = StringHandleConfig().apply {
        masterSeedAction = MasterSeedAction()
        privateKeyAction = PrivateKeyAction()
    }
}