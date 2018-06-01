package com.mycelium.wallet.external.news;

import com.mycelium.wallet.external.news.model.NewsContainer;

import retrofit.http.GET;

public interface NewsService {

    @GET("/posts")
    NewsContainer posts();
}
