package com.csipsimple.ui.incall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receiver that listens to broadcast events for starting call UI
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    private final String TAG = IncomingCallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        Intent intent1 = new Intent(context, CallActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtras(intent);
        context.startActivity(intent1);
    }
}