package me.ranmocy.monkeytree;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * A background service listen to contact changes and update phonetic information.
 */
@TargetApi(24)
public final class MonkeyService extends JobService {

    private static final String TAG = "MonkeyService";

    private static final int JOB_ID = 1;
    // private static final Uri CONTACT_URI = ContactsContract.AUTHORITY_URI;
    private static final Uri CONTACT_DATA_URI = ContactsContract.Data.CONTENT_URI;
    private static final String[] PROJECTION = new String[]{
            ContactsContract.Data._ID,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            ContactsContract.CommonDataKinds.StructuredName.MIDDLE_NAME,
            ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME};

    static void scheduleJob(Context context) {
        if (isScheduled(context)) {
            return;
        }
        Log.i(TAG, "scheduleJob");
        ComponentName jobService = new ComponentName(context, MonkeyService.class);
        getJobScheduler(context)
                .schedule(new JobInfo.Builder(JOB_ID, jobService)
                        .setTriggerContentUpdateDelay(1000)
                        .setTriggerContentMaxDelay(30000)
                        .addTriggerContentUri(new JobInfo.TriggerContentUri(CONTACT_DATA_URI, 0))
                        .build());
    }

    static void cancelJob(Context context) {
        if (!isScheduled(context)) {
            return;
        }
        Log.i(TAG, "cancelJob");
        getJobScheduler(context).cancel(JOB_ID);
    }

    static boolean isScheduled(Context context) {
        return getScheduledJob(context) != null;
    }

    @Nullable
    static JobInfo getScheduledJob(Context context) {
        for (JobInfo jobInfo : getJobScheduler(context).getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                return jobInfo;
            }
        }
        return null;
    }

    private static JobScheduler getJobScheduler(Context context) {
        return (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
    }

    private FixingTask task;

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.i(TAG, "start");
        for (Uri uri : params.getTriggeredContentUris()) {
            Log.i(TAG, String.format("uris:%s", uri));
        }
        if (task == null || task.isCancelled()) {
            task = new FixingTask(this);
            task.execute(params);
        } else {
            Log.i(TAG, "Task is running!");
        }
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        task.cancel(true);
        task = null;
        return false;
    }

    private final class FixingTask extends AsyncTask<JobParameters, Void, Void> {

        private final Context context;
        private final ContactFixer contactFixer;

        FixingTask(Context context) {
            this.context = context;
            this.contactFixer = new ContactFixer(context);
        }

        @Override
        protected Void doInBackground(JobParameters... params) {
            if (SharedPreferencesUtil.getAutoUpdateEnabled(context)) {
                contactFixer.fixContactPhonetic(contactFixer.getAllContactsToUpdate());
            } else {
                NotificationManager nm = context.getSystemService(NotificationManager.class);
                nm.notify(R.id.contact_changed, new Notification.Builder(context)
                        .setContentTitle("Contact changed")
                        .setContentIntent(PendingIntent.getActivity(
                                context,
                                0 /*requestCode*/,
                                MainActivity.getActivityIntent(context),
                                0 /*flags*/))
                        .build());
            }
            jobFinished(params[0], false);
            return null;
        }
    }
}
