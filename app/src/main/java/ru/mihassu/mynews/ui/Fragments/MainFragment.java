package ru.mihassu.mynews.ui.Fragments;

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
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import ru.mihassu.mynews.App;
import ru.mihassu.mynews.R;
import ru.mihassu.mynews.di.modules.ui.MainFragmentModule;
import ru.mihassu.mynews.domain.entity.ArticleCategory;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.domain.repository.ChannelCollector;
import ru.mihassu.mynews.presenters.ArticlePresenter;
import ru.mihassu.mynews.presenters.MainFragmentPresenter;
import ru.mihassu.mynews.ui.news.NewsViewPagerAdapter;

public class MainFragment extends Fragment implements Observer {

    private NewsViewPagerAdapter viewPagerAdapter;
    private ViewPager2 viewPager;
    private MainFragmentState currentState;
    private AnimatedVectorDrawableCompat animatedProgressBar;
    private ImageView progressBarImage;
    private ConstraintLayout progressBarContainer;

    @Inject
    Context context;

    @Inject
    ChannelCollector collector;

    @Inject
    HashMap<ArticleCategory, ArticlePresenter> articlePresenters;

    @Inject
    MainFragmentPresenter fragmentPresenter;

    // 1.
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        App
                .get()
                .getAppComponent()
                .plusMainFragmentComponent(new MainFragmentModule())
                .inject(this);
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
            viewPagerAdapter.updateContent(currentState.getCurrentSortedArticles());
        }
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
        viewPagerAdapter.updateContent(currentState.getCurrentSortedArticles());
    }

    // Init ViewPager
    private void initViewPager(View fragmentView) {
        viewPagerAdapter = new NewsViewPagerAdapter(this::updateAgentImpl, articlePresenters);
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
                    tab.setText(context.getString(
                            currentState
                                    .getCurrentCategories()[position]
                                    .getTextId()));
                }
        );
        mediator.attach();
    }

    /**
     * Запустить кастомный ProgressBar
     */
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

    // UpdateAgent::update()
    private void updateAgentImpl() {
        collector.updateChannels();
    }

    /**
     * Запускаем процесс получения данных
     * На выходе получаем списки статей упорядоченные по категориям в HashMap'е
     */
    @SuppressWarnings("unchecked")
    private void loadChannels() {
        fragmentPresenter.subscribe().observe(this,
                this);
//
//        collector.collectChannels()
//                .observe(this,
//                        this);
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
                if (searchedList.size() > 0) {
                    currentState.setCurrentArticles(searchedList);
                    viewPager.setCurrentItem(0);
                    viewPagerAdapter.updateContent(currentState.getCurrentSortedArticles());
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
}
