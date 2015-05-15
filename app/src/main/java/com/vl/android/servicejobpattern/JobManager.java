package com.vl.android.servicejobpattern;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;

import java.util.LinkedHashSet;

public class JobManager {

    static final int MAX_JOB_ID = 10000;
    public static final int INVALID_JOB_ID = -1;

    static final String EXTRA_JOB_ID = "job.id";

    static JobManager sInstance;

    Context mContext;
    volatile int mCurrentJobId;
    final SparseArray<Job> mJobs;
    final SparseArray<LinkedHashSet<JobListener>> mListeners;
    Handler mHandler;

    // Singletone, all thread safe of course
    public static JobManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (JobManager.class) {
                if (sInstance == null) {
                    sInstance = new JobManager(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private JobManager(Context context) {
        mContext = context;
        mJobs = new SparseArray<>();
        mListeners = new SparseArray<>();
        mHandler = new Handler(Looper.getMainLooper());
    }

    // Magic number to reduce overhead of passing objects through Intent as well as keeping
    // references to same object across all heap of the app
    private int createJobId() {
        return mCurrentJobId++ >= MAX_JOB_ID ? mCurrentJobId = 0 : mCurrentJobId;
    }

    // Getting reference object back from the heap, no cloning and other...
    public Job get(int id) {
        synchronized (mJobs) {
            return mJobs.get(id);
        }
    }

    // Removing reference if there is one
    public Job remove(int id) {
        synchronized (mJobs) {
            Job job = mJobs.get(id);
            if (job != null) {
                mJobs.remove(id);
                mListeners.remove(id);
            }
            return job;
        }
    }

    // Subscriber pattern to add/remove listener anytime
    public void addListener(int id, JobListener listener) {
        synchronized (mListeners) {
            LinkedHashSet<JobListener> listeners = mListeners.get(id);
            if (listeners == null) {
                mListeners.put(id, listeners = new LinkedHashSet<>());
            }
            // LinkedHashSet is used to ignore duplicates of listeners
            listeners.add(listener);
        }
    }

    // Subscriber pattern to add/remove listener anytime
    public void removeListener(int id, JobListener listener) {
        synchronized (mListeners) {
            LinkedHashSet<JobListener> listeners = mListeners.get(id);
            if (listeners != null) listeners.remove(listener);
        }
    }

    public void notifyCompleted(final Job job) {
        // always notify on UI thread
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mListeners) {
                    LinkedHashSet<JobListener> listeners = mListeners.get(job.mId);
                    if (listeners != null) {
                        // Hopefully no one will removeListener while we here...
                        for (JobListener listener : listeners) {
                            listener.onJobCompleted(JobManager.this, job);
                        }
                    }
                }
            }
        });
    }

    // Post some type of job, basically register and direct it to Service
    public int post(Job job) {
        synchronized (mJobs) {
            mJobs.put(job.mId = createJobId(), job);
            Intent service = new Intent(mContext, JobService.class);
            service.putExtra(EXTRA_JOB_ID, job.mId);
            mContext.startService(service);
            return job.mId;
        }
    }

    public static class Job implements Runnable {

        // Some common made up fields
        String mName;
        int mId;
        volatile boolean mCompleted;

        public Job(String name) {
            mName = name;
        }

        public int getId() {
            return mId;
        }

        public String getName() {
            return mName;
        }

        // In a case of UI was not listening while job was completed
        public boolean isCompleted() {
            return mCompleted;
        }

        // Override for other type of jobs to do the job
        // this is executed in non-UI thread
        @Override
        public void run() {
            try {
                // Doing something...
                Thread.sleep(5000l);
            } catch (InterruptedException ignore) {
            }
            mCompleted = true;
        }
    }

    public interface JobListener {
        void onJobCompleted(JobManager manager, Job job);
    }
}
