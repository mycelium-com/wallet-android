package com.mycelium.wallet.glidera.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.activities.GlideraTransaction;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.response.OrderState;
import com.mycelium.wallet.glidera.api.response.TransactionResponse;
import com.mycelium.wallet.glidera.api.response.TransactionsResponse;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import rx.Observer;
import rx.functions.Action1;

public class GlideraTransactionHistoryFragment extends ListFragment {
    private GlideraService glideraService;
    private TransactionHistoryAdapter transactionHistoryAdapter;
    private View root;

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
        root = Preconditions.checkNotNull(inflater.inflate(R.layout.glidera_transaction_history, container, false));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

//    @Override
//    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//        TransactionResponse transactionResponse = (TransactionResponse) parent.getAdapter().getItem(position);
//
//        Intent intent = new Intent(getActivity(), GlideraTransaction.class);
//        Bundle bundle = new Bundle();
//        bundle.putString("transactionuuid", transactionResponse.getTransactionUuid().toString());
//        intent.putExtras(bundle);
//        startActivity(intent);
//    }

    private final class TransactionHistoryAdapter extends ArrayAdapter<TransactionResponse> {
        private Context context;

        public TransactionHistoryAdapter(Context context, List<TransactionResponse> objects) {
            super(context, R.layout.glidera_transaction_row, objects);
            this.context = context;
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
            final String date;

            if(DateUtils.isToday(transactionResponse.getTransactionDate().getTime())) {
                date = DateFormat.getTimeInstance().format(transactionResponse.getTransactionDate());
            }
            else {
                date = DateFormat.getDateInstance().format(transactionResponse.getTransactionDate());
            }

            ((TextView) view.findViewById(R.id.tvDate)).setText(date);

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
