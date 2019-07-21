package com.mycelium.wapi.wallet.eth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.infura.InfuraHttpService;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class EthBalanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EthBalanceService.class);
    private final Web3j web3j;
    private final Address address;
    private BigInteger cachedBalance = BigInteger.valueOf(0);

    public EthBalanceService(String address) {
        this.web3j =  Web3j.build((new InfuraHttpService("https://ropsten.infura.io/WKXR51My1g5Ea8Z5Xh3l")));

        this.address = new Address(address);
        //this.address = "0x81b7E08F65Bdf5648606c89998A9CC8164397647"; //sample eth wallet with balance

    }

    //for tests
    public EthBalanceService(String address, Web3j injectedWeb3j, Boolean updateCacheInstantly) {
        this.address = new Address(address);
        this.web3j = injectedWeb3j;

    }

    public void updateBalanceCache() {
        cachedBalance = getBalanceSynchronously();
    }

    private BigInteger getBalanceSynchronously() {
        BigInteger balanceInEth = cachedBalance; //cached balance by default if e.g. no network

        try
        {
            Request<?, EthGetBalance> balanceRequest = web3j.ethGetBalance(address.toString(), DefaultBlockParameterName.PENDING);
            EthGetBalance balanceResult = balanceRequest.send();
            balanceInEth = balanceResult.getBalance();
        }
        catch(SocketTimeoutException | UnknownHostException e)
        {
            LOGGER.debug(e.getMessage());
        } catch(Exception e)
        {
            throw new RuntimeException(e);
        }

        return balanceInEth;
    }

    public BigInteger getBalance() {
        return cachedBalance;
    }
}
