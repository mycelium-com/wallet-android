package com.mycelium.wallet.external.mediaflow;

import com.mycelium.wallet.external.mediaflow.model.NewsContainer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface NewsApiService {
    String responseFields = "&fields=ID,title,content,excerpt,date,author,short_URL,featured_image,categories";

    @GET("posts?number=100" + responseFields)
    Call<NewsContainer> posts(@Query("after") String afterDate);

    @GET("posts?number=100" + responseFields)
    Call<NewsContainer> posts();
}
