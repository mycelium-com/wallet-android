package com.mycelium.wallet.activity.send

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.common.base.Preconditions
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.extsig.keepkey.activity.KeepKeySignTransactionActivity
import com.mycelium.wallet.extsig.ledger.activity.LedgerSignTransactionActivity
import com.mycelium.wallet.extsig.trezor.activity.TrezorSignTransactionActivity
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.RuntimeException
import java.util.UUID

open class SignTransactionActivity : AppCompatActivity() {
    @JvmField
    protected var _mbwManager: MbwManager? = null

    @JvmField
    protected var _account: WalletAccount<*>? = null
    protected var _isColdStorage: Boolean = false

    @JvmField
    protected var _transaction: Transaction? = null
    private var signingTask: Job? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setView()
        _mbwManager = MbwManager.getInstance(this)
        // Get intent parameters
        val accountId =
            Preconditions.checkNotNull<UUID>(getIntent().getSerializableExtra(SendCoinsActivity.ACCOUNT) as UUID?)
        _isColdStorage = getIntent().getBooleanExtra(SendCoinsActivity.IS_COLD_STORAGE, false)
        _account = Preconditions.checkNotNull(
            _mbwManager!!.getWalletManager(_isColdStorage).getAccount(accountId)
        )
        _transaction =
            Preconditions.checkNotNull<Transaction?>(getIntent().getSerializableExtra(TRANSACTION) as Transaction?)

        // Load state
        if (savedInstanceState != null) {
            // May be null
            _transaction = savedInstanceState.getSerializable(TRANSACTION) as Transaction?
        }
    }

    protected open fun setView() {
        setContentView(R.layout.sign_transaction_activity)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (_transaction != null) {
            outState.putSerializable(TRANSACTION, _transaction)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        if (signingTask == null) {
            signingTask = signTransaction()
        }
        super.onResume()
    }

    protected fun signTransaction(): Job {
        cancelSigningTask()
        return lifecycleScope.launch(Dispatchers.Default) {
            val signedTransaction = try {
                _account!!.signTx(_transaction!!, AesKeyCipher.defaultKeyCipher())
                if (_transaction!!.txBytes() != null) _transaction else null
            } catch (e: InvalidKeyCipher) {
                throw RuntimeException("doInBackground" + e.message)
            }
            withContext(Dispatchers.Main) {
                if (signedTransaction != null) {
                    val ret = Intent()
                    ret.putExtra(SendCoinsActivity.SIGNED_TRANSACTION, signedTransaction)
                    setResult(RESULT_OK, ret)
                } else {
                    setResult(RESULT_CANCELED)
                }
                this@SignTransactionActivity.finish()
            }
        }
    }

    protected fun cancelSigningTask() {
        signingTask?.cancel()
        signingTask = null
    }

    companion object {
        const val TRANSACTION: String = "transaction"

        fun callMe(
            currentActivity: Activity,
            account: UUID,
            isColdStorage: Boolean,
            transaction: Transaction?,
            requestCode: Int
        ) {
            currentActivity.startActivityForResult(
                getIntent(currentActivity, account, isColdStorage, transaction), requestCode
            )
        }

        @JvmStatic
        fun getIntent(
            currentActivity: Activity?,
            account: UUID,
            isColdStorage: Boolean,
            transaction: Transaction?
        ): Intent {
            val walletAccount =
                MbwManager.getInstance(currentActivity).getWalletManager(isColdStorage)
                    .getAccount(account)

            var targetClass: Class<*>?
            if (walletAccount is HDAccountExternalSignature) {
                val bip44AccountType = walletAccount.getBIP44AccountType()
                targetClass = when (bip44AccountType) {
                    HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER ->
                        LedgerSignTransactionActivity::class.java

                    HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY ->
                        KeepKeySignTransactionActivity::class.java

                    HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR ->
                        TrezorSignTransactionActivity::class.java

                    else -> throw RuntimeException("Unknown ExtSig Account type " + bip44AccountType)
                }
            } else {
                targetClass = SignTransactionActivity::class.java
            }
            Preconditions.checkNotNull<UUID?>(account)

            return Intent(currentActivity, targetClass)
                .putExtra(SendCoinsActivity.ACCOUNT, account)
                .putExtra(SendCoinsActivity.IS_COLD_STORAGE, isColdStorage)
                .putExtra(TRANSACTION, transaction)
        }
    }
}
