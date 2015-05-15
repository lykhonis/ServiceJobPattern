package com.vl.android.servicejobpattern;

import android.app.IntentService;
import android.content.Intent;

public class JobService extends IntentService {

    static final String TAG = "JobService";

    JobManager mJobManager;

    public JobService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mJobManager = JobManager.getInstance(this);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // getting jobs here
        if (intent != null) {
            int jobId = intent.getIntExtra(JobManager.EXTRA_JOB_ID, JobManager.INVALID_JOB_ID);
            // it is okay to call, since if id is invalid we get null anyway
            JobManager.Job job = mJobManager.get(jobId);
            if (job != null) handleJob(job);
        }
    }

    void handleJob(JobManager.Job job) {
        job.run();
        mJobManager.notifyCompleted(job);
    }
}
