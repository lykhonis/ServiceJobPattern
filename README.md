# Service Job Pattern
One of the most common questions in Android dev world is: How to communicate between Activity and Service. Problem here is that starting service is simple, but returning result can be a challenge. Most common solutions are ResultReceiver, Messenger, BroadcastReceiver (LocalBroadcastManager) and others. They all will work just fine, but they all are IPC/RPC, they all are used to communicate across different applications. What to do in a case of single app?

## Single App Case
Since Activity and Service are going to exist in same application, it means they share same memory heap. Connecting together Activity and Service directly is good, but not scalable in a case of multiple activities and single service. To solve this problem, pattern above provides middleware, a manager to represent specific type of activity (read as action) in the application. The manager is singletone to keep track of listeners as well as jobs to do. This allows to completly detach UI (activity, fragments and etc.) from actual job in background (service).

#### Service
```java
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
```

#### Activity
```java
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
```
