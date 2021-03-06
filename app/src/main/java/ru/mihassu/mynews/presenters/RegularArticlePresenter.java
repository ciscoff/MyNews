package ru.mihassu.mynews.presenters;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.reactivex.observers.DisposableObserver;
import ru.mihassu.mynews.data.eventbus.ActualDataBus;
import ru.mihassu.mynews.data.repository.RoomRepoBookmark;
import ru.mihassu.mynews.domain.entity.ArticleCategory;
import ru.mihassu.mynews.domain.model.DataSnapshot;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.presenters.i.ArticlePresenter;
import ru.mihassu.mynews.ui.fragments.main.MainFragmentState;
import ru.mihassu.mynews.ui.web.BrowserLauncher;

import static ru.mihassu.mynews.Utils.logIt;

public class RegularArticlePresenter implements ArticlePresenter {

    private ActualDataBus dataBus;
    private RoomRepoBookmark repoBookmark;
    private MainFragmentState currentState;
    private BrowserLauncher browserLauncher;

    public RegularArticlePresenter(ActualDataBus dataBus,
                                   RoomRepoBookmark repoBookmark,
                                   BrowserLauncher browserLauncher) {
        this.dataBus = dataBus;
        this.repoBookmark = repoBookmark;
        this.browserLauncher = browserLauncher;
        subscribeToDataSources();
    }

    private void subscribeToDataSources() {
        dataBus
                .connectToActualData()
                .subscribe(new DisposableObserver<DataSnapshot>() {
                    @Override
                    public void onNext(DataSnapshot dataSnapshot) {
                        if (currentState != null) {
                            currentState.updateState(dataSnapshot);
                        } else {
                            currentState = new MainFragmentState(dataSnapshot);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        logIt("RAP:: subscribe error\n" + e.getMessage());
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    @Override
    public int getTabCount() {
        if (currentState != null) {
            return currentState.getCurrentSortedArticles().keySet().size();
        }

        return 0;
    }

    @Override
    public void onClickBookmark(long articleId) {
        if (currentState != null) {
            MyArticle article = findArticle(articleId);
            article.isMarked = !article.isMarked;

            // Обновить базу
            if (article.isMarked) {
                repoBookmark.insertArticle(article);
            } else {
                repoBookmark.deleteArticle(article);
            }
        }
    }

    @Override
    public void onClickArticle(String articleUrl) {
        browserLauncher.showInBrowser(articleUrl);
    }


    /**
     * Выдать список статей согласно индексу таба (отдельная категория статей)
     */
    @Override
    public List<MyArticle> getTabArticles(int tabPosition) {

        if (currentState != null) {

            EnumMap<ArticleCategory, List<MyArticle>> classifiedNews = currentState.getCurrentSortedArticles();

            if (classifiedNews != null && classifiedNews.size() != 0) {
                ArrayList<Map.Entry<ArticleCategory, List<MyArticle>>> allArticles =
                        new ArrayList<>(classifiedNews.entrySet());

                return allArticles.get(tabPosition).getValue();
            }
        }
        return new ArrayList<>();
    }

    /**
     * Выдать все статьи
     */
    @Override
    public List<MyArticle> getArticles() {
        if (currentState != null) {
            return currentState.getLastUpdateArticles();
        }
        return new ArrayList<>();
    }

    @Override
    public MyArticle getArticle(int listPosition) {
        return null;
    }

    @Override
    public String getHighlight() {
        return currentState.getQuery();
    }

    // Найти статью в общем списке по её ID
    private MyArticle findArticle(long articleId) {
        return currentState
                .getLastUpdateArticles()
                .stream()
                .filter((a) -> a.id == articleId)
                .collect(Collectors.toList())
                .get(0);
    }

}
