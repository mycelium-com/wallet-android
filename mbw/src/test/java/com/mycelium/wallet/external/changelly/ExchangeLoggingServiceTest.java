package com.mycelium.wallet.external.changelly;


import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer;
import com.mycelium.wallet.external.changelly.model.Order;

import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import retrofit2.Response;

import static org.junit.Assert.assertTrue;

public class ExchangeLoggingServiceTest {
    @Test
    public void testLogging() {
        ChangellyTransactionOffer offer = new ChangellyTransactionOffer();
        offer.amountTo = BigDecimal.valueOf(111.222);
        Order order = new Order();
        order.transactionId = "5b0f10a544b9dfd86595fcea39f822919ee425ec2c7a87a2a29c0467e2669769";
        order.exchangingAmount = "100.1111";
        order.exchangingCurrency = "BCH";
        order.receivingAddress = "myjzzeSEokszvNoe3AQNP583B1f39pHioc";
        order.timestamp = SimpleDateFormat.getDateTimeInstance().format(new Date());
        order.receivingAmount = String.valueOf(offer.amountTo);
        order.receivingCurrency = "BTC";

        try {
            Response<Void> response = ExchangeLoggingService.exchangeLoggingService.saveOrder(order).execute();
            assertTrue(response.isSuccessful());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
