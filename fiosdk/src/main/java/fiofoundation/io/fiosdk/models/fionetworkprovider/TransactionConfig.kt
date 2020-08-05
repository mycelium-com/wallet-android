package fiofoundation.io.fiosdk.models.fionetworkprovider

class TransactionConfig {

    private companion object{
        private val DEFAULT_BLOCKS_BEHIND: Int = 3
        private val DEFAULT_EXPIRES_SECONDS: Int = 3 * 60
    }

    var expiresSeconds: Int = DEFAULT_EXPIRES_SECONDS
    var blocksBehind: Int = DEFAULT_BLOCKS_BEHIND
}