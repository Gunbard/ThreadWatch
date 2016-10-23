package honkhonk.threadwatch;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * App singleton handling requests, etc.
 * Created by Gunbard on 10/11/2016.
 */

public class ThreadWatch {
    private static ThreadWatch instance = null;
    private static Context context;
    private RequestQueue requestQueue;


    private ThreadWatch(final Context context) {
        this.context = context;
        requestQueue = getRequestQueue();
    }

    public static synchronized ThreadWatch getInstance(final Context context) {
        if (instance == null) {
            instance = new ThreadWatch(context.getApplicationContext());
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
