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