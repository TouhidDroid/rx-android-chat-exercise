package com.futurice.rxandroidchatexercise;

import java.util.List;

import retrofit2.http.GET;
import rx.Observable;

public interface ChatMessageApi {
    @GET("/messages")
    Observable<List<String>> messages();
}
