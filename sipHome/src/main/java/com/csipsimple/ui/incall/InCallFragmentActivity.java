package com.csipsimple.ui.incall;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri.ParsedSipContactInfos;
import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;

public class InCallFragmentActivity extends SherlockFragmentActivity {
    private static final String THIS_FILE = InCallFragmentActivity.class.getSimpleName();

    protected static final String TAG = InCallFragmentActivity.class.getSimpleName();

    private long existingProfileId = SipProfile.INVALID_ID;

    private SipProfile builtProfile;
    private ISipService service;

    //private IncomingCallReceiver incomingCallReceiver;
    private String targetName = "";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //registerIncomingCallReceiver();

        bindSipService();

        getCurrentSipAccount();

        builtProfile = getAccount(existingProfileId);
        Log.d(TAG, "profile full: " + builtProfile);
    }

    /**
     *  Get current account if any
     */
    private void getCurrentSipAccount(){
        Log.i(TAG, "getCurrentSipAccount");
        Cursor c = getContentResolver().query(SipProfile.ACCOUNT_URI, new String[]{
                SipProfile.FIELD_ID,
                SipProfile.FIELD_ACC_ID,
                SipProfile.FIELD_REG_URI
        }, null, null, SipProfile.FIELD_PRIORITY + " ASC");

        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    SipProfile foundProfile = new SipProfile(c);

                    Log.i(TAG, "found profile: " + foundProfile.toString());
                    existingProfileId = foundProfile.id;
                }
            } catch (Exception e) {
                Log.e(THIS_FILE, "Some problem occured while accessing cursor", e);
            } finally {
                c.close();
            }
        }
    }


    /**
     * Gets account from ContentResolver
     * @param accountId
     * @return
     */
    public SipProfile getAccount(long accountId) {
        Log.i(TAG, "getAccount");
        return SipProfile.getProfileFromDbId(this, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);
    }

    @Override
    public void onDestroy(){
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        //unregisterReceiver(incomingCallReceiver);
    }


    /**
     * Start the SIP stack
     */
    private void startSipStack(){
        Log.i(TAG, "startSipStack");
        try {
            service.sipStart();
        } catch (RemoteException e) {
            Log.d(TAG, "Exception while trying to start sip stack");
            e.printStackTrace();
        }
    }


    /**
     * Bind (starts if it is not running) to the SIP service
     */
    private void bindSipService() {
        Log.i(TAG, "bindService");
        Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
        // Optional, but here we bundle so just ensure we are using csipsimple package
        serviceIntent.setPackage(getApplicationContext().getPackageName());
        getApplicationContext().bindService(serviceIntent, connection,
                Context.BIND_AUTO_CREATE);
    }


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder iBinder) {
            service = ISipService.Stub.asInterface(iBinder);
            Log.d(TAG, "service connected!");
            startSipStack();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };


    public void makeCall(String toCall, String name) {
        Log.i(TAG, "makeCall, number: " + toCall + ", name: " + name);
        this.targetName = name;
        if (service == null) {
            Log.e(TAG, "service null");
            return;
        }

        Long accountToUse;

        // Find account to use
        if (builtProfile == null) {
            Log.e(TAG, "built profile null");
            return;
        }

        accountToUse = builtProfile.id;

        if(toCall.isEmpty()){
            Log.e(TAG, "target number empty!");
        }

        toCall = PhoneNumberUtils.stripSeparators(toCall);
        toCall = rewriteNumber(toCall);

        if (TextUtils.isEmpty(toCall)) {
            Log.e(TAG, "target number empty");
            return;
        }

        // make the call
        if (accountToUse >= 0) {

            // It is a SIP account, try to call service for that
            Log.i(TAG, "using SIP account");
            try {
                service.makeCallWithOptions(toCall, accountToUse.intValue(), null);
            } catch (RemoteException e) {
                Log.e(THIS_FILE, "Service can't be called to make the call");
            }
        }
    }


    private String rewriteNumber(String number) {
        SipProfile acc = builtProfile;
        if (acc == null) {
            return number;
        }
        String numberRewrite = Filter.rewritePhoneNumber(getApplicationContext(), acc.id, number);
        if (TextUtils.isEmpty(numberRewrite)) {
            return "";
        }

        ParsedSipContactInfos finalCallee = acc.formatCalleeNumber(numberRewrite);
        if (!TextUtils.isEmpty(finalCallee.displayName)) {
            return finalCallee.toString();
        }
        return finalCallee.getReadableSipUri();
    }
}