package ru.mihassu.mynews.presenters.i;

public interface BookmarkView {
    void onBookmarkDeleted(int qty);
    void onBookmarkRestored(int qty, int remain);
}
