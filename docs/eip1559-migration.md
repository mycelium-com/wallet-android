# ETH EIP-1559 fees — as-built (Web3j + Blockbook HTTP)

EIP-1559 (type-2) fees for the ETH send path in this repo (wallet-android-private).
This is an **as-built record** of what is implemented, grounded in our stack — Web3j
4.12.3 for signing and an HTTP fee feed — not the WalletKMP (TrustWallet Core +
Blockbook WebSocket) design this was first ported from.

> Stack note. We do **not** use TrustWallet Core, and we do **not** read fees over a
> Blockbook WebSocket. So the WalletKMP "`maxFeePerGas` is mislabeled / recompute it
> from `baseFee * headroom + tip`" gotcha does **not** apply here — our HTTP feed
> already returns base fee, per-tier max fee and per-tier priority fee as separate,
> correctly-named fields.

## Status

| area | state |
|------|-------|
| per-chain rollout flag | done — on for testnet, off (legacy) for mainnet |
| fee snapshot fetch + cache + tip recovery | done |
| type-2 send (ETH + ERC-20) | done |
| broadcast persistence | done (stores the cap) |
| type-2 cancel / RBF | done (snapshot-fallback bump) |
| fee refresh on Send / Exchange entry | done |
| strict +10%-on-both RBF (persisted tip) | **deferred** — needs a DB column |
| testnet estimator endpoint | **open** — host looks mainnet-only |

With the flag off, behaviour is byte-for-byte the old legacy gas price + type-0 path.

## Rollout flag

`EthCoin.supportsEip1559` (`walletcore/.../eth/coins/EthCoin.kt`), default `false`.
`EthTest` overrides it to `true`; `EthMain` stays `false`. Gas is always paid in ETH, so
ERC-20 sends key off the **ETH** coin's flag (`basedOnCoinType`), not the token.

## Data source & snapshot

