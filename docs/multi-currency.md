# Multi currency architecture

In order to support multiple asset classes, Mycelium Wallet has a set of abstract classes 
and interfaces to provide the ability to describe any type of asset.

An asset implementation involves *WalletAccount* interface methods' realization:

```
public interface WalletAccount<T extends GenericTransaction, A extends GenericAddress> {

    void completeAndSignTx(SendRequest<T> request) throws WalletAccountException;
    void completeTransaction(SendRequest<T> request) throws WalletAccountException;
    void signTransaction(SendRequest<T> request) throws WalletAccountException; 
    BroadcastResult broadcastTx(T tx) throws TransactionBroadcastException; 
    GenericAddress getReceiveAddress();
    CryptoCurrency getCoinType();
    Balance getAccountBalance();  
    T getTx(Sha256Hash transactionId); 
    List<GenericTransaction> getTransactions(int offset, int limit); 
    SendRequest getSendToRequest(GenericAddress destination, Value amount); 

    ...
}

```
The methods of [WalletAccount](../walletcore/src/main/java/com/mycelium/wapi/wallet/WalletAccount.java) cover all operations necessary to manage an asset. 
The Mycelium Wallet's user is able to get the balance of the specific asset, get a list of transactions,
prepare a transaction by signing it, broadcast the transaction to the network. 

As long as a majority of crypto-currencies have their own transaction formats
and address representation, the *WalletAccount* interface use templates. To describe the specific 
assets' transactions and its address representation, the custom classes implementing *GenericAddress* and *GenericTransaction*
should be created and used as templates.
 
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

A send request object contains all the nesessary information to prepare a transaction,
including fee size. If a fee size is not specified, a default value is taken.
It's time to create a send request object using the information about the receiving address and the 
amount to send:

```      
     SendRequest sendRequest = account.getSendToRequest(toAddress, amountToSend);     
```      

Since, the send request is ready, the transaction need to be completed and signed:

``` 
     account.completeAndSignTx(sendRequest);
```    

And we can broadcast the transaction:    
    
``` 
     account.broadcastTx(request.tx);
```    

## Retrieving fee estimations

Each asset has its own fee policy and ways to get an average fee.
The fees information could be retrieved using *WalletManager*.