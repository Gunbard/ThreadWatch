package honkhonk.threadwatch.retrievers;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

import honkhonk.threadwatch.R;
import honkhonk.threadwatch.models.PostModel;
import honkhonk.threadwatch.models.ThreadModel;

/**
 * Retrieves data for a thread
 * Created by Gunbard on 10/13/2016.
 */

public class ThreadsRetriever implements PostsRetriever.PostsRetrieverListener {
    final private static String TAG = ThreadsRetriever.class.getSimpleName();

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
        Log.d(TAG, "Starting thread retrieval");

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
        Log.d(TAG, "Posts retrieved successfully");

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
        thread.firstRefresh = false;
        thread.latestTime = latest.time;
        thread.lastPostId = latest.number;
        thread.newRepliesToYou = thread.newRepliesToYou || hasNewRepliesToYou(thread, posts);

        retrieveReplyComments(context, thread, posts);

        retrievedThreads.add(thread);
        processThreadQueue(context);
    }

    /**
     * Retrieval failed
     * @param context Context of the retrieval
     * @param thread The thread the posts were supposed to be retrieved from
     */
    public void postsRetrievalFailed(final Context context, final ThreadModel thread) {
        Log.d(TAG, "Posts failed to retrieve");

        failureOccurred = !thread.disabled;
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

            if (threadToGet.disabled) {
                postsRetrievalFailed(context, threadToGet);
            } else {
                PostsRetriever postsRetriever = new PostsRetriever();
                postsRetriever.addListener(this);
                postsRetriever.retrievePosts(context, threadToGet);
            }
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

    /**
     * Checks the latest posts to see if any of the watched replies were referenced
     * @param thread The thread to check
     * @param posts The posts referenced
     * @return True if a replyId was quoted, false otherwise
     */
    private boolean hasNewRepliesToYou(ThreadModel thread, final ArrayList<PostModel> posts) {
        if (thread.replyIds.isEmpty()) {
            return false;
        }

        for (int i = (posts.size() - thread.replyCountDelta); i < posts.size(); i++) {
            PostModel post = posts.get(i);
            for (String replyId : thread.replyIds.keySet()) {
                if (post.comment != null && post.comment.contains(replyId)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Regenerates the replyIds hash to populate each replyId to contain the comments that
     * reference it.
     * @param context Context for the retrieval
     * @param thread The thread to check
     * @param posts The posts in the thread
     */
    private void retrieveReplyComments(Context context, ThreadModel thread, final ArrayList<PostModel> posts) {
        if (thread.replyIds.isEmpty()) {
            return;
        }

        // Clear existing replyIds values
        for (String replyId : thread.replyIds.keySet()) {
            ArrayList<PostModel> replyComments = thread.replyIds.get(replyId);
            replyComments.clear();
        }

        // Check each post comment against the tracked reply ids and add if the id is referenced
        for (PostModel post : posts) {
            for (String replyId : thread.replyIds.keySet()) {
                ArrayList<PostModel> replyComments = thread.replyIds.get(replyId);

                // Ensure original post is first
                if (Integer.toString(post.number).equals(replyId)) {
                    replyComments.add(0, post);
                }

                if (post.comment != null && post.comment.contains(replyId)) {
                    // Add the comment to the replyId hash
                    replyComments.add(post);
                }
            }
        }

        // Mark posts that weren't found
        for (String replyId : thread.replyIds.keySet()) {
            ArrayList<PostModel> replyComments = thread.replyIds.get(replyId);

            int postNumber = 0;
            try {
                postNumber = Integer.parseInt(replyId);
            } catch (NumberFormatException e) {
                Log.i(TAG, String.format("Reply Id (%s) was not a post number!", replyId));
            }

            if (postNumber != 0 && replyComments == null || replyComments.size() == 0) {
                PostModel failedPost = new PostModel();
                failedPost.number = postNumber;
                failedPost.comment = context.getString(R.string.reply_not_found);
                failedPost.failed = true;

                ArrayList<PostModel> emptyReplies = new ArrayList<>();
                emptyReplies.add(failedPost);
                thread.replyIds.put(replyId, emptyReplies);
            }
        }
    }
}
