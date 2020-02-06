package ru.mihassu.mynews;

import android.content.Context;
import android.util.Log;

public class Utils {
    public static void logIt(String message) {
        String tag = "APP_TAG";
        Log.d(tag, message);
    }

    public static float pxToDp(final Context context, final float px) {
        return px / context.getResources().getDisplayMetrics().density;
    }

    public static int dpToPx(final Context context, final float dp) {
        return (int)(dp * context.getResources().getDisplayMetrics().density);
    }
}


