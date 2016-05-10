package com.mycelium.wallet.external.glidera.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.AdaptiveDateFormat;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.activities.GlideraTransaction;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.response.OrderState;
import com.mycelium.wallet.external.glidera.api.response.TransactionResponse;
import com.mycelium.wallet.external.glidera.api.response.TransactionsResponse;

import java.util.Date;
import java.util.List;

import rx.Observer;

public class GlideraTransactionHistoryFragment extends ListFragment {
    private GlideraService glideraService;
    private TransactionHistoryAdapter transactionHistoryAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        glideraService = GlideraService.getInstance();
    }

    @Override
    public void onResume() {
        super.onResume();
        glideraService.transaction().subscribe(new Observer<TransactionsResponse>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(TransactionsResponse transactionsResponse) {
                transactionHistoryAdapter = new TransactionHistoryAdapter(getActivity(), transactionsResponse.getTransactions());
                setListAdapter(transactionHistoryAdapter);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_transaction_history, container, false));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    private final class TransactionHistoryAdapter extends ArrayAdapter<TransactionResponse> {
        private Context context;
        private AdaptiveDateFormat adaptiveDateFormat;

        public TransactionHistoryAdapter(Context context, List<TransactionResponse> objects) {
            super(context, R.layout.glidera_transaction_row, objects);
            this.context = context;
            adaptiveDateFormat = new AdaptiveDateFormat(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;

            if (view == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = Preconditions.checkNotNull(vi.inflate(R.layout.glidera_transaction_row, null));
            }

            final TransactionResponse transactionResponse = getItem(position);

            /*
             Date
              */
            Date date = transactionResponse.getTransactionDate();
            TextView tvDate = (TextView) view.findViewById(R.id.tvDate);
            tvDate.setText(adaptiveDateFormat.format(date));

            /*
            Message
             */
            String type = (transactionResponse.getType().equals(TransactionResponse.Type.BUY) ? "bought" : "sold");

            String message = "You " + type + " " + GlideraUtils.formatBtcForDisplay(transactionResponse.getQty()) + " for " +
                    GlideraUtils.formatFiatForDisplay(transactionResponse.getTotal());

            ((TextView) view.findViewById(R.id.tvMessage)).setText(message);

            /*
            Dot
             */
            if (transactionResponse.getStatus().equals(OrderState.COMPLETE)) {
                ((ImageView) view.findViewById(R.id.ivDot)).setImageResource(R.drawable.circle_full_green);
            } else {
                ((ImageView) view.findViewById(R.id.ivDot)).setImageResource(R.drawable.circle_line_white);
            }

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), GlideraTransaction.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("transactionuuid", transactionResponse.getTransactionUuid().toString());
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
            });

            return view;
        }
    }
}
