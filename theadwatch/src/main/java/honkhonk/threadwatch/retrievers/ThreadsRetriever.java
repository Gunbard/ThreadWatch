package honkhonk.threadwatch.retrievers;

import android.content.Context;

import java.util.ArrayList;

import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadsRetriever implements PostsRetriever.PostsRetrieverListener {
    private ArrayList<ThreadRetrieverListener> listeners = new ArrayList<>();
    private ArrayList<ThreadModel> threadsToRetrieve = new ArrayList<>();
    private ArrayList<ThreadModel> retrievedThreads = new ArrayList<>();
    private boolean failureOccurred = false;

    public interface ThreadRetrieverListener {
        void threadsRetrieved(final ArrayList<ThreadModel> threads);
        void threadRetrievalFailed(final ArrayList<ThreadModel> threads);
    }

    public void addListener(final ThreadRetrieverListener listener) {
        listeners.add(listener);
    }

    public void retrieveThreadData(final Context context, final ArrayList<ThreadModel> threads) {
        threadsToRetrieve.addAll(threads);
        processThreadQueue(context);
    }

    /**
     * PostsRetriever.PostsRetrieverListener
     */
    public void postsRetrieved(final Context context,
                               final ThreadModel thread,
                               final ArrayList<PostModel> posts) {
        // First post should always be OP
        final PostModel op = posts.get(0);
        final PostModel latest = posts.get(posts.size() - 1);
        thread.name = op.name;
        thread.comment = op.comment;
        thread.subject = op.subject;
        thread.time = op.time;
        thread.replyCountDelta = op.replyCount - thread.replyCount;
        thread.replyCount = op.replyCount;
        thread.imageCount = op.imageCount;
        thread.archived = (op.archived == 1);
        thread.closed = (op.closed == 1);
        thread.latestTime = latest.time;

        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    public void postsRetrievalFailed(final Context context, final ThreadModel thread) {
        thread.notFound = true;
        failureOccurred = true;
        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    private void processThreadQueue(final Context context) {
        if (threadsToRetrieve.size() > 0) {
            final ThreadModel threadToGet = threadsToRetrieve.remove(0);

            PostsRetriever postsRetriever = new PostsRetriever();
            postsRetriever.addListener(this);
            postsRetriever.retrievePosts(context, threadToGet);
        } else {
            finishedRetrieving();
        }
    }

    private void finishedRetrieving() {
        for (final ThreadRetrieverListener listener : listeners) {
            if (failureOccurred) {
                listener.threadRetrievalFailed(retrievedThreads);
            } else {
                listener.threadsRetrieved(retrievedThreads);
            }
        }
    }
}
