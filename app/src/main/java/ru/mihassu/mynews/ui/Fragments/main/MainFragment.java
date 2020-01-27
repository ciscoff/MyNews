package ru.mihassu.mynews.ui.Fragments.main;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.vectordrawable.graphics.drawable.Animatable2Compat;
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import ru.mihassu.mynews.App;
import ru.mihassu.mynews.R;
import ru.mihassu.mynews.di.modules.ui.MainFragmentModule;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.presenters.i.ArticlePresenter;
import ru.mihassu.mynews.presenters.i.MainFragmentPresenter;

public class MainFragment extends Fragment implements Observer, UpdateAgent {

    private ViewPager2 viewPager;
    private ImageView progressBarImage;
    private MainFragmentState currentState;
    private NewsViewPagerAdapter viewPagerAdapter;
    private ConstraintLayout progressBarContainer;
    private AnimatedVectorDrawableCompat animatedProgressBar;

    @Inject
    Context context;

    @Inject
    MainFragmentPresenter fragmentPresenter;

    @Inject
    ArticlePresenter articlePresenter;

    @Inject
    @Named("search_result_publisher")
    BehaviorSubject<List<MyArticle>> searchResultPublisher;

    // 1.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        App
                .get()
                .getAppComponent()
                .plusMainFragmentComponent(new MainFragmentModule(this))
                .inject(this);

        fragmentPresenter.onFragmentConnected(searchResultPublisher.hide());
    }

    // 2.
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View viewFragment = inflater.inflate(R.layout.fragment_main, container, false);

        progressBarContainer = viewFragment.findViewById(R.id.pb_container);
        progressBarImage = progressBarContainer.findViewById(R.id.iv_moving_points);

        initViewPager(viewFragment);
        setHasOptionsMenu(true);
        return viewFragment;
    }

    // 3. Tabs
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initTabLayout(view);
    }

    // 4.
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadChannels();

        if (currentState == null) {
            initProgressBar();
        } else {
            // Убрать ProgressBar и показать новости
            hideProgressBar();
            viewPagerAdapter.updateContent();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewPagerAdapter.updateContent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fragmentPresenter.onFragmentDisconnected();
    }

    /**
     * LiveData Observer Implementation
     */
    @Override
    @SuppressWarnings("unchecked")
    public void onChanged(Object obj) {
        currentState = (MainFragmentState) obj;

        // Убрать ProgressBar, показать новости
        hideProgressBar();
        viewPagerAdapter.updateContent();
    }

    // Init ViewPager
    private void initViewPager(View fragmentView) {
        viewPagerAdapter =
                new NewsViewPagerAdapter(this, articlePresenter);

        viewPager = fragmentView.findViewById(R.id.news_viewpager);
        viewPager.setAdapter(viewPagerAdapter);
    }

    /**
     * Порядок Tab'ов соответствует порядку элементов в ArticleCategory[]
     */
    private void initTabLayout(@NonNull View fragment) {
        TabLayout tabLayout = fragment.findViewById(R.id.news_tabs);
        TabLayoutMediator mediator = new TabLayoutMediator(
                tabLayout,
                viewPager,
                (tab, position) -> {
                    if (currentState != null) {
                        tab.setText(context.getString(
                                currentState
                                        .getCurrentCategories()[position]
                                        .getTextId()));
                    }
                }
        );
        mediator.attach();
    }

    // Запустить кастомный ProgressBar
    private void initProgressBar() {
        animatedProgressBar =
                AnimatedVectorDrawableCompat.create(context, R.drawable.avd_moving_points);
        progressBarImage.setImageDrawable(animatedProgressBar);

        if (animatedProgressBar != null) {
            animatedProgressBar.registerAnimationCallback(new Animatable2Compat.AnimationCallback() {
                @Override
                public void onAnimationEnd(Drawable drawable) {
                    progressBarImage.post(animatedProgressBar::start);
                }
            });
            animatedProgressBar.start();
        }
    }

    // Убрать ProgressBar
    private void hideProgressBar() {
        progressBarContainer.setVisibility(View.INVISIBLE);
        if (animatedProgressBar != null) {
            animatedProgressBar.stop();
        }
    }

    @Override
    public void launchUpdate() {
        fragmentPresenter.updateChannels();
    }

    /**
     * Запускаем процесс получения данных
     * На выходе имеем списки статей упорядоченные по категориям в HashMap'е
     */
    @SuppressWarnings("unchecked")
    private void loadChannels() {
        fragmentPresenter
                .subscribe()
                .observe(this, this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        inflater.inflate(R.menu.menu_search, menu);
        MenuItem search = menu.findItem(R.id.action_search);

        SearchView searchView = (SearchView) search.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                String text = s.toLowerCase();
                List<MyArticle> searchedList = new ArrayList<>();
                List<MyArticle> currentList = currentState.getCurrentArticles();

                for (MyArticle article : currentList) {
                    String title = article.title.toLowerCase();
                    if (title.contains(text)) {
                        searchedList.add(article);
                    }
                }

                // Если поиск успешный, то объявить результаты подписчикам (MainFragmentPresenter)
                if (searchedList.size() > 0) {
                    // Без этого поиск глючил, т.к. запрос помещался в RX на основном потоке
                    executeInSeparateThread(() -> searchResultPublisher.onNext(searchedList));
                } else {
                    Toast.makeText(getActivity(), getString(R.string.not_found), Toast.LENGTH_SHORT).show();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

    private void executeInSeparateThread(Runnable runnable) {
        Completable.fromRunnable(runnable)
                .subscribeOn(Schedulers.io())
                .subscribe();
    }
}
