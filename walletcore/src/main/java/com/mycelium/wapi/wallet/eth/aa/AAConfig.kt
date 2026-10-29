package com.mycelium.wapi.wallet.eth.aa

/**
 * Per-chain configuration of the ERC-20 gas payment ("pay the fee in the token") built on
 * ERC-4337 (UserOperations through a bundler, fee taken by an ERC-20 paymaster) and EIP-7702
 * (the EOA temporarily runs smart-account code, so no separate smart account is deployed).
 *
 * Ported from WalletKMP `com.mycelium.wapi.wallet.aa`.
 */
data class ChainAAConfig(
    val chainId: Long,
    /** ERC-4337 EntryPoint v0.8 (required for EIP-7702). */
    val entryPointAddress: String,
    /** ERC-20 paymaster the fee token is approved to. */
    val paymasterAddress: String,
    /** Bundler JSON-RPC: eth_estimateUserOperationGas, eth_sendUserOperation, ... */
    val bundlerUrl: String,
    /** Plain Ethereum JSON-RPC for eth_call / eth_getCode / eth_getTransactionCount. */
    val ethRpcUrl: String,
    /** Smart-account implementation the EOA delegates to (EIP-7702). */
    val simpleAccountImpl: String,
    /** Tokens the fee may be paid in. */
    val supportedFeeTokens: List<FeeTokenConfig>,
    /**
     * Endpoint of `pm_getPaymasterStubData` / `pm_getPaymasterData`. Null = the bundler URL
     * (Pimlico serves both and signs with its own key, markup goes to Pimlico).
     */
    val paymasterRpcUrl: String? = null
) {
    val effectivePaymasterRpcUrl: String get() = paymasterRpcUrl ?: bundlerUrl

    fun feeToken(contractAddress: String): FeeTokenConfig? =
        supportedFeeTokens.firstOrNull { it.contractAddress.equals(contractAddress, ignoreCase = true) }
}

data class FeeTokenConfig(
    val symbol: String,
    val contractAddress: String,
    val decimals: Int
)

object AAChains {
    /** ERC-4337 EntryPoint v0.8 — native EIP-7702 support. */
    const val ENTRY_POINT_V08 = "0x4337084d9e255ff0702461cf8895ce9e3b5ff108"

    /** Simple7702Account v0.8, deterministic address on every chain. */
    const val SIMPLE_7702_ACCOUNT_V08 = "0x4Cd241E8d1510e30b2076397afc7508Ae59C66c9"

    /** Pimlico's public ERC-20 paymaster singleton (no markup of our own). */
    const val PIMLICO_ERC20_PAYMASTER = "0x888888888888Ec68A58AB8094Cc1AD20Ba3D2402"

    const val ETHEREUM_MAINNET = 1L
    const val ETHEREUM_SEPOLIA = 11155111L

    private const val ETH_RPC_MAINNET = "https://ethereum-rpc.publicnode.com"
    private const val ETH_RPC_SEPOLIA = "https://ethereum-sepolia-rpc.publicnode.com"

    fun pimlicoBundlerUrl(chainId: Long, apiKey: String) =
        "https://api.pimlico.io/v2/$chainId/rpc?apikey=$apiKey"

    fun mainnet(bundlerUrl: String, paymasterRpcUrl: String? = null) = ChainAAConfig(
        chainId = ETHEREUM_MAINNET,
        entryPointAddress = ENTRY_POINT_V08,
        paymasterAddress = PIMLICO_ERC20_PAYMASTER,
        bundlerUrl = bundlerUrl,
        ethRpcUrl = ETH_RPC_MAINNET,
        simpleAccountImpl = SIMPLE_7702_ACCOUNT_V08,
        paymasterRpcUrl = paymasterRpcUrl,
        supportedFeeTokens = listOf(
            FeeTokenConfig("USDC", "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48", 6),
            FeeTokenConfig("USDT", "0xdAC17F958D2ee523a2206206994597C13D831ec7", 6)
        )
    )

    fun sepolia(bundlerUrl: String, paymasterRpcUrl: String? = null) = ChainAAConfig(
        chainId = ETHEREUM_SEPOLIA,
        entryPointAddress = ENTRY_POINT_V08,
        paymasterAddress = PIMLICO_ERC20_PAYMASTER,
        bundlerUrl = bundlerUrl,
        ethRpcUrl = ETH_RPC_SEPOLIA,
        simpleAccountImpl = SIMPLE_7702_ACCOUNT_V08,
        paymasterRpcUrl = paymasterRpcUrl,
        supportedFeeTokens = listOf(
            FeeTokenConfig("USDC", "0x1c7D4B196Cb0C7B01d743Fbc6116a902379C7238", 6),
            FeeTokenConfig("USDT", "0xd077A400968890Eacc75cdc901F0356c943e4fDb", 6)
        )
    )
}
