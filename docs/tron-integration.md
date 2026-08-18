# TRON / TRC-20 integration

Branch: `feature/tron`. Ported from `WalletKMP` (`walletCore/.../tron`, `trc20`) **without TrustWallet Core**.

## What is supported

| Area | Status |
|---|---|
| TRON account from masterseed (`m/44'/195'/n'/0/0`, one address per account) | ✅ created by default alongside BTC/ETH (`StartupActivity.mainAccounts`, `MbwManager.checkMainAccountsCreated`) |
| Additional TRON accounts / watch-only address accounts | ✅ Add Account → *Add TRON Account*, Advanced → clipboard/scan of a `T…` address |
| TRC-20 token accounts (parent/child like ERC-20) | ✅ Add Account → *Add TRC-20 Account*; token list `WalletConfiguration.TRC20_TOKENS` |
| TRX send / TRC-20 send | ✅ `SendTronViewModel/Model`, fee from `TronFeeEstimator` (bandwidth, energy, fee_limit, activation fee) |
| Receive (QR, `tron:` URI, `req-asset` for tokens) | ✅ |
| History (all contract types classified by `TronTxType`), tx details, block explorers (tronscan / blockchair) | ✅ |
| Message signing (TIP-191) | ✅ `TronKeys.signMessage` |
| Exchange (mbwswaps) | ✅ TRX and TRC-20; as in KMP the token goes as its plain ticker plus `fromNetwork/toNetwork = "trx"` on the rate request (`Changelly2Repository.exchangeNetwork`), the network is bound to `rateId`; TRC-20 → payin can pay the fee via the energy service (`ExchangeFragment.sendViaEnergyService`) |
| Energy service ("pay the TRC-20 fee in the token") | ✅ see below |
| Staking, custom TRC-20 tokens, permission-hijack audit (present in KMP) | ❌ not ported |

## Architecture (walletcore)

- `wallet/tron/`
  - `TronAddress` — base58check `0x41 + 20 bytes`, `TronKeys` — address derivation (keccak via web3j) and 65-byte `r||s||recId` signing.
  - `TronTransactionBuilder` + `ProtoWriter` — hand-rolled protobuf for `Transaction.raw` (`TransferContract`, `TriggerSmartContract`, `fee_limit`) and the signed `Transaction`. `txID = sha256(raw_data)` is computed locally; TAPOS from `wallet/getnowblock`.
    Verified byte-for-byte against TrustWallet Core vectors (`TronSignerTest`).
  - `TronModule`/`TronAccount`, `TronBacking`, `TronAccountContext`, `TronFeeEstimator`, `providers/TronFeeProvider`.
  - `remote/TronBlockchainService` — TronGrid-compatible HTTP API (OkHttp + Jackson), per-endpoint rate gate, cooldown on 429/403, failover; `broadcasthex` for sending.
- `wallet/trc20/` — `TRC20Module`, `TRC20Account` (holds parent `tronAcc`), `TRC20Backing`, `coins/TRC20Token`.
- `genericdb/TronAccountBacking` + SQLDelight `TronContext.sq`, `Trc20Context.sq`, `TronAccountBacking.sq`, migration `15.sqm`.
- `content/tron/TronUriParser` (`tron:<addr>?amount=`, TRC-21 `tron:<contract>?address=&amount=`, `req-asset`).

Values of TRC-20 accounts are persisted with the TRX coin type (DB adapter only knows registered coins) and re-tagged on read — same rule as ERC-20.

## Energy service (pay TRC-20 fees in USDT)

Partner HTTP API (`X-API-Key`): `GET health`, `GET api/v1/fee-info?sender&recipient&token_contract&amount`,
`POST api/v1/transfer/execute {transaction_id, signed_tx_fee, signed_tx_main}`, `GET api/v1/order/{id}/status`.

- `walletcore/.../tron/energy/`: `EnergyServiceClient` (OkHttp+Jackson), `EnergyServiceManager` (availability →
  quote → two signed transfers → poll → local history rows + `EnergyServiceFeeLink`), models, `EnergyOrderStore`
  (mbw: `PrefsEnergyOrderStore`, per-account pending order marker with self-healing on next open).
- Wire format of `signed_tx_*`: hex of a TrustWallet-Core-compatible `Tron.SigningOutput` protobuf
  (`TronTransactionBuilder.encodeSigningOutput`: id, signature, ref_block_bytes, ref_block_hash, json with
  `txID/raw_data/raw_data_hex/signature`) — what the KMP wallet passed through from `AnySigner.sign()`.
  Both txs are plain `transfer(address,uint256)` calls signed with `raw_data.expiration = now + 30 min` and
  the estimator's recommended fee_limit; the partner broadcasts them (fee first).
