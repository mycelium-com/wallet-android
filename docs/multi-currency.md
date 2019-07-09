# Multi currency architecture

In order to support multiple asset classes, Mycelium Wallet has a set of abstract classes 
and interfaces to provide the ability to describe any type of asset.

An asset implementation involves *WalletAccount* interface methods' realization:

```
public interface WalletAccount<A extends GenericAddress> {

    GenericTransaction createTransaction(GenericAddress address, Value amount, GenericFee fee);
    void signTx(GenericTransaction request, KeyCipher keyCipher);
    BroadcastResult broadcastTx(GenericTransaction tx);
    Balance getAccountBalance();
    List<TransactionSummaryGeneric> getTransactionSummaries(int offset, int limit);

    ...
}

```
The methods of [WalletAccount](../walletcore/src/main/java/com/mycelium/wapi/wallet/WalletAccount.java) cover all operations necessary to manage an asset. 
The Mycelium Wallet's user is able to get the balance of the specific asset, get a list of transactions,
prepare a transaction by signing it, broadcast the transaction to the network. 

As long as a majority of crypto-currencies have their own transaction formats
and address representation, the *WalletAccount* interface use templates. To describe the specific 
assets' transactions and its address representation, the custom classes implementing *GenericAddress* and *GenericTransaction*
should be created.
 
Below is an example of some assets implementing *WalletAccount* interface:


![Image](images/accs.png)

## Creating a new Wallet

Mycelium Wallet Core provides the *WalletManager* class to manage user's crypto assets. Before starting
to use its methods, a configuration of data storage and master seed setup should be provided.

### Data storage configuration

Since the Wallet Core library is able to run on different environments supporting Java platform 
(Android, Desktop, ...) , it is flexible to use any required data storage.
To implement it, *WalletBacking* interface is used. There are some existing implementation like
InMemoryBtcWalletManagerBacking for in-memory storage or SqliteBtcWalletManagerBacking for Android's
Sqlite database.

```
val backing = InMemoryBtcWalletManagerBacking()

```

### Master seed configuration

Master seed could be created randomly or restored from known list of words.
It is stored inside the dedicated store called *SecureKeyValueStore*.

```
val store =  SecureKeyValueStore(backing, MyRandomSource());
val masterSeedManager = MasterSeedManager(store);
val randomMasterSeed = createRandomMasterSeed();
val restoredMasterSeed =  Bip39.generateSeedFromWordList(new String[]{"cliff", "battle","noise","aisle","inspire","total","sting","vital","marble","add","daring","mouse"}, "");
masterSeedManager.configureBip32MasterSeed(masterSeed, AesKeyCipher.defaultKeyCipher());

```

### Creating WalletManager instance

To create a WalletManager object, it should be configured by setting up ElectrumX servers and 
network to work with (mainnet, testnet)

```
val tcpEndpoints = arrayOf(TcpEndpoint("electrumx-aws-test.mycelium.com", 19335))
val wapiClient = WapiClientElectrumX(testnetWapiEndpoints, tcpEndpoints, wapiLogger, "0")
val walletManager = WalletManager(
                network,
                wapiClient,
                currenciesSettingsMap)
```

### Adding modules

A Wallet Core module is a set of classes that supports the specific crypto-asset. In order to
add bitcoin cryptocurrency support, an object of class *BitcoinHDModule* should be added to
WalletManager:

```
    val bitcoinHDModule = BitcoinHDModule(backing as BtcWalletManagerBacking<HDAccountContext>, store, network, wapiClient, btcSettings, storage, null, null, null)
    walletManager.add(bitcoinHDModule)
```

## Getting account balance

```
     Balance balance = account.getAccountBalance();
``` 

## Getting a list of transactions

The example below retrieves the first 100 transactions for the account:

```
     List<GenericTransaction> transactions = account.getTransactions(0, 100);
``` 

## Creating and broadcasting a transaction

As mentioned below, a transaction is created in generic way using a set of *WalletAccount* methods.

Let's assume we have an instance of WalletAccount and want to send 1000000 cryptocurrency units
from this account to an address. The receiving address object is an instance of GenericAddress.

At the first step, create a Value object based on asset type 
and a number of transferred currency units:
 
```
     Value amountToSend = Value.valueOf(asset, 1000000);
``` 

Before creating a transaction, we should specify fee size.Crypto-currencies have different fee
 policies. For example, bitcoin-based cryptocurrencies use well-known Fee-per-KB strategy.
For example we can specify 30000 satoshis per kilobyte:

```
     GenericFee fee = new FeePerKb(Value.valueOf(asset, 30000)
```


It's time to create a send request object using the information about the receiving address and the 
amount to send:

```      
     GenericTransaction tx = account.createTx(toAddress, amountToSend, fee);
```      


If the account does not have enough funds to execute the transaction, an appropriate exception will
be thrown.  

Since the transaction object is ready, the transaction need to be signed:

``` 
     account.signTx(tx);
```    

And we can broadcast the transaction:    
    
``` 
     account.broadcastTx(tx);
```    

## Retrieving fee estimations

Each asset has its own fee policy and ways to get an average fee.
The fees information could be retrieved using *WalletManager*.
