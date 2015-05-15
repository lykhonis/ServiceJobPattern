package com.vl.android.servicejobpattern;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.Toast;

public class JobActivity extends Activity {

    static final String TAG = "JobActivity";

    JobManager mJobManager;
    JobManager.Job mJob;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mJobManager = JobManager.getInstance(this);

        // restore Job
        if (savedInstanceState != null) {
            mJob = mJobManager.get(savedInstanceState.getInt("job.id", JobManager.INVALID_JOB_ID));
        }

        if (mJob == null) {
            // create job
            mJob = new JobManager.Job("Tell Android Hi!");
            mJobManager.post(mJob);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("job.id", mJob.getId());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Subscribe to the job while UI is active
        mJobManager.addListener(mJob.getId(), mJobListener);
        // Job may already finished
        if (mJob.isCompleted()) {
            notifyJobCompleted(mJob);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unsubscribe from the job while UI is not active
        mJobManager.removeListener(mJob.getId(), mJobListener);
    }

    void notifyJobCompleted(JobManager.Job job) {
        // Remove job if no need anymore
        mJobManager.remove(job.getId());
        Toast.makeText(JobActivity.this, "Job completed " + job.getName(), Toast.LENGTH_LONG)
             .show();
    }

    JobManager.JobListener mJobListener = new JobManager.JobListener() {
        @Override
        public void onJobCompleted(JobManager manager, JobManager.Job job) {
            notifyJobCompleted(job);
        }
    };
}
