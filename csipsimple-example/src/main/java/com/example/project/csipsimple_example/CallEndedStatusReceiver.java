package com.example.project.csipsimple_example;

/**
 * Created by kadyrovs on 18.01.2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;

/**
 * Receiver that listens to broadcast events for starting call UI
 */
public class CallEndedStatusReceiver extends BroadcastReceiver {
    private final String TAG = CallEndedStatusReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        boolean callEnded = intent.getBooleanExtra(ApiConstants.CALL_ENDED_STATUS_INTENT_KEY, false);
        if(callEnded){
            Toast.makeText(context, "Call ended", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Call ended");
        } else {
            Log.e(TAG, "status is null or empty!");
        }

    }
}