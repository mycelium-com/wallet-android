package com.mycelium.wallet.activity.rmc;

import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class RmcPaymentsStatistics {
    public static final int MAX_TRANSACTION_RETRIEVAL_LIMIT = 1000;
    public static final String CURRENCY = "USD";
    public static final int MILLISECONDS_IN_SECOND = 1000;

    public static final String POOL_ADDRESS = "mx92L6iuCfxQUz4cLNU4jJpfWbavVHgYj9";

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
        for(TransactionSummary summary : txSummaries) {
            if (!summary.isIncoming)
                continue;

            TransactionDetails txDetails = linkedAccount.getTransactionDetails(summary.txid);

            boolean isPoolAddressFounded = false;
            for(TransactionDetails.Item item : txDetails.inputs) {
                if (item.address.toString().equals(POOL_ADDRESS)) {
                    isPoolAddressFounded = true;
                    break;
                }
            }

            if (!isPoolAddressFounded)
                continue;

            result.add(summary);
        }
        return result;
    }

    public LineGraphSeries<DataPoint> getStatistics()
    {
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TransactionSummary> txSummaries = getTransactionSummaries();

        if (txSummaries.size() > 0) {
            ExchangeRate rate = exchangeRateManager.getExchangeRate(CURRENCY);
            if (rate == null) {
                return new LineGraphSeries<>();
            }

            TransactionSummary firstTransaction = txSummaries.get(0);

            Calendar curEndWeekInstance = Calendar.getInstance();
            curEndWeekInstance.setTimeInMillis(firstTransaction.time * MILLISECONDS_IN_SECOND);

            int dayOfWeek = curEndWeekInstance.get(Calendar.DAY_OF_WEEK);
            curEndWeekInstance.set(Calendar.DAY_OF_MONTH, curEndWeekInstance.get(Calendar.DAY_OF_MONTH) + (7 - dayOfWeek));

            int dataPointIndex = 0;

            for(int i = 0; i < txSummaries.size(); i++ ) {
                TransactionSummary curTransaction = txSummaries.get(i);
                Calendar curTxTimeCalendar = Calendar.getInstance();
                curTxTimeCalendar.setTimeInMillis(curTransaction.time);

                BigDecimal currencyValue = curTransaction.value.getValue();
                if (currencyValue == null)
                    continue;

                double curValue = currencyValue.doubleValue() * rate.price;

                if (curTxTimeCalendar.before(curEndWeekInstance)) {
                    processDataPoint(dataPoints, curEndWeekInstance.getTime(), dataPointIndex, curValue);
                } else {
                    dataPointIndex = dataPointIndex + 1;
                    curEndWeekInstance.setTimeInMillis(curTransaction.time);

                    dayOfWeek = curEndWeekInstance.get(Calendar.DAY_OF_WEEK);
                    curEndWeekInstance.set(Calendar.DAY_OF_MONTH, curEndWeekInstance.get(Calendar.DAY_OF_MONTH) + (7 - dayOfWeek));

                    processDataPoint(dataPoints, curEndWeekInstance.getTime(), dataPointIndex, curValue);
                }
            }
        }

        //Temporarily show the sample data
        if (dataPoints.size() == 0) {
            Calendar calendar = Calendar.getInstance();
            Random random = new Random(System.currentTimeMillis());
            double shift = 0;
            for (int i = 0; i < 50; i++) {
                Date date = calendar.getTime();
                dataPoints.add(new DataPoint(date, Math.sin(date.getTime() / 2) / 70 + 0.14 + shift));
                calendar.add(Calendar.DAY_OF_MONTH, 2);
                if(i % 15 == 14) shift = random.nextDouble() / 25;
            }
        }

        return new LineGraphSeries<>(dataPoints.toArray(new DataPoint[dataPoints.size()]));
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