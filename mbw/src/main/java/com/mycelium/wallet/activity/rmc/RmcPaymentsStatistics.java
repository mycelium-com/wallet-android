package com.mycelium.wallet.activity.rmc;

import com.google.api.client.util.DateTime;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wapi.model.ExchangeRate;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RmcPaymentsStatistics {
    public static final int MAX_TRANSACTION_RETRIEVAL_LIMIT = 1000;
    public static final String CURRENCY = "USD";

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
            result.add(summary);
        }
        return result;
    }

    public LineGraphSeries<DataPoint> getStatistics()
    {
        List<DataPoint> dataPoints = new ArrayList<>();
        List<TransactionSummary> txSummaries = getTransactionSummaries();

        if (txSummaries.size() > 0) {
            TransactionSummary firstTransaction = txSummaries.get(0);

            Calendar curEndWeekInstance = Calendar.getInstance();
            curEndWeekInstance.setTimeInMillis(firstTransaction.time);
            curEndWeekInstance.set(Calendar.DAY_OF_WEEK, 7);

            int dataPointIndex = -1;

            for(int i = 0; i < txSummaries.size(); i++ ) {
                TransactionSummary curTransaction = txSummaries.get(0);
                Calendar curTxTimeCalendar = Calendar.getInstance();
                curTxTimeCalendar.setTimeInMillis(curTransaction.time);

                ExchangeRate rate = exchangeRateManager.getExchangeRate(CURRENCY);

                double curValue = curTransaction.value.getValue().doubleValue() * rate.price;

                if (curTxTimeCalendar.before(curEndWeekInstance)) {

                    processDataPoint(dataPoints, dataPointIndex, curValue);

                } else {
                    dataPointIndex = dataPointIndex + 1;
                    curEndWeekInstance.setTimeInMillis(firstTransaction.time);
                    curEndWeekInstance.set(Calendar.DAY_OF_WEEK, 7);

                    processDataPoint(dataPoints, dataPointIndex, curValue);
                }
            }

        }

        return new LineGraphSeries<>(dataPoints.toArray(new DataPoint[dataPoints.size()]));
    }

    private void processDataPoint(List<DataPoint> dataPoints, int dataPointIndex, double curValue) {
        DataPoint dataPoint;
        if (dataPointIndex < dataPoints.size()) {
            dataPoint = new DataPoint(dataPointIndex + 1, curValue);
        } else {
            dataPoint = dataPoints.get(dataPointIndex);
        }

        if (dataPointIndex < dataPoints.size()) {
            dataPoints.add(dataPoint);
        } else {
            double accumulatedValue = dataPoint.getY();
            accumulatedValue += curValue;
            dataPoints.set(dataPointIndex, new DataPoint(dataPointIndex, accumulatedValue));
        }
    }

}