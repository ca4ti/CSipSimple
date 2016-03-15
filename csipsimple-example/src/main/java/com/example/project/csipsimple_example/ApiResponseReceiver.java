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
public class ApiResponseReceiver extends BroadcastReceiver {
    private final String TAG = ApiResponseReceiver.class.getSimpleName();


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive");

        int apiResponseType = intent.getIntExtra(ApiConstants.API_RESPONSE_TYPE_INTENT_KEY, -1);
        switch (apiResponseType){
            case ApiConstants.API_RESPONSE_TYPE_CALL_ENDED:
                boolean callEnded = intent.getBooleanExtra(ApiConstants.CALL_ENDED_STATUS_INTENT_KEY, false);
                if(callEnded){
                    Toast.makeText(context, "Call ended", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Call ended");
                } else {
                    Log.e(TAG, "status is null or empty!");
                }
                break;
            case ApiConstants.API_RESPONSE_TYPE_CALL_CONNECTED:
                Toast.makeText(context, "Call connected ...", Toast.LENGTH_SHORT).show();
                boolean isIncoming = intent.getBooleanExtra(ApiConstants.IS_CALL_INCOMING_INTENT_KEY, false);
                String calleeContact = intent.getStringExtra(ApiConstants.CALL_CONNECTED_CALLEE_INTENT_KEY);
                if(calleeContact != null){
                    if(isIncoming){
                        Log.d(TAG, "call is incoming call");
                    }
                    Log.i(TAG, "Call connected with: " + calleeContact);
                } else {
                    Log.e(TAG, "callee contact null");
                }
                //TODO: notify the AMI about this
                break;
            case ApiConstants.API_RESPONSE_TYPE_INSTALLATION_CHECK:
                Log.i(TAG, "App is installed");
                Toast.makeText(context, "App is installed", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}