package honkhonk.threadwatch.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;

import honkhonk.threadwatch.helpers.Common;
import honkhonk.threadwatch.models.ThreadModel;

public class ThreadDataManager {
    final private static String TAG = ThreadDataManager.class.getSimpleName();

    public static ArrayList<ThreadModel> getThreadList(Context context) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        final String listDataAsJson = savedPrefs.getString(Common.SAVED_THREAD_DATA, null);
        if (listDataAsJson == null) {
            return new ArrayList<>();
        }

        return (new Gson()).fromJson(listDataAsJson,
                new TypeToken<ArrayList<ThreadModel>>() {}.getType());
    }

    public static void addThread(Context context, ThreadModel newThread) {
        ArrayList<ThreadModel> threadList = getThreadList(context);
        if (threadList == null) {
            threadList = new ArrayList<>();
        }

        threadList.add(newThread);

        final String listDataAsJson = (new Gson()).toJson(threadList);
        Log.d(TAG, "List data: " + listDataAsJson);

        SharedPreferences settings = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Common.SAVED_THREAD_DATA, listDataAsJson);
        editor.apply();
    }

    public static void deleteThread(Context context, String board, String id) {
        final ArrayList<ThreadModel> threadList = getThreadList(context);
        if (threadList == null) {
            Log.e(TAG, "Tried to delete thread in an empty list");
            return;
        }

        for (final ThreadModel thread : threadList) {
            if (thread.board.equals(board) && thread.id.equals(id)) {
                threadList.remove(thread);
                updateThreadList(context, threadList);
                return;
            }
        }

        Log.e(TAG, "Could not find thread to delete");
    }

    public static boolean updateThread(Context context, ThreadModel updatedThread) {
        final ArrayList<ThreadModel> threadList = getThreadList(context);
        if (threadList == null) {
            Log.e(TAG, "Tried to update thread in an empty list");
            return false;
        }

        for (final ThreadModel thread : threadList) {
            if (updatedThread.board.equals(thread.board) && updatedThread.id.equals(thread.id)) {
                threadList.remove(thread);
                threadList.add(updatedThread);
                updateThreadList(context, threadList);
                return true;
            }
        }

        Log.e(TAG, "Could not find thread to update");
        return false;
    }

    public static void updateThreadList(Context context, ArrayList<ThreadModel> newThreadList) {
        final String listDataAsJson = (new Gson()).toJson(newThreadList);
        Log.d(TAG, "List data: " + listDataAsJson);

        SharedPreferences settings = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Common.SAVED_THREAD_DATA, listDataAsJson);
        editor.apply();
    }

    public static ThreadModel getThread(Context context, String board, String id) {
        final ArrayList<ThreadModel> threadList = getThreadList(context);
        if (threadList == null)
        {
            return null;
        }

        for (final ThreadModel thread : threadList) {
            if (thread.board.equals(board) && thread.id.equals(id)) {
                return thread;
            }
        }

        return null;
    }

    public static HashMap<String, Integer> getUpdatedThreads(Context context) {
        final SharedPreferences savedPrefs = context.getSharedPreferences(Common.PREFS_NAME, 0);
        final String listDataAsJson = savedPrefs.getString(Common.SAVED_UPDATED_THREADS, null);
        if (listDataAsJson == null) {
            return new HashMap<>();
        }

        return (new Gson()).fromJson(listDataAsJson,
                new TypeToken<HashMap<String, Integer>>() {}.getType());
    }

    public static void setUpdatedThreads(Context context,
                                            HashMap<String, Integer> newUpdatedThreads) {
        final String listDataAsJson = (new Gson()).toJson(newUpdatedThreads);

        SharedPreferences settings = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Common.SAVED_UPDATED_THREADS, listDataAsJson);
        editor.apply();
    }

    public static void clearUpdatedThreads(Context context) {
        SharedPreferences settings = context.getSharedPreferences(Common.PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(Common.SAVED_UPDATED_THREADS, null);
        editor.apply();
    }
}
