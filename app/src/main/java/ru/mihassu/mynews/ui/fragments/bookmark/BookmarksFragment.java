package ru.mihassu.mynews.ui.fragments.bookmark;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import javax.inject.Inject;

import ru.mihassu.mynews.App;
import ru.mihassu.mynews.R;
import ru.mihassu.mynews.di.modules.ui.BookmarkFragmentModule;
import ru.mihassu.mynews.domain.entity.UndoStatus;
import ru.mihassu.mynews.presenters.i.BookmarkFragmentPresenter;
import ru.mihassu.mynews.presenters.i.BookmarkView;

public class BookmarksFragment extends Fragment implements BookmarkView, Observer {

    @Inject
    Context context;

    @Inject
    BookmarkAdapter adapter;

    @Inject
    BookmarkFragmentPresenter bookmarkPresenter;

    private Menu menu;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        App
                .get()
                .getAppComponent()
                .plusBookmarkFragmentComponent(new BookmarkFragmentModule(this))
                .inject(this);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View viewFragment = inflater.inflate(
                R.layout.fragment_bookmark_with_bottomsheet,
                container,
                false);
        bookmarkPresenter.onFragmentConnected(this);
        this.setHasOptionsMenu(true);

        initBottomSheetMenu(viewFragment);

        return viewFragment;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        RecyclerView rv = view.findViewById(R.id.bookmarks_list);
        initRecyclerView(rv);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        bookmarkPresenter.onFragmentDisconnected();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();

        inflater.inflate(R.menu.menu_bookmark, menu);
        menu.findItem(R.id.menu_undo).setVisible(bookmarkPresenter.getUndoStatus() != UndoStatus.EMPTY);
        this.menu = menu;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_undo:
                bookmarkPresenter.restoreRecent();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onChanged(Object o) {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBookmarkDeleted() {
        menu.findItem(R.id.menu_undo).setVisible(true);
    }

    @Override
    public void onAllRestored() {
        menu.findItem(R.id.menu_undo).setVisible(false);
    }

    private void initBottomSheetMenu(View viewFragment) {
        BottomSheetBehavior bsb =
                BottomSheetBehavior.from(viewFragment.findViewById(R.id.bookmarks_bottomSheet));
        ImageView ivArrow = viewFragment.findViewById(R.id.iv_arrow);

        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                animateBottomSheetArrows(ivArrow, slideOffset);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void initRecyclerView(RecyclerView rv) {
        bookmarkPresenter.subscribe().observe(getViewLifecycleOwner(), this);

        rv.setLayoutManager(new LinearLayoutManager(context));
        rv.setHasFixedSize(false);
        rv.setAdapter(adapter);

        // Добавить поддержку удаления элементов через swipe
        ItemTouchHelper.Callback callback =
                new ItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(rv);
    }

    // Поворот стрелки при движении BottomSheet
    private void animateBottomSheetArrows(ImageView imgArrow, float slideOffset) {
        imgArrow.setRotation(slideOffset * -180f);
    }
}
