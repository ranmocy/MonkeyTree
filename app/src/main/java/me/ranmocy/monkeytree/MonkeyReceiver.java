package me.ranmocy.monkeytree;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * The broadcast receiver that listen to system events.
 */
public final class MonkeyReceiver extends BroadcastReceiver {

    private static final String TAG = "MonkeyReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Receive action:" + intent.getAction());
        MonkeyService.updateJob(context);
    }
}