- UI (`SendTronModel/ViewModel`, `send_coins_fee_block_tron.xml`): for TRC-20 accounts, when the partner is
  healthy and sponsors the token, a "Pay fee in TRX / Pay fee in USDT" toggle appears (USDT preselected, as
  in KMP; hidden — USDT only — when the parent has no TRX). Quote is refreshed on address/amount change; Send
  requires `amount + fee ≤ balance`. Progress dialog while the order is processed; `Pending` after the 2 min
  poll window keeps the marker and blocks a second send until resolved.
- Config: `BuildConfig.EnergyServiceUrl/EnergyServiceApiKey` per flavor+buildType (prodnet debug → staging
  `164.90.215.54:8005`, release → `api.energly20.com`, btctestnet → `164.90.215.54:8003`); override with
  `-PenergyServiceUrl=… -PenergyServiceApiKey=…`. Cleartext to `164.90.215.54` allowed in
  `network_security_config.xml`.
- Status (Aug 17 2026): the testnet partner answers `INVALID_API_KEY` for the known keys and lists no tokens;
  staging mainnet works (health/fee-info verified from the app). Real execution not yet exercised.

## Configuration

- Endpoints: `nodes-b.json` (bundled asset, then `wallet-config.mycelium.com/nodes-b-v3160200[-test].json`) sections
  `TRX-mainnet` / `TRX-testnet` in the **WalletKMP `wallet-config` layout** — the KMP section can be pasted verbatim:
  ```json
  "TRX-mainnet": { "endpoints": [
    { "url": "https://api.trongrid.io/", "type": "DEFAULT", "header-key": "TRON-PRO-API-KEY", "header-value": "enc2_…", "priority": 6, "rps": 15 },
    { "url": "https://api.trongrid.io/", "type": "DEFAULT", "priority": 7, "rps": 1 }
  ] }
  ```
  This app only speaks the TronGrid full HTTP API, so `type: DEFAULT` (or no type) entries are used, ordered by `priority`,
  `rps` feeds the per-endpoint gate; `GRPC`/`REST` entries (used by KMP's other providers) are skipped. Without any usable
  entry: `BuildConfig.TronServers` (`https://api.trongrid.io` prodnet, `https://nile.trongrid.io` testnet). Parsed
  endpoints are logged (key masked): `TRON endpoints: https://api.trongrid.io/(prio=6, rps=15, key=yes), …`.
- **TronGrid API key**: without a key TronGrid throttles to ~1 request/s (15 QPS with a key). Either pass it at build time
  (`./gradlew ... -PtronGridApiKey=<key>` or `tronGridApiKey=<key>` in `~/.gradle/gradle.properties` → `BuildConfig.TronGridApiKey`)
  or put it into the config `header-value` (with `header-key: TRON-PRO-API-KEY`). Config values may be obfuscated the same way as in WalletKMP
  (`walletcore/.../ConfigObfuscation`): `enc2_<hex>` = AES-128-CTR `[IV16][ciphertext]` of the UTF-8 text with the shared
  constant key (`enc1_` for raw bytes → hex); plain values still work. The KMP `wallet-config-prodnet.json` value for
  `TRON-PRO-API-KEY` can be copied verbatim. To produce a value:
  `(openssl rand -hex 16 | tr -d '\n'; echo -n "<key>" | openssl enc -aes-128-ctr -K a8105c3c8b75556a9099b8dcab9cc133 -iv <same iv> -nosalt | xxd -p)` → prefix `enc2_`,
  or `ConfigObfuscation.encodeText("<key>")` from a unit test. This is obfuscation, not protection.
- Rates: `wapi.mycelium.com/wapi/getExchangeRates` returns no rates for `TRX` yet → fiat balance/“No Exchange available” until the backend adds TRX. USDT is available.

## Tests

- `walletcore/src/test/.../tron/TronSignerTest` — address vectors, transfer + TRC-20 signing vectors, protobuf, TAPOS.
- `walletcore/src/test/.../tron/TronBlockchainServiceIntegrationTest` (`@Ignore("network")`) — live TronGrid: block ref, balance/resources, history, TRC-20 balance/transfers/energy estimate, and a full sign+broadcast round trip from an empty account (node answers `CONTRACT_VALIDATE_ERROR: account does not exist`, i.e. encoding, TAPOS and signature were accepted).

## Manual test checklist

1. Fresh wallet → Accounts shows *TRON & TRC-20* group with `Tron 1`.
2. Add TRC-20 (USDT) on `Tron 1`; both accounts sync (`Synchronizing TRX/USDT account … success` in logcat).
3. Fund the TRX address (mainnet or Nile testnet with the `btctestnet` flavor), send TRX to another address; then send USDT (needs ≥ ~15 TRX on the parent for the fee limit; actual fee is much lower).
4. Watch-only: Advanced → clipboard with a `T…` address → history and tx details.
