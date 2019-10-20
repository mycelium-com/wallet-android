package com.mycelium.wallet.external.mediaflow;

import com.mycelium.wallet.external.mediaflow.model.NewsContainer;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;


public interface NewsApiService {
    String RESPONSE_FIELDS = "&fields=ID,title,content,excerpt,date,author,short_URL,featured_image,categories";

    @GET("posts?number=100" + RESPONSE_FIELDS)
    Call<NewsContainer> updatedPosts(@Query("modified_after") String updateAfterDate, @Query("page_handle") String nextPage);

    @GET("posts?number=100" + RESPONSE_FIELDS)
    Call<NewsContainer> posts(@Query("page_handle") String nextPage);
}
