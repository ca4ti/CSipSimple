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
                Toast.makeText(context, "Call ended", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Call ended");
                break;

            case ApiConstants.API_RESPONSE_TYPE_CALL_CANCELLED:
                Toast.makeText(context, "Call cancelled", Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Call cancelled");
                break;

            case ApiConstants.API_RESPONSE_TYPE_CALL_CONNECTED:
                Toast.makeText(context, "Call connected ...", Toast.LENGTH_SHORT).show();
                boolean isIncoming = intent.getBooleanExtra(ApiConstants.IS_CALL_INCOMING_INTENT_KEY, false);
                String calleeContact = intent.getStringExtra(ApiConstants.CALL_CONNECTED_CALLEE_INTENT_KEY);
                if(calleeContact != null){
                    Log.i(TAG, "CalleeContact: " + calleeContact);
                    if(isIncoming){
                        Log.d(TAG, "call is incoming call");
                    } else {
                        // AMI gets notified via JS
                        //sendCallConnected
                    }
                } else {
                    Log.e(TAG, "callee contact null");
                }
                break;

            case ApiConstants.API_RESPONSE_TYPE_INSTALLATION_CHECK:
                Log.i(TAG, "App is installed");
                Toast.makeText(context, "App is installed", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}