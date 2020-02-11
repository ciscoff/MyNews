package ru.mihassu.mynews.ui.fragments.bookmark;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;

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

    private View fragment;
    private CoordinatorLayout coordinatorLayout;

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
        this.fragment = viewFragment;
        this.coordinatorLayout = viewFragment.findViewById(R.id.cl_snackbar_parent);

        initBottomSheetMenu(viewFragment, bookmarkPresenter.getUndoCount());

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
    public void onChanged(Object o) {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onBookmarkRestored(int qty, int remain) {

        if (remain > 0) {
            moveUpBottomSheetMenu(fragment);
        } else {
            moveDownBottomSheetMenu(fragment);
        }

        if (qty > 0) {
            String message = new StringBuilder(context.getString(R.string.message_restored))
                    .append("  ")
                    .append(qty)
                    .toString();
            showSnackBar(message);
        }

        updateBadgeView(fragment.findViewById(R.id.tv_badge), remain);
    }

    @Override
    public void onBookmarkDeleted(int qty, int itemsInBasket) {

        // Если что-то удалили, значит undoStack не пустой, значит подсвечиваем крышку BottomSheet
        moveUpBottomSheetMenu(fragment);

        if (qty > 1) {
            String message = new StringBuilder(context.getString(R.string.message_deleted))
                    .append("  ")
                    .append(qty)
                    .toString();
            showSnackBar(message);
        }

        updateBadgeView(fragment.findViewById(R.id.tv_badge), itemsInBasket);
    }

    private void initBottomSheetMenu(View viewFragment, int undoQty) {
        BottomSheetBehavior bsb =
                BottomSheetBehavior.from(viewFragment.findViewById(R.id.bookmarks_bottomSheet));

        // Перехватывать касания. Иначе они "пролетают" на нижний слой
        bsb.setDraggable(true);

        ImageView ivArrow = viewFragment.findViewById(R.id.iv_arrow);

        viewFragment.findViewById(R.id.cl_undo_recent).setOnClickListener(v ->
                bookmarkPresenter.restoreRecent()
        );

        viewFragment.findViewById(R.id.cl_undo_all).setOnClickListener(v ->
                bookmarkPresenter.restoreAll()
        );

        viewFragment.findViewById(R.id.cl_delete_all).setOnClickListener(v ->
                bookmarkPresenter.deleteAll()
        );

        bsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
                animateBottomSheetArrows(ivArrow, slideOffset);
            }
        });

        updateBadgeView(viewFragment.findViewById(R.id.tv_badge), undoQty);

        if(undoQty != 0) {
            moveUpBottomSheetMenu(viewFragment);
        }
    }

    // Показать "крышку" BottomSheet
    private void moveUpBottomSheetMenu(View viewFragment) {
        BottomSheetBehavior bsb =
                BottomSheetBehavior.from(viewFragment.findViewById(R.id.bookmarks_bottomSheet));
        bsb.setPeekHeight(getResources().getDimensionPixelSize(R.dimen.peek_height), true);
    }

    // Спрятать BottomSheet
    private void moveDownBottomSheetMenu(View viewFragment) {
        BottomSheetBehavior bsb =
                BottomSheetBehavior.from(viewFragment.findViewById(R.id.bookmarks_bottomSheet));
        bsb.setPeekHeight(0, true);
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

    // Показать SnackBar
    private void showSnackBar(String message) {

        Snackbar snackbar = Snackbar
                .make(coordinatorLayout, message, Snackbar.LENGTH_SHORT)
                .setTextColor(Color.WHITE)
                .setBackgroundTint(ContextCompat.getColor(context, R.color.colorPrimary));

        final Snackbar.SnackbarLayout snackBarLayout = (Snackbar.SnackbarLayout) snackbar.getView();

        for (int i = 0; i < snackBarLayout.getChildCount(); i++) {
            View parent = snackBarLayout.getChildAt(i);
            if (parent instanceof LinearLayout) {
                parent.setRotation(180);
                break;
            }
        }

        snackbar.show();
    }

    private void updateBadgeView(View view, int qty) {

        if(qty == 0) {
            view.setVisibility(View.INVISIBLE);
        } else {
            ((TextView)view).setText(Integer.toString(qty));
            view.setVisibility(View.VISIBLE);
        }
    }
}
