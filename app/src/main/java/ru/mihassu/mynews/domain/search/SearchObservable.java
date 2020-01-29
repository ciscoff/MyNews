package ru.mihassu.mynews.domain.search;

<<<<<<< HEAD
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
=======
import androidx.appcompat.widget.SearchView;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
>>>>>>> sy_dev
import io.reactivex.schedulers.Schedulers;

public class SearchObservable {

    public static Observable<String> fromView(SearchView searchView) {

<<<<<<< HEAD
        Observable<String> searchObservable = Observable.create((ObservableOnSubscribe<String>) emitter ->

            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                emitter.onNext(s.toLowerCase());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                emitter.onNext(s.toLowerCase());
                return true;
            }
        }))
//                .debounce(1, TimeUnit.SECONDS)
                .switchMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        return Observable.just(s).delay(1, TimeUnit.SECONDS);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        return searchObservable;
=======
        return Observable.create((ObservableOnSubscribe<String>) emitter ->

                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String s) {
                        emitter.onNext(s.toLowerCase());
                        searchView.clearFocus();
                        return true;
                    }

                    @Override
                    public boolean onQueryTextChange(String s) {
                        emitter.onNext(s.toLowerCase());
                        return true;
                    }
                }))
                .debounce(1000, TimeUnit.MILLISECONDS)
                .switchMap(text -> Observable.fromCallable(() -> text.toLowerCase().trim()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
>>>>>>> sy_dev
    }
}
