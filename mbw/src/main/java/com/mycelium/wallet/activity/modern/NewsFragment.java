package com.mycelium.wallet.activity.modern;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.ResolvedType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.adapter.NewsAdapter;
import com.mycelium.wallet.external.news.NewsFactory;
import com.mycelium.wallet.external.news.model.News;

import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewsFragment extends Fragment {

    @BindView(R.id.list_news)
    RecyclerView recyclerView;

    private NewsAdapter adapter;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getActivity().getSharedPreferences("news", Context.MODE_PRIVATE);
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_news, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, true));
        adapter = new NewsAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setClickListener(new NewsAdapter.ClickListener() {
            @Override
            public void onClick(News news) {
                Intent s = new Intent(android.content.Intent.ACTION_SEND);
                s.setType("text/plain");
                s.putExtra(Intent.EXTRA_SUBJECT, news.title);
                s.putExtra(Intent.EXTRA_TEXT, news.link);
                startActivity(Intent.createChooser(s, "Share News"));
            }
        });
        String savedNews = sharedPreferences.getString("news", null);
        if(savedNews != null) {
            try {
                adapter.setData((List<News>) NewsFactory.objectMapper.readValue(savedNews, new TypeReference<List<News>>() {}));;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        new AsyncTask<Void, Void, List<News>>() {
            @Override
            protected List<News> doInBackground(Void... voids) {
                return NewsFactory.getService().posts().posts;
            }

            @Override
            protected void onPostExecute(List<News> news) {
                super.onPostExecute(news);
                adapter.setData(news);
                try {
                    String s = NewsFactory.objectMapper.writeValueAsString(news);
                    sharedPreferences.edit().putString("news", s).apply();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
