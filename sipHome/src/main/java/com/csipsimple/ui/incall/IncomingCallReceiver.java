package com.csipsimple.ui.incall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;

/**
 * Receiver that listens to broadcast events for starting call UI
 */
public class IncomingCallReceiver extends BroadcastReceiver {
    private final String TAG = IncomingCallReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        notifyClientApps(context);

        Intent intent1 = new Intent(context, CallActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtras(intent);
        context.startActivity(intent1);
    }

    private void notifyClientApps(Context context){
        Log.i(TAG, "About to start/receive a phone call, sneding a broadcast request about it");
        Intent intentVersion = new Intent(ApiConstants.API_RESPONSE_BROADCAST_ACTION);
        intentVersion.putExtra(ApiConstants.API_RESPONSE_TYPE_INTENT_KEY, ApiConstants.API_RESPONSE_TYPE_STARTING_CALL);
        context.sendBroadcast(intentVersion);
    }
}