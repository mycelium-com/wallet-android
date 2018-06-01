package com.mycelium.wallet.activity.modern.adapter;


import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.adapter.holder.LinksViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.NewsViewHolder;
import com.mycelium.wallet.external.news.model.News;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public final static int TYPE_NEWS = 1;
    public final static int TYPE_LINKS = 2;

    private List<News> data = new ArrayList<>();
    private ClickListener clickListener;

    public void setData(List<News> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setClickListener(ClickListener clickListener) {
        this.clickListener = clickListener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_NEWS) {
            return new NewsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false));
        } else if (viewType == TYPE_LINKS) {
            return new LinksViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_links, parent, false));
        }
        return null;
    }

    static Map<String, Drawable> images = new HashMap<>();

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (getItemViewType(position) == TYPE_NEWS) {
            final NewsViewHolder newsViewHolder = (NewsViewHolder) holder;
            final News news = data.get(position);
            newsViewHolder.title.setText(news.title);
            newsViewHolder.description.setText(Html.fromHtml(news.content, new Html.ImageGetter() {
                @Override
                public Drawable getDrawable(final String s) {
                    if (!images.containsKey(s)) {
                        Glide.with(newsViewHolder.description.getContext())
                                .load(s)
                                .into(new SimpleTarget<Drawable>() {
                                    @Override
                                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                        int viewWidth = newsViewHolder.description.getWidth();
                                        if (resource.getIntrinsicWidth() < viewWidth) {
                                            resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());
                                        } else {
                                            resource.setBounds(0, 0, viewWidth, viewWidth * resource.getIntrinsicHeight() / resource.getIntrinsicWidth());
                                        }
                                        images.put(s, resource);
                                        notifyItemChanged(position);
                                    }
                                });
                    }
                    return images.get(s);
                }
            }, null));
            newsViewHolder.date.setText(DateUtils.getRelativeTimeSpanString(news.date.getTime()));
            newsViewHolder.author.setText(news.author.name);
            Glide.with(newsViewHolder.authorLogo.getContext())
                    .load(news.author.avatar)
                    .into(newsViewHolder.authorLogo);
            newsViewHolder.share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (clickListener != null) {
                        clickListener.onClick(news);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
//        if (position == 0) {
//            return TYPE_LINKS;
//        } else {
        return TYPE_NEWS;
//        }
    }

    public interface ClickListener {
        void onClick(News news);
    }
}