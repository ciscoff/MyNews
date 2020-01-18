package ru.mihassu.mynews.ui.news;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.subjects.BehaviorSubject;
import ru.mihassu.mynews.R;
import ru.mihassu.mynews.domain.entity.ArticleCategory;
import ru.mihassu.mynews.domain.model.MyArticle;
import ru.mihassu.mynews.presenters.ArticlePresenter;
import ru.mihassu.mynews.ui.Fragments.UpdateAgent;
import ru.mihassu.mynews.ui.main.MainAdapter;
import ru.mihassu.mynews.ui.web.ArticleActivity;
import ru.mihassu.mynews.ui.web.CustomTabHelper;

public class NewsViewPagerAdapter
        extends RecyclerView.Adapter<NewsViewPagerAdapter.NewsViewHolder> {

    // Списки новостей по категориям
    private EnumMap<ArticleCategory, List<MyArticle>> classifiedNews;

    // Helper для работы с Chrome CustomTabs
    private CustomTabHelper customTabHelper = new CustomTabHelper();

    private UpdateAgent updateAgent;
    private HashMap<ArticleCategory, ArticlePresenter> articlePresenters;
    private boolean isUpdateInProgress;

    public NewsViewPagerAdapter(
            UpdateAgent updateAgent,
            HashMap<ArticleCategory, ArticlePresenter> articlePresenters) {

        this.updateAgent = updateAgent;
        this.isUpdateInProgress = true;
        this.articlePresenters = articlePresenters;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.item_news, parent, false);
        return new NewsViewHolder(v);
    }

    // v 1.2
    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        if (classifiedNews != null && classifiedNews.size() != 0) {

            ArrayList<Map.Entry<ArticleCategory, List<MyArticle>>> allArticles =
                    new ArrayList<>(classifiedNews.entrySet());

            ArticlePresenter presenter = articlePresenters.get(allArticles.get(position).getKey());

            if(presenter != null) {
                presenter.setArticles(allArticles.get(position).getValue());
                holder.bind(presenter);
            }
        }
    }

    // v 1.1
//    @Override
//    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
//        if (classifiedNews != null && classifiedNews.size() != 0) {
//
//            ArrayList<Map.Entry<ArticleCategory, List<MyArticle>>> allArticles =
//                    new ArrayList<>(classifiedNews.entrySet());
//            holder.bind(
//                    allArticles.get(position).getValue(),
//                    articlePresenters.get(allArticles.get(position).getKey()));
//        }
//    }

//      v1.0
//    @Override
//    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
//        if (classifiedNews != null && classifiedNews.size() != 0) {
//            ArticleCategory[] actualCategories = new ArticleCategory[classifiedNews.keySet().size()];
//            classifiedNews.keySet().toArray(actualCategories);
//            holder.bind(
//                    classifiedNews.get(actualCategories[position]),
//                    articlePresenters.get(actualCategories[position]));
//        }
//    }

    @Override
    public int getItemCount() {
        return classifiedNews != null ? classifiedNews.size() : 0;
    }

    // Вызывается из MainFragment при обновлении новостей
    public void updateContent(EnumMap<ArticleCategory, List<MyArticle>> enumMap) {
        classifiedNews = enumMap;
        notifyDataSetChanged();
        isUpdateInProgress = false;
    }

    /**
     * Holder отдельной ViewGroup внутри ViewPager2
     */
    class NewsViewHolder extends RecyclerView.ViewHolder {

        private RecyclerView rv;
        private SwipeRefreshLayout swipeRefreshLayout;
        private BehaviorSubject<Integer> scrollEventsRelay;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);

            scrollEventsRelay = BehaviorSubject.create();
            rv = itemView.findViewById(R.id.news_recyclerview);
            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    if (dy != 0) {
                        scrollEventsRelay.onNext(dy);
                    }
                }
            });

            initSwipeRefreshLayout();
        }

        void bind(ArticlePresenter articlePresenter) {
            MainAdapter adapter = new MainAdapter(
                    scrollEventsRelay.hide(),
                    this::showInChromeCustomTabs,
                    articlePresenter);
            rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            rv.setAdapter(adapter);
            swipeRefreshLayout.setRefreshing(false);
        }

//        void bind(List<MyArticle> articles, ArticlePresenter articlePresenter) {
//            MainAdapter adapter = new MainAdapter(
//                    scrollEventsRelay.hide(),
//                    this::showInChromeCustomTabs,
//                    articlePresenter);
//
//            adapter.setDataList(articles);
//            rv.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
//            rv.setAdapter(adapter);
//            swipeRefreshLayout.setRefreshing(false);
//        }

        // Настроить работу SwipeRefreshLayout
        private void initSwipeRefreshLayout() {
            swipeRefreshLayout = itemView.findViewById(R.id.swipe_refresh);
            swipeRefreshLayout.setColorSchemeResources(
                    android.R.color.holo_blue_bright,
                    android.R.color.holo_green_light,
                    android.R.color.holo_orange_light,
                    android.R.color.holo_red_light
            );

            // Запросить обновление при Swipe
            swipeRefreshLayout.setOnRefreshListener(() -> {

                        if (!isUpdateInProgress) {
                            isUpdateInProgress = true;
                            swipeRefreshLayout.setRefreshing(true);
                            updateAgent.update();
                        }
                    }
            );

            swipeRefreshLayout.setRefreshing(isUpdateInProgress);
        }

        // Отобразить новость в Chrome CustomTabs
        private void showInChromeCustomTabs(String link) {

            int requestCode = 100;

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();

            builder.setToolbarColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
            builder.addDefaultShareMenuItem();
            builder.setStartAnimations(itemView.getContext(), android.R.anim.fade_in, android.R.anim.fade_out);
            builder.setExitAnimations(itemView.getContext(), android.R.anim.fade_in, android.R.anim.fade_out);
            builder.setShowTitle(true);

            CustomTabsIntent anotherCustomTab = new CustomTabsIntent.Builder().build();

            Intent intent = anotherCustomTab.intent;
            intent.setData(Uri.parse(link));
            PendingIntent pendingIntent = PendingIntent.getActivity(itemView.getContext(), requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addMenuItem("Our custom menu", pendingIntent);

            CustomTabsIntent customTabsIntent = builder.build();

            String packageName = customTabHelper.getPackageNameToUse(itemView.getContext(), link);

            if (packageName != null) {
                customTabsIntent.intent.setPackage(packageName);
                customTabsIntent.launchUrl(itemView.getContext(), Uri.parse(link));
            } else {
                Intent intentOpenUri = new Intent(itemView.getContext(), ArticleActivity.class);
                intentOpenUri.putExtra(itemView.getResources().getString(R.string.article_url_key), link);
                itemView.getContext().startActivity(intentOpenUri);
            }
        }
    }
}
