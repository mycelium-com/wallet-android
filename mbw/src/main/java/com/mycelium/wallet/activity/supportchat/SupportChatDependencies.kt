package com.mycelium.wallet.activity.supportchat

import com.mycelium.supportchat.data.ChatIdentityManagerImpl
import com.mycelium.supportchat.data.ChatRepository
import com.mycelium.supportchat.data.IdentityAccountKeyManager
import com.mycelium.supportchat.data.ImageUploadService
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.functions.functions

/**
 * Manual dependency holder for SupportChat integration.
 * No DI framework is used in mbw — dependencies are wired manually here.
 */
object SupportChatDependencies {

    var hasAnimatedThisSession = false

    val identityManager: ChatIdentityManagerImpl by lazy {
        ChatIdentityManagerImpl {
            val mbw = MbwManager.getInstance(WalletApplication.getInstance())
            val walletcoreManager = mbw.masterSeedManager
                .getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher())
            object : IdentityAccountKeyManager {
                override fun getPublicKeyForWebsite(key: String, cipher: KeyCipher): ByteArray =
                    walletcoreManager.getPrivateKeyForWebsite(key, cipher).publicKey.publicKeyBytes

                override fun getPrivateKeyForWebsite(
                    key: String,
                    cipher: KeyCipher
                ) = walletcoreManager.getPrivateKeyForWebsite(key, cipher)
            }
        }
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(Firebase.firestore, Firebase.functions, identityManager)
    }

    val imageUploadService: ImageUploadService by lazy {
        ImageUploadService()
    }
}
