package com.csipsimple.ui.incall;

/**
 * Created by kadyrovs on 18.01.2016.
 */

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
        /*
        if(targetName != null && !targetName.isEmpty()){
            Log.i(TAG, "targetName: " + targetName);
        } else {
            Log.e(TAG, "targetName is null or empty");
        }
        */
        /*
        InCallFragment callFragment = new InCallFragment();
        intent.putExtra(InCallFragment.CALL_NAME_INTENT_KEY, targetName);
        callFragment.setIntent(intent);
        callFragment.setArguments(intent.getExtras());
        callFragment.show(getSupportFragmentManager(), "");
        */

        //Intent intent1 = new Intent(context, CallActivityWithFragment.class);
        Intent intent1 = new Intent(context, CallActivity.class);
        intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent1.putExtras(intent);
        context.startActivity(intent1);
    }
}