package ru.mihassu.mynews.data.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Observable;
import ru.mihassu.mynews.domain.channel.ChannelParser;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.domain.repository.ChannelRepository;
import ru.mihassu.mynews.domain.repository.RawChannelRepository;

public class ChannelRepositoryImpl implements ChannelRepository {

    private RawChannelRepository repo;
    private ChannelParser parser;

    public ChannelRepositoryImpl(
            RawChannelRepository repo,
            ChannelParser parser) {
        this.repo = repo;
        this.parser = parser;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<List<MyArticle>> updateChannel() {
        return repo
                .sendRequest()
                .map(response -> {
                    try {
                        return parser.parse(response.body().byteStream());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new ArrayList<>();
                    // Отфильровать новости без картинок
                })
                .map(
                        list -> ((List<MyArticle>) list)
                                .stream()
                                .filter(article -> article.image != null)
                                .collect(Collectors.toList()))
                .toObservable();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<List<MyArticle>> updateChannelEx() {
        return repo
                .sendRequestEx()
                .map(byteStream -> {
                    try {
                        return parser.parse(byteStream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new ArrayList<>();
                    // Отфильровать новости без картинок
                })
                .map(
                        list -> ((List<MyArticle>) list)
                                .stream()
                                .filter(article -> article.image != null)
                                .collect(Collectors.toList()))
                .toObservable();
    }
}
