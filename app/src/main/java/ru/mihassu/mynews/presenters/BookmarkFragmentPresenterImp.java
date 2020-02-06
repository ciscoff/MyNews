package ru.mihassu.mynews.presenters;

import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import ru.mihassu.mynews.data.eventbus.ActualDataBus;
import ru.mihassu.mynews.data.repository.RoomRepoBookmark;
import ru.mihassu.mynews.domain.entity.Stack;
import ru.mihassu.mynews.domain.entity.UndoStatus;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.presenters.i.BookmarkFragmentPresenter;
import ru.mihassu.mynews.presenters.i.BookmarkView;
import ru.mihassu.mynews.ui.fragments.bookmark.BookmarkFragmentState;
import ru.mihassu.mynews.ui.web.BrowserLauncher;

import static ru.mihassu.mynews.Utils.logIt;

public class BookmarkFragmentPresenterImp implements BookmarkFragmentPresenter {

    private MutableLiveData<BookmarkFragmentState> liveData = new MutableLiveData<>();
    private BrowserLauncher browserLauncher;
    private RoomRepoBookmark roomRepoBookmark;
    private ActualDataBus dataBus;
    private Disposable disposable;
    private BookmarkView bookmarkView;


    public BookmarkFragmentPresenterImp(ActualDataBus dataBus,
                                        RoomRepoBookmark roomRepoBookmark,
                                        BrowserLauncher browserLauncher) {
        this.roomRepoBookmark = roomRepoBookmark;
        this.browserLauncher = browserLauncher;
        this.dataBus = dataBus;
    }

    // Стек для хранения удаленных закладок
    private Stack<MyArticle> undoStack = new Stack<>();

    @Override
    public void onFragmentConnected(BookmarkView bookmarkView) {
        disposable = connectToRepo();
        this.bookmarkView = bookmarkView;
    }

    @Override
    public void onFragmentDisconnected() {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }

        bookmarkView = null;
    }

    @Override
    public LiveData<BookmarkFragmentState> subscribe() {
        return liveData;
    }

    @Override
    public UndoStatus getUndoStatus() {
        return (undoStack.isEmpty()) ? UndoStatus.EMPTY : UndoStatus.PRESENT;
    }

    // Восстановить последнюю удаленную статью
    @Override
    public void restoreRecent() {
        if (!undoStack.isEmpty()) {
            MyArticle article = undoStack.pop();

            if (article != null) {
                article.isMarked = true;
                roomRepoBookmark.insertArticle(article);
            }
        }

        if (undoStack.isEmpty()) {
            bookmarkView.onAllRestored();
        }
    }

    // Восстановить все удаленные статьи
    @Override
    public void restoreAll() {

        while (!undoStack.isEmpty()) {
            MyArticle article = undoStack.pop();

            if (article != null) {
                article.isMarked = true;
                roomRepoBookmark.insertArticle(article);
            }
        }

        bookmarkView.onAllRestored();
    }

    // Очистить список закладок (поместить в undoStack)
    @Override
    public void deleteAll() {
        if (liveData.getValue() != null) {
            List<MyArticle> li = new ArrayList<>(liveData.getValue().getArticles());

            li.forEach( article -> {
                roomRepoBookmark.deleteArticle(article);
                undoStack.push(article);
            });
        }
    }

    private Disposable connectToRepo() {

        return dataBus
                .connectToBookmarkData()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                            if (liveData.getValue() != null) {
                                BookmarkFragmentState currentState = liveData.getValue();
                                currentState.setArticles(list);
                                liveData.setValue(currentState);
                            } else {
                                liveData.setValue(new BookmarkFragmentState(list));
                            }
                        },
                        error -> {
                            logIt("BFP: error in subscribe\n" + error.getMessage());
                        });
    }

    @Override
    public int getTabCount() {
        return 0;
    }

    @Override
    public void onClickBookmark(long articleId) {
        Pair<Integer, MyArticle> foundArticle = findArticle(articleId);

        if (foundArticle != null &&
                foundArticle.first != null &&
                foundArticle.second != null) {

            MyArticle article = foundArticle.second;
            article.isMarked = !article.isMarked;

            // Обновить базу
            if (article.isMarked) {
                roomRepoBookmark.insertArticle(article);
            } else {
                roomRepoBookmark.deleteArticle(article);
                undoStack.push(article);

                if (bookmarkView != null) {
                    bookmarkView.onBookmarkDeleted();
                }
            }
        }
    }

    @Override
    public void onClickArticle(String articleUrl) {
        browserLauncher.showInBrowser(articleUrl);
    }

    @Override
    public List<MyArticle> getTabArticles(int tabPosition) {
        return getArticles();
    }

    @Override
    public List<MyArticle> getArticles() {
        if (liveData.getValue() != null) {
            BookmarkFragmentState currentState = liveData.getValue();
            return currentState.getArticles();
        }
        return new ArrayList<>();
    }

    @Override
    public MyArticle getArticle(int listPosition) {
        if (liveData.getValue() != null) {
            BookmarkFragmentState currentState = liveData.getValue();
            if (listPosition <= currentState.getArticles().size()) {
                return currentState.getArticles().get(listPosition);
            }
        }
        return null;
    }

    @Override
    public String getHighlight() {
        return "";
    }

    // Найти статью в общем списке по её ID
    private Pair<Integer, MyArticle> findArticle(long articleId) {
        if (liveData.getValue() != null) {
            BookmarkFragmentState currentState = liveData.getValue();

            MyArticle article = currentState
                    .getArticles()
                    .stream()
                    .filter((a) -> a.id == articleId)
                    .collect(Collectors.toList())
                    .get(0);

            if (article != null) {
                return new Pair<Integer, MyArticle>(currentState
                        .getArticles().indexOf(article), article);
            }
        }
        return null;
    }
}
