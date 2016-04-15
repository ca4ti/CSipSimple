package com.csipsimple.api;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.IBinder;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

import com.csipsimple.db.DBProvider;
import com.csipsimple.models.Filter;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;


/**
 * Created by kadyrovs on 04.02.2016.
 */
public class MakeCallService extends IntentService {

    private static final String TAG = MakeCallService.class.getSimpleName();


    private Long sipAccountId = SipProfile.INVALID_ID;
    private SipProfile builtProfile;
    private ISipService service;

    private String number;
    public static String CALLEE_NAME;

    public MakeCallService() {
        super(MakeCallService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        Log.d(TAG, "onHandleIntent");

        CALLEE_NAME = intent.getStringExtra(ApiConstants.CONTACT_NAME_INTENT_KEY);
        Log.i(TAG, "calleeName: " + CALLEE_NAME);
        number = intent.getStringExtra(ApiConstants.CONTACT_NUMBER_INTENT_KEY);

        if (!TextUtils.isEmpty(number)) {
            try {
                bindSipService();

                Log.d(TAG, MakeCallService.class.getCanonicalName() + " Stopping!");
                //unbindService(connection);
                this.stopSelf();
            } catch (Exception e) {
                Log.e(TAG, "Exception while starting makeCall", e);
            }
        } else {
            Log.e(TAG, "To call number is null or empty, not making a call");
        }
    }

    private String rewriteNumber(String number) {
        if(number == null){
            return null;
        }

        SipProfile acc = builtProfile;
        if (acc == null) {
            return number;
        }
        String numberRewrite = Filter.rewritePhoneNumber(getApplicationContext(), acc.id, number);
        if (TextUtils.isEmpty(numberRewrite)) {
            return "";
        }
        SipUri.ParsedSipContactInfos finalCallee = acc.formatCalleeNumber(numberRewrite);
        if (!TextUtils.isEmpty(finalCallee.displayName)) {
            return finalCallee.toString();
        }
        return finalCallee.getReadableSipUri();
    }


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            Log.i(TAG, "service connected!");
            makeCall(number);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            service = null;
        }
    };

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

    /**
     * Gets account from ContentResolver
     *
     * @param accountId
     * @return
     */
    public SipProfile getAccount(long accountId) {
        Log.i(TAG, "getAccount");
        return SipProfile.getProfileFromDbId(this, accountId, DBProvider.ACCOUNT_FULL_PROJECTION);
    }

    /**
     * Get current account if any
     */
    private void getCurrentSipAccount() {
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
                    sipAccountId = foundProfile.id;
                    builtProfile = getAccount(sipAccountId);
                    Log.d(TAG, "profile full: " + builtProfile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Some problem occured while accessing cursor", e);
            } finally {
                c.close();
            }
        }
    }


    public void makeCall(String number) {
        Log.i(TAG, "makeCall, number: " + number);
        if (service == null) {
            Log.e(TAG, "service null");
            return;
        }

        getCurrentSipAccount();

        // Find account to use
        if (builtProfile == null) {
            Log.e(TAG, "account null");
            return;
        }

        if (TextUtils.isEmpty(number)) {
            Log.e(TAG, "target number empty!");
            return;
        }

        number = rewriteNumber(PhoneNumberUtils.stripSeparators(number));

        if (TextUtils.isEmpty(number)) {
            Log.e(TAG, "target number empty");
            return;
        }

        // make the call
        if (sipAccountId >= 0) {
            try {
                service.makeCallWithOptions(number, sipAccountId.intValue(), null);
            } catch (RemoteException e) {
                Log.e(TAG, "Service can't be called to make the call");
                com.csipsimple.utils.Log.e(TAG, "Service can't be called to make the call");
            }
        }
    }
}