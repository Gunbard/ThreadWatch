package honkhonk.threadwatch.retrievers;

import android.content.Context;

import java.util.ArrayList;

import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Retrieves data for a thread
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadsRetriever implements PostsRetriever.PostsRetrieverListener {
    /**
     * List of listeners to notify about retrieval events
     */
    private ArrayList<ThreadRetrieverListener> listeners = new ArrayList<>();

    /**
     * List of threads that need to be retrieved
     */
    private ArrayList<ThreadModel> threadsToRetrieve = new ArrayList<>();

    /**
     * List of threads that have been processed
     */
    private ArrayList<ThreadModel> retrievedThreads = new ArrayList<>();

    /**
     * Whether or not a thread or threads could not have been retrieved
     */
    private boolean failureOccurred = false;

    /**
     * Thread retrieval events
     */
    public interface ThreadRetrieverListener {
        /**
         * Threads were retrieved successfully
         * @param threads List of successfully retrieved threads
         */
        void threadsRetrieved(final ArrayList<ThreadModel> threads);

        /**
         * Threads couldn't be retrieved
         * @param threads List of threads that failed to retrieve
         */
        void threadRetrievalFailed(final ArrayList<ThreadModel> threads);
    }

    /**
     * Adds a listener to notify about retrieval events
     * @param listener The listener to add
     */
    public void addListener(final ThreadRetrieverListener listener) {
        listeners.add(listener);
    }

    /**
     * Makes a bunch of requests to get thread data
     * @param context Context for the retrieval
     * @param threads List of threads to retrieve
     */
    public void retrieveThreadData(final Context context, final ArrayList<ThreadModel> threads) {
        threadsToRetrieve.addAll(threads);
        processThreadQueue(context);
    }

    /****************************************
     * PostsRetriever.PostsRetrieverListener
     ****************************************/

    /**
     * The retrieval was successful
     * @param context Context of the retrieval
     * @param thread The thread the posts were retrieved from
     * @param posts The lists of posts that were retrieved
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
        thread.newReplyCount = op.replyCount - thread.replyCount;
        thread.replyCountDelta += (thread.firstRefresh) ? 0 : thread.newReplyCount;
        thread.replyCount = op.replyCount;
        thread.imageCount = op.imageCount;
        thread.archived = (op.archived == 1);
        thread.closed = (op.closed == 1);
        thread.notFound = false;
        thread.firstRefresh = false;
        thread.latestTime = latest.time;
        thread.lastPostId = latest.number;

        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    /**
     * Retrieval failed
     * @param context Context of the retrieval
     * @param thread The thread the posts were supposed to be retrieved from
     */
    public void postsRetrievalFailed(final Context context, final ThreadModel thread) {
        thread.notFound = true;
        failureOccurred = true;
        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    /**
     * Make another request to get a thread's data
     * @param context Context for the request
     */
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

    /**
     * No more threads to retrieve, so notify listeners
     */
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
