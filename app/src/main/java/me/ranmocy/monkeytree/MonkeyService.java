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

import java.util.Set;

/**
 * A background service listen to contact changes and update phonetic information.
 */
@TargetApi(24)
public final class MonkeyService extends JobService {

    private static final String TAG = "MonkeyService";

    private static final int JOB_ID = 1;
    // private static final Uri CONTACT_URI = ContactsContract.AUTHORITY_URI;
    private static final Uri CONTACT_DATA_URI = ContactsContract.Data.CONTENT_URI;

    static void updateJob(Context context) {
        boolean monitoringEnabled = SharedPreferencesUtil.getMonitoringEnabled(context);
        if (monitoringEnabled) {
            scheduleJob(context);
        } else {
            cancelJob(context);
        }
    }

    private static void scheduleJob(Context context) {
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

    private static void cancelJob(Context context) {
        if (!isScheduled(context)) {
            return;
        }
        Log.i(TAG, "cancelJob");
        getJobScheduler(context).cancel(JOB_ID);
    }

    private static boolean isScheduled(Context context) {
        return getScheduledJob(context) != null;
    }

    @Nullable
    private static JobInfo getScheduledJob(Context context) {
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
            Log.i(TAG, "doInBackground");
            Set<ContactLite> allContactsToUpdate = contactFixer.getAllContactsToUpdate();
            if (!allContactsToUpdate.isEmpty()) {
                if (SharedPreferencesUtil.getAutoUpdateEnabled(context)) {
                    contactFixer.fixContactPhonetic(allContactsToUpdate);
                } else {
                    NotificationManager nm = context.getSystemService(NotificationManager.class);
                    nm.notify(R.id.notification_id_contact_changed, new Notification.Builder(context)
                            .setContentTitle(getString(R.string.notification_title))
                            .setContentText(getString(R.string.notification_message))
                            .setSmallIcon(R.drawable.ic_group_add_black_24dp)
                            .setAutoCancel(true)
                            .setContentIntent(PendingIntent.getActivity(
                                    context,
                                    0 /*requestCode*/,
                                    MainActivity.getUpdateAllIntent(context),
                                    0 /*flags*/))
                            .build());
                }
            }

            jobFinished(params[0], false);
            Log.i(TAG, "done");

            updateJob(context);
            return null;
        }
    }
}
