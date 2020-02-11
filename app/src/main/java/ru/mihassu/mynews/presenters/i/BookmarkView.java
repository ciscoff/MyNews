package ru.mihassu.mynews.presenters.i;

public interface BookmarkView {
    void onBookmarkDeleted(int qty, int itemsInBasket);
    void onBookmarkRestored(int qty, int remain);
}
