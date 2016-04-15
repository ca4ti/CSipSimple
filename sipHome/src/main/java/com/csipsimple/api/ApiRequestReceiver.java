package com.csipsimple.api;

/**
 * Created by kadyrovs on 18.01.2016.
 */

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.csipsimple.db.DBProvider;
import com.csipsimple.ui.SipHome;
import com.csipsimple.ui.account.AccountsEditList;
import com.csipsimple.utils.PreferencesWrapper;
import com.csipsimple.utils.VersionUtil;
import com.csipsimple.wizards.WizardUtils;

import java.util.UUID;

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

        if (requestType == ApiConstants.REQUEST_TYPE_MAKE_CALL) {
            Log.i(TAG, "received request for making call");
            Intent serviceIntent = new Intent(Intent.ACTION_SYNC, null, context, MakeCallService.class);
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);

        } else if (requestType == ApiConstants.REQUEST_TYPE_OPEN_SETTINGS_PAGE) {
            Log.i(TAG, "received request for opening account settings page");
            context.startActivity(new Intent(context, AccountsEditList.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

        } else if (requestType == ApiConstants.REQUEST_TYPE_SEND_MESSAGE) {
            Log.i(TAG, "received request for sending a message");
            Intent serviceIntent = new Intent(Intent.ACTION_SYNC, null, context, SendMessageService.class);
            serviceIntent.putExtras(intent);
            context.startService(serviceIntent);

        } else if (requestType == ApiConstants.REQUEST_TYPE_INSTALLATION_CHECK) {
            Log.i(TAG, "received request for checking installation");
            Intent intentInstallationResponse = new Intent(ApiConstants.API_RESPONSE_BROADCAST_ACTION);
            intentInstallationResponse.putExtra(ApiConstants.API_RESPONSE_TYPE_INTENT_KEY, ApiConstants.API_RESPONSE_TYPE_INSTALLATION_CHECK);
            context.sendBroadcast(intentInstallationResponse);

        } else if (requestType == ApiConstants.REQUEST_TYPE_VERSION_REQUEST) {
            try {
                Log.i(TAG, "received request for checking version");
                Intent intentVersion = new Intent(ApiConstants.API_RESPONSE_BROADCAST_ACTION);
                intentVersion.putExtra(ApiConstants.API_RESPONSE_TYPE_INTENT_KEY, ApiConstants.API_RESPONSE_TYPE_CSIPSIMPLE_VERSION);
                intentVersion.putExtra(ApiConstants.CSIPIMPLE_VERSION_INTENT_KEY, ApiConstants.APPNAME + ": " + VersionUtil.getVersionName(context));
                context.sendBroadcast(intentVersion);
            } catch (Exception e) {
                Log.e(TAG, "An error occured while trying to send version information. " + e);
                Intent intentVersion = new Intent(ApiConstants.API_RESPONSE_BROADCAST_ACTION);
                intentVersion.putExtra(ApiConstants.API_RESPONSE_TYPE_INTENT_KEY, ApiConstants.API_RESPONSE_TYPE_CSIPSIMPLE_VERSION);
                intentVersion.putExtra(ApiConstants.CSIPIMPLE_VERSION_INTENT_KEY, "An error occurred");
                context.sendBroadcast(intentVersion);
            }

        } else if (requestType == ApiConstants.REQUEST_TYPE_UPDATE_REGISTRATION) {
            Log.i(TAG, "received request for updating registration");
            String displayName = intent.getStringExtra(ApiConstants.CONTACT_NUMBER_INTENT_KEY);
            String userName = intent.getStringExtra(ApiConstants.CONTACT_NAME_INTENT_KEY);
            String domain = intent.getStringExtra(ApiConstants.CONTACT_DOMAIN_INTENT_KEY);
            String data = intent.getStringExtra(ApiConstants.CONTACT_PASS_INTENT_KEY);
            if (TextUtils.isEmpty(displayName)) {
                Log.e(TAG, "displayName empty!");
            } else if (TextUtils.isEmpty(userName)) {
                Log.e(TAG, "userName empty!");
            }
            updateAccountRegistration(context, displayName, userName, domain, data);
            startSipService(context);
        } else {
            Log.e(TAG, "Unknown requestType: " + requestType + " . Make sure the request type is correct.");
        }
    }

    private void updateAccountRegistration(Context context, String displayName, String userName, String domain, String data) {
        Log.d(TAG, "updateAccountRegistration");
        Log.d(TAG, "new number: " + displayName);
        Log.d(TAG, "new name: " + userName);
        Log.d(TAG, "new domain: " + domain);
        Log.d(TAG, "new data: " + data);

        // Get current account if any
        Cursor c = context.getContentResolver().query(SipProfile.ACCOUNT_URI, new String[]{
                SipProfile.FIELD_ID,
                SipProfile.FIELD_ACC_ID,
                SipProfile.FIELD_REG_URI
        }, null, null, SipProfile.FIELD_PRIORITY + " ASC");
        if (c != null) {
            try {
                if (c.moveToLast()) {
                    SipProfile foundProfile = new SipProfile(c);
                    Log.d(TAG, "found profile: " + foundProfile.toString());
                    SipProfile account = SipProfile.getProfileFromDbId(context, foundProfile.id, DBProvider.ACCOUNT_FULL_PROJECTION);
                    Log.d(TAG, "full account before change: " + account.toString());
                    updateAccount(context, buildFromAccount(account, userName, displayName, domain, data));
                } else {
                    Log.w(TAG, "no existing account found, adding a new account");
                    SipProfile acc = SipProfile.getProfileFromDbId(context, SipProfile.INVALID_ID, DBProvider.ACCOUNT_FULL_PROJECTION);
                    addNewAccount(context, buildFromAccount(acc, userName, displayName, domain, data));
                }
            } catch (Exception e) {
                Log.e(TAG, "Some problem occured while accessing cursor", e);
            } finally {
                c.close();
            }
        } else {
            Log.w(TAG, "no existing account found, adding a new account");
            SipProfile acc = SipProfile.getProfileFromDbId(context, SipProfile.INVALID_ID, DBProvider.ACCOUNT_FULL_PROJECTION);
            addNewAccount(context, buildFromAccount(acc, userName, displayName, domain, data));
        }
    }


    /**
     * Apply default settings for a new account to check very basic coherence of settings and auto-modify settings missing
     *
     * @param account
     */
    private void applyNewAccountDefault(SipProfile account) {
        if (account.use_rfc5626) {
            if (TextUtils.isEmpty(account.rfc5626_instance_id)) {
                String autoInstanceId = (UUID.randomUUID()).toString();
                account.rfc5626_instance_id = "<urn:uuid:" + autoInstanceId + ">";
            }
        }
    }

    /**
     * This account does not exists yet
     */
    private void addNewAccount(Context context, SipProfile account) {
        Log.d(TAG, "addNewAccount");

        PreferencesWrapper prefs = new PreferencesWrapper(context);
        prefs.startEditing();
        prefs.setPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID, account.display_name);
        prefs.endEditing();
        applyNewAccountDefault(account);
        Log.d(TAG, "account getDbContentValues: " + account.getDbContentValues());
        Uri uri = context.getContentResolver().insert(SipProfile.ACCOUNT_URI, account.getDbContentValues());
        Log.d(TAG, "uri after inserting new account: " + uri);
    }

    /**
     * Updating an existing account
     *
     * @param context
     * @param account
     */
    private void updateAccount(Context context, SipProfile account) {
        Log.d(TAG, "updateAccount");

        PreferencesWrapper prefs = new PreferencesWrapper(context);
        prefs.startEditing();

        Log.d(TAG, "account display name:" + account.display_name);
        prefs.setPreferenceStringValue(SipConfigManager.DEFAULT_CALLER_ID, account.display_name);
        prefs.endEditing();
        int rows = context.getContentResolver().update(ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, account.id), account.getDbContentValues(), null, null);
        Log.d(TAG, "number of rows affected: " + rows);
    }

    public SipProfile buildFromAccount(SipProfile account, String displayname, String username, String domain, String data) {
        Log.d(TAG, "buildFromAccount");

        account.display_name = displayname;
        username = username.trim();
        domain = domain.trim();
        String[] serverParts = domain.split(":");
        account.acc_id = "<sip:" + SipUri.encodeUser(username) + "@" + serverParts[0].trim() + ">";
        String regUri = "sip:" + domain;
        account.reg_uri = regUri;
        account.proxies = new String[]{regUri};
        account.wizard = WizardUtils.BASIC_WIZARD_TAG;
        account.realm = "*";
        account.username = username;
        com.csipsimple.utils.Log.d(TAG, "username: " + account.username);
        account.data = data;
        account.scheme = SipProfile.CRED_SCHEME_DIGEST;
        account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
        account.transport = SipProfile.TRANSPORT_UDP;
        return account;
    }

    // Service monitoring stuff
    private void startSipService(final Context context) {
        Thread t = new Thread("StartSip") {
            public void run() {
                Intent serviceIntent = new Intent(SipManager.INTENT_SIP_SERVICE);
                // Optional, but here we bundle so just ensure we are using csipsimple package
                serviceIntent.setPackage(context.getPackageName());
                serviceIntent.putExtra(SipManager.EXTRA_OUTGOING_ACTIVITY, new ComponentName(context, SipHome.class));
                context.startService(serviceIntent);
                //postStartSipService();
            }
        };
        t.start();
    }
}