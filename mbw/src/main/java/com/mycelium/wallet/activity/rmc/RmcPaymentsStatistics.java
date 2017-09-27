package com.mycelium.wallet.activity.rmc;

import com.jjoe64.graphview.series.DataPoint;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class RmcPaymentsStatistics {
    public static final int MAX_TRANSACTION_RETRIEVAL_LIMIT = 1000;
    public static final String CURRENCY = "USD";
    public static final int MILLISECONDS_IN_SECOND = 1000;

    private ColuAccount coluAccount;
    private final ExchangeRateManager exchangeRateManager;

    public RmcPaymentsStatistics(ColuAccount coluAccount, ExchangeRateManager exchangeRateManager) {
        this.coluAccount = coluAccount;
        this.exchangeRateManager = exchangeRateManager;
    }

    public List<TransactionSummary> getTransactionSummaries() {
        SingleAddressAccount linkedAccount = coluAccount.getLinkedAccount();

        List<TransactionSummary> txSummaries = linkedAccount.getTransactionHistory(0, MAX_TRANSACTION_RETRIEVAL_LIMIT);
        List<TransactionSummary> result = new ArrayList<>();
        for (TransactionSummary summary : txSummaries) {
            if (!summary.isIncoming)
                continue;

            TransactionDetails txDetails = linkedAccount.getTransactionDetails(summary.txid);

            boolean isChangeAddressFounded = false;
            for (TransactionDetails.Item item : txDetails.outputs) {
                if (item.address.toString().equals(BuildConfig.RMCChangeAddress)) {
                    isChangeAddressFounded = true;
                    break;
                }
            }

            if (!isChangeAddressFounded)
                continue;

            result.add(summary);
        }
        return result;
    }

    public List<DataPoint> getStatistics() {
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TransactionSummary> txSummaries = getTransactionSummaries();

        if (txSummaries.size() > 0) {
            ExchangeRate rate = exchangeRateManager.getExchangeRate(CURRENCY);
            if (rate == null) {
                return dataPoints;
            }

//            TransactionSummary firstTransaction = txSummaries.get(0);

//            Calendar curEndWeekInstance = Calendar.getInstance();
//            curEndWeekInstance.setTimeInMillis(firstTransaction.time * MILLISECONDS_IN_SECOND);
//
//            int dayOfWeek = curEndWeekInstance.get(Calendar.DAY_OF_WEEK);
//            curEndWeekInstance.set(Calendar.DAY_OF_MONTH, curEndWeekInstance.get(Calendar.DAY_OF_MONTH) + (7 - dayOfWeek));

            int dataPointIndex = 0;

            for (int i = 0; i < txSummaries.size(); i++) {
                TransactionSummary curTransaction = txSummaries.get(i);
                Calendar curTxTimeCalendar = Calendar.getInstance();
                curTxTimeCalendar.setTimeInMillis(curTransaction.time * MILLISECONDS_IN_SECOND);

                BigDecimal currencyValue = curTransaction.value.getValue();
                if (currencyValue == null)
                    continue;

                double curValue = currencyValue.doubleValue() * rate.price
                        / CurrencyValue.fromValue(coluAccount.getCurrencyBasedBalance().confirmed, CURRENCY, exchangeRateManager).getValue().doubleValue();



//                if (curTxTimeCalendar.before(curEndWeekInstance)) {
//                processDataPoint(dataPoints, curTxTimeCalendar.getTime(), dataPointIndex++, curValue);
                dataPoints.add(new DataPoint(curTxTimeCalendar.getTime(), curValue));
//                } else {
//                    dataPointIndex = dataPointIndex + 1;
//                    curEndWeekInstance.setTimeInMillis(curTransaction.time);
//
//                    dayOfWeek = curEndWeekInstance.get(Calendar.DAY_OF_WEEK);
//                    curEndWeekInstance.set(Calendar.DAY_OF_MONTH, curEndWeekInstance.get(Calendar.DAY_OF_MONTH) + (7 - dayOfWeek));
//
//                    processDataPoint(dataPoints, curEndWeekInstance.getTime(), dataPointIndex, curValue);
//                }
            }
        }

        //Temporarily show the sample data
        if (dataPoints.size() == 0 && BuildConfig.FLAVOR.equals("btctestnet")) {
            Calendar calendar = Calendar.getInstance();
            List<TransactionSummary> summaries = coluAccount.getTransactionHistory(0, 1);
            if (summaries.size() > 0) {
                calendar.setTimeInMillis(summaries.get(0).time * MILLISECONDS_IN_SECOND);
            } else {
                calendar.set(2017, 8, 1);
            }
            Random random = new Random(System.currentTimeMillis());
            double shift = 0;
            for (int i = 0; i < 17; i++) {
                Date date = calendar.getTime();
                dataPoints.add(new DataPoint(date, Math.sin(i / 20.0f) / 70 + 0.14 + shift));
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                if (i % 60 == 59) shift = random.nextDouble() / 40;
            }
        }

        Collections.sort(dataPoints, new Comparator<DataPoint>() {
            @Override
            public int compare(DataPoint dataPoint, DataPoint t1) {
                return (int) (dataPoint.getX() - t1.getX());
            }
        });

        return dataPoints;
    }

    private void processDataPoint(List<DataPoint> dataPoints, Date date, int dataPointIndex, double curValue) {
        DataPoint dataPoint;

        boolean addNew = dataPoints.size() == 0 || (dataPointIndex > dataPoints.size());

        if (addNew) {
            dataPoint = new DataPoint(date, curValue);
        } else {
            dataPoint = dataPoints.get(dataPointIndex);
        }

        if (addNew) {
            dataPoints.add(dataPoint);
        } else {
            double accumulatedValue = dataPoint.getY();
            accumulatedValue += curValue;
            dataPoints.set(dataPointIndex, new DataPoint(date, accumulatedValue));
        }
    }

}