Fees come from the dedicated estimator (`EIP1559_ESTIMATOR_URL` in
`EthBlockchainService.kt`, `https://bb-eth.mycelium.com:8181/eth`,
[eip1559_gas_estimator](https://github.com/AlhimicMan/eip1559_gas_estimator)). One request
returns `base_fee_per_gas` plus four `price_levels`, each carrying its own
`max_fee_per_gas` (the cap) and `max_priority_fee_per_gas` (the tip) — already
correctly-named, so there is no formula/headroom step.

`EthBlockchainService` parses this into an `Eip1559Snapshot(baseFeePerGas, tiers)` where
each `Eip1559Tier` is `(level, maxFeePerGas, maxPriorityFeePerGas)`. Estimator levels map
to our UI tiers as `safe_low→LOW`, `average→ECONOMY`, `fast→NORMAL`, `fastest→HIGH`.

`EthFeeProvider.getGasPriceEstimates()` drives it: when `supportsEip1559`, it fetches the
snapshot, caches it (`service.cacheEip1559Fees`), and builds `FeeEstimationsGeneric` from
each tier's **cap**. When the chain is on legacy it calls `service.clearEip1559Fees()` and
uses the per-block `estimatefee` answers as the tiers.

When the estimator fails on a 1559 chain the cached snapshot is **kept** (it is logged at
WARNING) and the provider falls back to blockbook's `estimatefee`
(`legacyFallbackEstimates`). Blockbook's Ethereum `estimatefee` ignores the block target and
answers `baseFeePerGas + 1000 wei` for every one, so every answer is a base-fee reading: the
highest of the four is taken as the base fee and the tiers are derived with `deriveFeeTiers`,
and the snapshot is cached from that. The four answers differing by a few wei (a block
between the calls, or `endpoints.random()` hitting servers at different heights) used to be
read as "the server differentiates" — that cleared the snapshot and the next send signed
legacy at ~base fee while the picker showed 1559 caps (Sepolia `0x7a4237e0…`, 2026-09-15).
A 1559 chain now never signs legacy from the fallback path, so the cached snapshot and the
displayed caps cannot diverge.

## Carrying the tip (cap → tip recovery)

The fee picker passes only one value per tier (`FeePerKbFee.feePerKb`), i.e. the chosen
**maxFeePerGas cap**. The matching tip is recovered at tx-build time:

- `EthTransactionData.maxPriorityFeePerGas` — an explicit tip (e.g. dApp/WC flows), used
  if present; otherwise
- `EthBlockchainService.priorityFeeFor(maxFeePerGas)` — picks the tier whose cap is
  nearest the chosen value, clamped to `[1 wei, cap − baseFee]`. Returns `null` when no
  snapshot is cached or the cap leaves no headroom above the base fee → the build falls
  back to legacy.

The shared resolver is `AbstractEthERC20Account.eip1559Tip(feeCoin, maxFeePerGas,
explicitTip)`, used by both `EthAccount` and `ERC20Account`.

## Transaction model

`EthTransaction` carries the legacy `gasPrice` plus optional `maxFeePerGas` /
`maxPriorityFeePerGas`. `isEip1559` is true only when both type-2 fields are set;
`effectiveGasPrice` returns the cap for type-2, else the gas price; `totalFee()` accounts
on `effectiveGasPrice`.

`createTx` (in `EthAccount` and `ERC20Account`) treats the chosen gas price as the cap,
resolves the tip via `eip1559Tip`, and builds a type-2 `EthTransaction` when a tip exists,
else a legacy one.

## Signing (type-2 on Web3j 4.12.3)

Verified against the library: the 8-arg overload exists and the field order is
`(chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas)`; the
chain id lives in the type-2 envelope so `signMessage` is called **without** it.

```kotlin
val raw = if (tx.isEip1559)
    RawTransaction.createTransaction(chainId, tx.nonce, tx.gasLimit, to, tx.ethValue.value,
        tx.inputData, tx.maxPriorityFeePerGas, tx.maxFeePerGas)
else
    RawTransaction.createTransaction(tx.nonce, tx.gasPrice, tx.gasLimit, to, tx.ethValue.value, tx.inputData)
val signed = if (tx.isEip1559) TransactionEncoder.signMessage(raw, credentials)          // chainId in envelope
             else              TransactionEncoder.signMessage(raw, chainId, credentials) // legacy EIP-155
```

`txHash = Hash.sha3(signed)` / `txBinary = TransactionEncoder.encode(raw)` are unchanged
in both modes — the txid must be keccak over the actually-broadcast bytes or sync creates
a duplicate row. ERC-20 uses the same branch with `to = token.contractAddress` and the
`transfer()` calldata in `inputData`.

## Broadcast persistence

`broadcastTx` (ETH + ERC-20) persists `tx.effectiveGasPrice` (the cap for type-2, the gas
price for legacy) for both the fee column and the `gasPrice` column. Pending type-2 rows
therefore show `cap × gasLimit` (the max), matching the send screen; once the tx mines,
sync overwrites this with the real on-chain effective price + `gasUsed`.

## Cancel / RBF (type-2)

`CancelEstimate` carries optional `newMaxFeePerGas` / `newMaxPriorityFeePerGas` (+
`isEip1559`). When the ETH coin is 1559-enabled, `estimateCancelTx` produces a type-2
estimate; `EthAccount.cancelTransaction` signs the 0-value self-transfer as type-2.

Because the stuck tx's tip is **not persisted**, the bump is derived from the known old
cap (`record.gasPrice`) via `type2CancelFees(bumpedOldCap, networkCap, networkTip)`, which
guarantees `cap ≥ oldCap×1.1`, `tip ≥ oldCap×1.1`, `cap ≥ tip`. Since `oldTip ≤ oldCap`
always, `tip ≥ oldCap×1.1` also clears `oldTip×1.1`, so the replacement is always
mempool-valid without the persisted tip. The current fastest tier (or the legacy network
estimate, if no snapshot) is used as a floor so a cancel during a fee spike still lands.
This also correctly cancels a legacy stuck tx with a type-2 replacement.

## Fee refresh on entry

The snapshot must exist before a tx is built, or the send silently signs legacy (only
possible now when no fee fetch has ever succeeded in this process):

- **Send-ETH** (`SendEthModel`): the `FeeUpdater` now fires its first
  `updateFeeEstimationsAsync()` immediately (initial delay `0`, was the full 30 s
  interval), before the countdown loop.
- **Exchange** (`ExchangeViewModel.refreshEthFeesForSend()` called from `ExchangeFragment`
  just before the final `prepareTx`): refreshes the from-account's fees for ETH/ERC-20
  only, at the single actual-send build point — not on the per-keystroke validation path.

## Tests (`walletcore/src/test/.../eth/`)

| file | covers |
|------|--------|
| `LegacyFallbackEstimatesTest` | blockbook readings that drift by a few wei still yield a cached snapshot + derived tiers on a 1559 chain; legacy chain passes them through and clears |
| `Eip1559SigningTest` | type-2 overload signs a 0x02 envelope; decoded round-trip proves cap/tip field order; legacy stays type-0 |
| `Eip1559SnapshotTest` | `priorityFeeFor` tier match + headroom/floor clamps + clear |
| `EthTransactionFeeTest` | `isEip1559` / `effectiveGasPrice` / `totalFee` accounting |
| `Type2CancelFeesTest` | cancel bump invariants (calm / spike / cap≥tip / 10% threshold) |

## File map (this repo)

| concern | file |
|---------|------|
| per-chain flag | `walletcore/.../eth/coins/EthCoin.kt` (+ `EthTest`) |
| snapshot types, estimator fetch, cache, `priorityFeeFor` | `walletcore/.../eth/EthBlockchainService.kt` |
| fee tiers from snapshot + legacy fallback | `walletcore/.../wallet/providers/EthFeeProvider.kt` |
| tx fee fields (`isEip1559` / `effectiveGasPrice`) | `walletcore/.../eth/EthTransaction.kt` |
| explicit tip input | `walletcore/.../eth/EthTransactionData.kt` |
| shared tip resolver + `CancelEstimate` + `type2CancelFees` | `walletcore/.../eth/AbstractEthERC20Account.kt` |
| build + type-2 signing + broadcast + cancel (ETH) | `walletcore/.../eth/EthAccount.kt` |
| build + type-2 signing + broadcast (ERC-20) | `walletcore/.../erc20/ERC20Account.kt` |
| immediate fee refresh on Send entry | `mbw/.../activity/send/model/SendEthModel.kt` |
| fee refresh for exchange send | `mbw/.../external/changelly2/viewmodel/ExchangeViewModel.kt`, `.../changelly2/ExchangeFragment.kt` |

## Deferred / open

- **Strict RBF (persisted tip).** A `max_priority_fee_per_gas` DB column would let RBF
  bump strictly +10% over the stuck tx's *own* tip instead of over its cap. Needs a new
  migration (`15.sqm`), `EthAccountBacking.sq` schema/queries, all three select mappers,
  `createTransactionSummary`, and the `EthTransactionSummary` model. Deliberately skipped;
  the cap-derived bump above is correct meanwhile.
- **Testnet estimator endpoint.** The estimator host looks mainnet-only. On testnet the
  type-2 *signing* path is still validatable (test ETH is free; mainnet caps just
  overpay), but confirm/point at a testnet estimator before relying on it for real
  testnet fee accuracy.
- **End-to-end verification.** Build a testnet APK and perform a real type-2 ETH + ERC-20
  send and cancel.
- **UX.** Display shows `maxFeePerGas × gasLimit` (the cap); consider "estimated vs max".
