package com.csipsimple.api;

/**
 * Created by kadyrovs on 18.01.2016.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.csipsimple.ui.account.AccountsEditList;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;

/**
 * Receiver that listens to broadcast events for starting call UI
 */
public class ApiRequestReceiver extends BroadcastReceiver {
    private final String TAG = ApiRequestReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        int requestType = intent.getIntExtra(ApiConstants.REQUEST_TYPE_INTENT_KEY, -1);

        if(requestType == ApiConstants.REQUEST_TYPE_MAKE_CALL) {
            Log.i(TAG, "received request for making call");
            Intent serviceIntent = new Intent(Intent.ACTION_SYNC, null, context, MakeCallService.class);
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);
        } else if(requestType == ApiConstants.REQUEST_TYPE_OPEN_SETTINGS_PAGE){
            Log.i(TAG, "received request for making call");
            context.startActivity(new Intent(context, AccountsEditList.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if(requestType == ApiConstants.REQUEST_TYPE_SEND_MESSAGE){
            Log.i(TAG, "received request for sending a message");
            Intent serviceIntent = new Intent(Intent.ACTION_SYNC, null, context, SendMessageService.class);
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);
        } else {
            Log.e(TAG, "Unknown requestType: " + " . Make sure the request type is correct.");
        }
    }
}