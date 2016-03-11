package com.csipsimple.ui.incall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;

//import com.actionbarsherlock.app.SherlockDialogFragment;
//import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.csipsimple.api.ISipService;
import com.csipsimple.api.MediaState;
import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.service.SipService;
import com.csipsimple.ui.PickupSipUri;
import com.csipsimple.ui.incall.locker.IOnLeftRightChoice;
import com.csipsimple.ui.incall.locker.InCallAnswerControls;
import com.csipsimple.ui.incall.locker.ScreenLocker;
import com.csipsimple.utils.CallsUtils;
import com.csipsimple.utils.DialingFeedback;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesProviderWrapper;
import com.csipsimple.utils.Theme;
import com.csipsimple.utils.keyguard.KeyguardWrapper;

import org.webrtc.videoengine.ViERenderer;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by kadyrovs on 04.01.2016.
 */
public class InCallFragment extends Fragment implements IOnCallActionTrigger,
        IOnLeftRightChoice, CallProximityManager.ProximityDirector, DtmfDialogFragment.OnDtmfListener{


    private static final int QUIT_DELAY = 3000;
    private final static String TAG = InCallFragment.class.getSimpleName();
    public static final String CALL_NAME_INTENT_KEY = "calling_person_name_key";

    private Object callMutex = new Object();
    private SipCallSession[] callsInfo = null;
    private MediaState lastMediaState;


    private ViewGroup mainFrame;
    //private InCallControls inCallControls;

    // Screen wake lock for incoming call
    private PowerManager.WakeLock wakeLock;
    // Screen wake lock for video
    private PowerManager.WakeLock videoWakeLock;

    private InCallInfoGrid activeCallsGrid;
    private Timer quitTimer;


    private DialingFeedback dialFeedback;
    private PowerManager powerManager;
    private PreferencesProviderWrapper prefsWrapper;

    private SurfaceView cameraPreview;
    private CallProximityManager proximityManager;
    private KeyguardWrapper keyguardManager;

    private boolean useAutoDetectSpeaker = false;
    private InCallAnswerControls inCallAnswerControls;
    private CallsAdapter activeCallsAdapter;
    private InCallInfoGrid heldCallsGrid;
    private CallsAdapter heldCallsAdapter;

    private final static int PICKUP_SIP_URI_XFER = 0;
    private final static int PICKUP_SIP_URI_NEW_CALL = 1;
    private static final String CALL_ID = "call_id";

    private FragmentActivity activity;
    private SipCallSession initialSession;
    private String targetName = "";

    private boolean callActive = true;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);

        activity = getActivity();
        this.callActive = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");


        View rootView = inflater.inflate(com.csipsimple.R.layout.call_dialog, container, false);


        getActivity().bindService(new Intent(activity, SipService.class), connection, Context.BIND_AUTO_CREATE);
        prefsWrapper = new PreferencesProviderWrapper(activity);

        powerManager = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "com.csipsimple.onIncomingCall");
        wakeLock.setReferenceCounted(false);

        activity.takeKeyEvents(true);

        // Cache findViews
        mainFrame = (ViewGroup) rootView.findViewById(com.csipsimple.R.id.mainFrame);
        //inCallControls = (InCallControls) rootView.findViewById(R.id.inCallControls);
        inCallAnswerControls = (InCallAnswerControls) rootView.findViewById(com.csipsimple.R.id.inCallAnswerControls);
        activeCallsGrid = (InCallInfoGrid) rootView.findViewById(com.csipsimple.R.id.activeCallsGrid);
        heldCallsGrid = (InCallInfoGrid) rootView.findViewById(com.csipsimple.R.id.heldCallsGrid);

        // Bind
        attachVideoPreview();

        //inCallControls.setOnTriggerListener(this);
        inCallAnswerControls.setOnTriggerListener(this);

        if(activeCallsAdapter == null) {
            activeCallsAdapter = new CallsAdapter(true);
        }
        activeCallsGrid.setAdapter(activeCallsAdapter);


        if(heldCallsAdapter == null) {
            heldCallsAdapter = new CallsAdapter(false);
        }
        heldCallsGrid.setAdapter(heldCallsAdapter);


        ScreenLocker lockOverlay = (ScreenLocker) rootView.findViewById(com.csipsimple.R.id.lockerOverlay);
        lockOverlay.setActivity(activity);
        lockOverlay.setOnLeftRightListener(this);

        /*
        middleAddCall = (Button) findViewById(R.id.add_call_button);
        middleAddCall.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                onTrigger(ADD_CALL, null);
            }
        });
        if (!prefsWrapper.getPreferenceBooleanValue(SipConfigManager.SUPPORT_MULTIPLE_CALLS)) {
            middleAddCall.setEnabled(false);
            middleAddCall.setText(R.string.not_configured_multiple_calls);
        }
        */

        // Listen to media & sip events to update the UI
        getActivity().registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_CALL_CHANGED));
        getActivity().registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_SIP_MEDIA_CHANGED));
        getActivity().registerReceiver(callStateReceiver, new IntentFilter(SipManager.ACTION_ZRTP_SHOW_SAS));

        proximityManager = new CallProximityManager(activity, this, lockOverlay);
        keyguardManager = KeyguardWrapper.getKeyguardManager(activity);

        dialFeedback = new DialingFeedback(getActivity(), true);

        if (prefsWrapper.getPreferenceBooleanValue(SipConfigManager.PREVENT_SCREEN_ROTATION)) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        if (quitTimer == null) {
            quitTimer = new Timer("Quit-timer");
        }


        useAutoDetectSpeaker = prefsWrapper.getPreferenceBooleanValue(SipConfigManager.AUTO_DETECT_SPEAKER);

        applyTheme();
        proximityManager.startTracking();

        //inCallControls.setCallState(initialSession);
        inCallAnswerControls.setCallState(initialSession);
        // Do something else
        return rootView;
    }




    @Override
    public void onStart() {
        Log.d(TAG, "Start in call");
        super.onStart();

        keyguardManager.unlock();
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
        endCallTargetRect = null;
        holdTargetRect = null;
        answerTargetRect = null;
        xferTargetRect = null;
        */
        dialFeedback.resume();


        activity.runOnUiThread(new UpdateUIFromCallRunnable());

    }

    @Override
    public void onPause() {
        super.onPause();
        dialFeedback.pause();
    }

    @Override
    public void onStop() {
        super.onStop();
        keyguardManager.lock();
    }

    @Override
    public void onDestroy() {

        if(infoDialog != null) {
            infoDialog.dismiss();
        }

        if (quitTimer != null) {
            quitTimer.cancel();
            quitTimer.purge();
            quitTimer = null;
        }
        /*
        if (draggingTimer != null) {
            draggingTimer.cancel();
            draggingTimer.purge();
            draggingTimer = null;
        }
        */

        try {
            activity.unbindService(connection);
        } catch (Exception e) {
            // Just ignore that
        }
        service = null;
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        proximityManager.stopTracking();
        proximityManager.release(0);
        try {
            activity.unregisterReceiver(callStateReceiver);
        } catch (IllegalArgumentException e) {
            // That's the case if not registered (early quit)
        }

        if(activeCallsGrid != null) {
            activeCallsGrid.terminate();
        }

        detachVideoPreview();
        //handler.setActivityInstance(null);
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void attachVideoPreview() {
        // Video stuff
        if(prefsWrapper.getPreferenceBooleanValue(SipConfigManager.USE_VIDEO)) {
            if(cameraPreview == null) {
                Log.d(TAG, "Create Local Renderer");
                cameraPreview = ViERenderer.CreateLocalRenderer(activity);
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(256, 256);
                //lp.leftMargin = 2;
                //lp.topMargin= 4;
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
                cameraPreview.setVisibility(View.GONE);
                mainFrame.addView(cameraPreview, lp);
            }else {
                Log.d(TAG, "NO NEED TO Create Local Renderer");
            }

            if(videoWakeLock == null) {
                videoWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "com.csipsimple.videoCall");
                videoWakeLock.setReferenceCounted(false);
            }
        }

        if(videoWakeLock != null && videoWakeLock.isHeld()) {
            videoWakeLock.release();
        }
    }

    private void detachVideoPreview() {
        if(mainFrame != null && cameraPreview != null) {
            mainFrame.removeView(cameraPreview);
        }
        if(videoWakeLock != null && videoWakeLock.isHeld()) {
            videoWakeLock.release();
        }
        if(cameraPreview != null) {
            cameraPreview = null;
        }
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed");
        if(cameraPreview != null && cameraPreview.getVisibility() == View.VISIBLE) {

            cameraPreview.setVisibility(View.GONE);
        }
        activity.runOnUiThread(new UpdateUIFromCallRunnable());
    }

    private void applyTheme() {
        Theme t = Theme.getCurrentTheme(activity);
        if (t != null) {
            // TODO ...
        }
    }




    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICKUP_SIP_URI_XFER:
                if (resultCode == Activity.RESULT_OK && service != null) {
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    int callId = data.getIntExtra(CALL_ID, -1);
                    if(callId != -1) {
                        try {
                            service.xfer((int) callId, callee);
                        } catch (RemoteException e) {
                            // TODO : toaster
                        }
                    }
                }
                return;
            case PICKUP_SIP_URI_NEW_CALL:
                if (resultCode == Activity.RESULT_OK && service != null) {
                    Log.d(TAG, "OnActivityResult, PICKUP_SIP_URI_NEW_CALL");
                    String callee = data.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
                    long accountId = data.getLongExtra(SipProfile.FIELD_ID,
                            SipProfile.INVALID_ID);
                    if (accountId != SipProfile.INVALID_ID) {
                        try {
                            service.makeCall(callee, (int) accountId);
                        } catch (RemoteException e) {
                            // TODO : toaster
                        }
                    }
                }
                return;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }



    /**
     * Get the call that is active on the view
     *
     * @return
     */
    private SipCallSession getActiveCallInfo() {
        SipCallSession currentCallInfo = null;
        if (callsInfo == null) {
            return null;
        }
        for (SipCallSession callInfo : callsInfo) {
            currentCallInfo = getPrioritaryCall(callInfo, currentCallInfo);
        }
        return currentCallInfo;
    }

    /**
     * Get the call with the higher priority comparing two calls
     * @param call1 First call object to compare
     * @param call2 Second call object to compare
     * @return The call object with highest priority
     */
    private SipCallSession getPrioritaryCall(SipCallSession call1, SipCallSession call2) {
        // We prefer the not null
        if (call1 == null) {
            return call2;
        } else if (call2 == null) {
            return call1;
        }
        // We prefer the one not terminated
        if (call1.isAfterEnded()) {
            return call2;
        } else if (call2.isAfterEnded()) {
            return call1;
        }
        // We prefer the one not held
        if (call1.isLocalHeld()) {
            return call2;
        } else if (call2.isLocalHeld()) {
            return call1;
        }
        // We prefer the older call
        // to keep consistancy on what will be replied if new call arrives
        return (call1.getCallStart() > call2.getCallStart()) ? call2 : call1;
    }

    public void setIntent(Intent intent) {
        Log.d(TAG, "setIntent");
        initialSession = intent.getParcelableExtra(SipManager.EXTRA_CALL_INFO);
        targetName = intent.getStringExtra(CALL_NAME_INTENT_KEY);
        if(initialSession == null){
            Log.e(TAG, "setIntent initialSession null!");
        }
        synchronized (callMutex) {
            callsInfo = new SipCallSession[1];
            callsInfo[0] = initialSession;
        }
    }


    /**
     * Update the user interface from calls state.
     */
    private class UpdateUIFromCallRunnable implements Runnable {

        @Override
        public void run() {
            // Current call is the call emphasis by the UI.
            SipCallSession mainCallInfo = null;

            int mainsCalls = 0;
            int heldsCalls = 0;

            synchronized (callMutex) {
                if (callsInfo != null) {
                    for (SipCallSession callInfo : callsInfo) {
                        //Log.d(TAG, "We have a call " + callInfo.getCallId() + " / " + callInfo.getCallState()
                        //                + "/" + callInfo.getMediaStatus());

                        if (!callInfo.isAfterEnded()) {
                            if (callInfo.isLocalHeld()) {
                                heldsCalls++;
                            } else {
                                mainsCalls++;
                            }
                        }
                        mainCallInfo = getPrioritaryCall(callInfo, mainCallInfo);
                    }
                }
            }

            // Update call control visibility - must be done before call cards
            // because badge avail size depends on that
            if ((mainsCalls + heldsCalls) >= 1) {
                // Update in call actions
                //inCallControls.setCallState(mainCallInfo);
                inCallAnswerControls.setCallState(mainCallInfo);
            } else {
                //inCallControls.setCallState(null);
                inCallAnswerControls.setCallState(null);
            }

            heldCallsGrid.setVisibility((heldsCalls > 0)? View.VISIBLE : View.GONE);

            activeCallsAdapter.notifyDataSetChanged();
            heldCallsAdapter.notifyDataSetChanged();

            //findViewById(R.id.inCallContainer).requestLayout();

            if (mainCallInfo != null) {
                Log.d(TAG, "Active call is " + mainCallInfo.getCallId());
                Log.d(TAG, "Update ui from call " + mainCallInfo.getCallId() + " state "
                        + CallsUtils.getStringCallState(mainCallInfo, activity));
                int state = mainCallInfo.getCallState();

                //int backgroundResId = R.drawable.bg_in_call_gradient_unidentified;

                // We manage wake lock
                switch (state) {
                    case SipCallSession.InvState.INCOMING:
                    case SipCallSession.InvState.EARLY:
                    case SipCallSession.InvState.CALLING:
                    case SipCallSession.InvState.CONNECTING:

                        Log.d(TAG, "Acquire wake up lock");
                        if (wakeLock != null && !wakeLock.isHeld()) {
                            wakeLock.acquire();
                        }
                        break;
                    case SipCallSession.InvState.CONFIRMED:
                        break;
                    case SipCallSession.InvState.NULL:
                    case SipCallSession.InvState.DISCONNECTED:
                        Log.d(TAG, "Active call session is disconnected or null wait for quit...");

                        sendEndCallBroadcast();
                        // This will release locks
                        onDisplayVideo(false);
                        delayedQuit();
                        getActivity().finish();
                        return;
                }

                Log.d(TAG, "we leave the update ui function");
            }

            proximityManager.updateProximitySensorMode();

            if (heldsCalls + mainsCalls == 0) {
                delayedQuit();
            }
        }
    }

    private void sendEndCallBroadcast(){
        if(this.callActive) {
            try {
                this.callActive = false;
                
            } catch (Exception e) {
                Log.e(TAG, "Error while sending end call event: " + e);
            }
        } else {
            Log.i(TAG, "endCallEvent is already sent");
        }
    }



    @Override
    public void onDisplayVideo(boolean show) {
        activity.runOnUiThread(new UpdateVideoPreviewRunnable(show));
    }

    /**
     * Update ui from media state.
     */
    private class UpdateUIFromMediaRunnable implements Runnable {
        @Override
        public void run() {
            //inCallControls.setMediaState(lastMediaState);
            proximityManager.updateProximitySensorMode();
        }
    }

    private class UpdateVideoPreviewRunnable implements Runnable {
        private final boolean show;
        UpdateVideoPreviewRunnable(boolean show){
            this.show = show;
        }
        @Override
        public void run() {
            // Update the camera preview visibility
            if(cameraPreview != null) {
                cameraPreview.setVisibility(show ? View.VISIBLE : View.GONE);
                if(show) {
                    if(videoWakeLock != null) {
                        videoWakeLock.acquire();
                    }
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, cameraPreview, true);
                }else {
                    if(videoWakeLock != null && videoWakeLock.isHeld()) {
                        videoWakeLock.release();
                    }
                    SipService.setVideoWindow(SipCallSession.INVALID_CALL_ID, null, true);
                }
            }else {
                Log.w(TAG, "No camera preview available to be shown");
            }
        }
    }



    private synchronized void delayedQuit() {

        if (wakeLock != null && wakeLock.isHeld()) {
            Log.d(TAG, "Releasing wake up lock");
            wakeLock.release();
        }

        proximityManager.release(0);

        activeCallsGrid.setVisibility(View.VISIBLE);
        //inCallControls.setVisibility(View.GONE);

        Log.d(TAG, "Start quit timer");
        if (quitTimer != null) {
            quitTimer.schedule(new QuitTimerTask(), QUIT_DELAY);
        } else {
            //dismiss();
            getActivity().finish();
        }
    }




    private class QuitTimerTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "Run quit timer");
            //dismiss();
            getActivity().finish();
        }
    }

    private void showDialpad(int callId) {
        Log.i(TAG, "showDialpad");
        DtmfDialogFragment newFragment = DtmfDialogFragment.newInstance(callId);
        newFragment.show(activity.getSupportFragmentManager(), "dialog");
        //Toast.makeText(activity, "Not implemented yet", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void OnDtmf(int callId, int keyCode, int dialTone) {
        Log.i(TAG, "onDtmf, keyCode: " + keyCode);

        proximityManager.restartTimer();

        if (service != null) {
            if (callId != SipCallSession.INVALID_CALL_ID) {
                try {
                    service.sendDtmf(callId, keyCode);
                    dialFeedback.giveFeedback(dialTone);
                } catch (RemoteException e) {
                    Log.e(TAG, "Was not able to send dtmf tone", e);
                }
            }
        }
    }

/*
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key down : " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                //
                // Volume has been adjusted by the user.
                //
                Log.d(TAG, "onKeyDown: Volume button pressed");
                int action = AudioManager.ADJUST_RAISE;
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    action = AudioManager.ADJUST_LOWER;
                }

                // Detect if ringing
                SipCallSession currentCallInfo = getActiveCallInfo();
                // If not any active call active
                if (currentCallInfo == null && serviceConnected) {
                    break;
                }

                //TODO: adjust volume here
                if (service != null) {
                    try {
                        service.adjustVolume(currentCallInfo, action, AudioManager.FLAG_SHOW_UI);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Can't adjust volume", e);
                    }
                }

                return true;
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_ENDCALL:
                return inCallAnswerControls.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_SEARCH:
                // Prevent search
                return true;
            default:
                // Nothing to do
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Log.d(TAG, "Key up : " + keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_CALL:
            case KeyEvent.KEYCODE_SEARCH:
                return true;
            case KeyEvent.KEYCODE_ENDCALL:
                return inCallAnswerControls.onKeyDown(keyCode, event);

        }
        return super.onKeyUp(keyCode, event);
    }
*/

    private BroadcastReceiver callStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(SipManager.ACTION_SIP_CALL_CHANGED)) {
                if (service != null) {
                    try {
                        synchronized (callMutex) {
                            callsInfo = service.getCalls();
                            activity.runOnUiThread(new UpdateUIFromCallRunnable());
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Not able to retrieve calls");
                    }
                }
            } else if (action.equals(SipManager.ACTION_SIP_MEDIA_CHANGED)) {
                if (service != null) {
                    MediaState mediaState;
                    try {
                        mediaState = service.getCurrentMediaState();
                        Log.d(TAG, "Media update ...." + mediaState.isSpeakerphoneOn);
                        synchronized (callMutex) {
                            if (!mediaState.equals(lastMediaState)) {
                                lastMediaState = mediaState;
                                activity.runOnUiThread(new UpdateUIFromMediaRunnable());
                            }
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "Can't get the media state ", e);
                    }
                }
            } else if (action.equals(SipManager.ACTION_ZRTP_SHOW_SAS)) {
                SipCallSession callSession = intent.getParcelableExtra(SipManager.EXTRA_CALL_INFO);
                String sas = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                activity.runOnUiThread(new ShowZRTPInfoRunnable(callSession, sas));
            }
        }
    };

    /**
     * Service binding
     */
    private boolean serviceConnected = false;
    private ISipService service;
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            service = ISipService.Stub.asInterface(arg1);
            try {
                // Log.d(TAG,
                // "Service started get real call info "+callInfo.getCallId());
                callsInfo = service.getCalls();
                serviceConnected = true;

                activity.runOnUiThread(new UpdateUIFromCallRunnable());
                activity.runOnUiThread(new UpdateUIFromMediaRunnable());
            } catch (RemoteException e) {
                Log.e(TAG, "Can't get back the call", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceConnected = false;
            callsInfo = null;
        }
    };
    private AlertDialog infoDialog;




    /**
     * Controls buttons (accept, reject etc).
     * @param whichAction what action has been done
     * @param call
     */
    @Override
    public void onTrigger(int whichAction, final SipCallSession call) {

        // Sanity check for actions requiring valid call id
        if (whichAction == IOnCallActionTrigger.TAKE_CALL || whichAction == IOnCallActionTrigger.REJECT_CALL || whichAction == IOnCallActionTrigger.DONT_TAKE_CALL ||
                whichAction == IOnCallActionTrigger.TERMINATE_CALL || whichAction == IOnCallActionTrigger.DETAILED_DISPLAY ||
                whichAction == IOnCallActionTrigger.TOGGLE_HOLD || whichAction == IOnCallActionTrigger.START_RECORDING ||
                whichAction == IOnCallActionTrigger.STOP_RECORDING || whichAction == IOnCallActionTrigger.DTMF_DISPLAY ||
                whichAction == IOnCallActionTrigger.XFER_CALL || whichAction == IOnCallActionTrigger.TRANSFER_CALL ||
                whichAction == IOnCallActionTrigger.START_VIDEO || whichAction == IOnCallActionTrigger.STOP_VIDEO ) {
            // We check that current call is valid for any actions
            if (call == null) {
                Log.e(TAG, "Try to do an action on a null call !!!");
                return;
            }
            if (call.getCallId() == SipCallSession.INVALID_CALL_ID) {
                Log.e(TAG, "Try to do an action on an invalid call !!!");
                return;
            }
        }

        // Reset proximity sensor timer
        proximityManager.restartTimer();

        try {
            switch (whichAction) {
                case IOnCallActionTrigger.TAKE_CALL: {
                    if (service != null) {
                        Log.i(TAG, "Answering call " + call.getCallId());

                        boolean shouldHoldOthers = false;

                        // Well actually we should be always before confirmed
                        if (call.isBeforeConfirmed()) {
                            shouldHoldOthers = true;
                        }

                        service.answer(call.getCallId(), SipCallSession.StatusCode.OK);

                        // if it's a ringing call, we assume that user wants to
                        // hold other calls
                        if (shouldHoldOthers && callsInfo != null) {
                            for (SipCallSession callInfo : callsInfo) {
                                // For each active and running call
                                if (SipCallSession.InvState.CONFIRMED == callInfo.getCallState()
                                        && !callInfo.isLocalHeld()
                                        && callInfo.getCallId() != call.getCallId()) {

                                    Log.d(TAG, "Hold call " + callInfo.getCallId());
                                    service.hold(callInfo.getCallId());

                                }
                            }
                        }
                    }
                    break;
                }
                case IOnCallActionTrigger.DONT_TAKE_CALL: {
                    if (service != null) {
                        Log.i(TAG, "Rejecting the call with BUSY_HERE");
                        service.hangup(call.getCallId(), SipCallSession.StatusCode.BUSY_HERE);
                    }
                    break;
                }
                case IOnCallActionTrigger.REJECT_CALL:
                case IOnCallActionTrigger.TERMINATE_CALL: {
                    if (service != null) {
                        Log.i(TAG, "Rejecting the call");
                        service.hangup(call.getCallId(), 0);
                    }
                    break;
                }
                case IOnCallActionTrigger.MUTE_ON:
                case IOnCallActionTrigger.MUTE_OFF: {
                    if (service != null) {
                        Log.i(TAG, "Set mute " + (whichAction == IOnCallActionTrigger.MUTE_ON));
                        service.setMicrophoneMute((whichAction == IOnCallActionTrigger.MUTE_ON) ? true : false);
                    }
                    break;
                }
                case IOnCallActionTrigger.SPEAKER_ON:
                case IOnCallActionTrigger.SPEAKER_OFF: {
                    if (service != null) {
                        Log.d(TAG, "Set speaker " + (whichAction == IOnCallActionTrigger.SPEAKER_ON));
                        useAutoDetectSpeaker = false;
                        service.setSpeakerphoneOn((whichAction == IOnCallActionTrigger.SPEAKER_ON) ? true : false);
                    }
                    break;
                }
                case IOnCallActionTrigger.BLUETOOTH_ON:
                case IOnCallActionTrigger.BLUETOOTH_OFF: {
                    if (service != null) {
                        Log.d(TAG, "Set bluetooth " + (whichAction == IOnCallActionTrigger.BLUETOOTH_ON));
                        service.setBluetoothOn((whichAction == IOnCallActionTrigger.BLUETOOTH_ON) ? true : false);
                    }
                    break;
                }
                case IOnCallActionTrigger.DTMF_DISPLAY: {
                    Log.d(TAG, "DTMF_DISPLAY");
                    showDialpad(call.getCallId());
                    break;
                }
                case IOnCallActionTrigger.DETAILED_DISPLAY: {
                    if (service != null) {
                        if(infoDialog != null) {
                            infoDialog.dismiss();
                        }
                        String infos = service.showCallInfosDialog(call.getCallId());
                        String natType = service.getLocalNatType();
                        SpannableStringBuilder buf = new SpannableStringBuilder();
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                        buf.append(infos);
                        if(!TextUtils.isEmpty(natType)) {
                            buf.append("\r\nLocal NAT type detected : ");
                            buf.append(natType);
                        }
                        TextAppearanceSpan textSmallSpan = new TextAppearanceSpan(activity,
                                android.R.style.TextAppearance_Small);
                        buf.setSpan(textSmallSpan, 0, buf.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        infoDialog = builder.setIcon(android.R.drawable.ic_dialog_info)
                                .setMessage(buf)
                                .setNeutralButton(com.csipsimple.R.string.ok, null)
                                .create();
                        infoDialog.show();
                    }
                    break;
                }
                case IOnCallActionTrigger.TOGGLE_HOLD: {
                    if (service != null) {
                        // Log.d(TAG,
                        // "Current state is : "+callInfo.getCallState().name()+" / "+callInfo.getMediaStatus().name());
                        if (call.getMediaStatus() == SipCallSession.MediaState.LOCAL_HOLD ||
                                call.getMediaStatus() == SipCallSession.MediaState.NONE) {
                            service.reinvite(call.getCallId(), true);
                        } else {
                            service.hold(call.getCallId());
                        }
                    }
                    break;
                }
                case IOnCallActionTrigger.MEDIA_SETTINGS: {
                    startActivity(new Intent(activity, InCallMediaControl.class));
                    break;
                }
                case IOnCallActionTrigger.XFER_CALL: {
                    Intent pickupIntent = new Intent(activity, PickupSipUri.class);
                    pickupIntent.putExtra(CALL_ID, call.getCallId());
                    startActivityForResult(pickupIntent, PICKUP_SIP_URI_XFER);
                    break;
                }
                case IOnCallActionTrigger.TRANSFER_CALL: {
                    final ArrayList<SipCallSession> remoteCalls = new ArrayList<SipCallSession>();
                    if(callsInfo != null) {
                        for(SipCallSession remoteCall : callsInfo) {
                            // Verify not current call
                            if(remoteCall.getCallId() != call.getCallId() && remoteCall.isOngoing()) {
                                remoteCalls.add(remoteCall);
                            }
                        }
                    }

                    if(remoteCalls.size() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        CharSequence[] simpleAdapter = new String[remoteCalls.size()];
                        for(int i = 0; i < remoteCalls.size(); i++) {
                            simpleAdapter[i] = remoteCalls.get(i).getRemoteContact();
                        }
                        builder.setSingleChoiceItems(simpleAdapter , -1, new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (service != null) {
                                    try {
                                        // 1 = PJSUA_XFER_NO_REQUIRE_REPLACES
                                        service.xferReplace(call.getCallId(), remoteCalls.get(which).getCallId(), 1);
                                    } catch (RemoteException e) {
                                        Log.e(TAG, "Was not able to call service method", e);
                                    }
                                }
                                dialog.dismiss();
                            }
                        })
                                .setCancelable(true)
                                .setNeutralButton(com.csipsimple.R.string.cancel, new Dialog.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }

                    break;
                }
                case IOnCallActionTrigger.ADD_CALL: {
                    Intent pickupIntent = new Intent(activity, PickupSipUri.class);
                    startActivityForResult(pickupIntent, PICKUP_SIP_URI_NEW_CALL);
                    break;
                }
                case IOnCallActionTrigger.START_RECORDING :{
                    if(service != null) {
                        // TODO : add a tweaky setting for two channel recording in different files.
                        // Would just result here in two calls to start recording with different bitmask
                        service.startRecording(call.getCallId(), SipManager.BITMASK_ALL);
                    }
                    break;
                }
                case IOnCallActionTrigger.STOP_RECORDING : {
                    if(service != null) {
                        service.stopRecording(call.getCallId());
                    }
                    break;
                }
                case IOnCallActionTrigger.START_VIDEO :
                case IOnCallActionTrigger.STOP_VIDEO : {
                    if(service != null) {
                        Bundle opts = new Bundle();
                        opts.putBoolean(SipCallSession.OPT_CALL_VIDEO, whichAction == IOnCallActionTrigger.START_VIDEO);
                        service.updateCallOptions(call.getCallId(), opts);
                    }
                    break;
                }
                case IOnCallActionTrigger.ZRTP_TRUST : {
                    if(service != null) {
                        service.zrtpSASVerified(call.getCallId());
                    }
                    break;
                }
                case IOnCallActionTrigger.ZRTP_REVOKE : {
                    if(service != null) {
                        service.zrtpSASRevoke(call.getCallId());
                    }
                    break;
                }
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Was not able to call service method", e);
        }
    }



    @Override
    public void onLeftRightChoice(int whichHandle) {
        switch (whichHandle) {
            case IOnLeftRightChoice.LEFT_HANDLE:
                Log.d(TAG, "We unlock");
                proximityManager.release(0);
                proximityManager.restartTimer();
                break;
            case IOnLeftRightChoice.RIGHT_HANDLE:
                Log.d(TAG, "We clear the call");
                onTrigger(IOnCallActionTrigger.TERMINATE_CALL, getActiveCallInfo());
                proximityManager.release(0);
            default:
                break;
        }

    }




    private class ShowZRTPInfoRunnable implements Runnable, DialogInterface.OnClickListener {
        private String sasString;
        private SipCallSession callSession;

        public ShowZRTPInfoRunnable(SipCallSession call, String sas) {
            callSession = call;
            sasString = sas;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            if(which == DialogInterface.BUTTON_POSITIVE) {
                Log.d(TAG, "ZRTP confirmed");
                if (service != null) {
                    try {
                        service.zrtpSASVerified(callSession.getCallId());
                    } catch (RemoteException e) {
                        Log.e(TAG, "Error while calling service", e);
                    }
                    dialog.dismiss();
                }
            }else if(which == DialogInterface.BUTTON_NEGATIVE) {
                dialog.dismiss();
            }
        }
        @Override
        public void run() {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            Resources r = getResources();
            builder.setTitle("ZRTP supported by remote party");
            builder.setMessage("Do you confirm the SAS : " + sasString);
            builder.setPositiveButton(r.getString(com.csipsimple.R.string.yes), this);
            builder.setNegativeButton(r.getString(com.csipsimple.R.string.no), this);

            AlertDialog backupDialog = builder.create();
            backupDialog.show();
        }
    }





    @Override
    public boolean shouldActivateProximity() {

        // TODO : missing headset & keyboard open
        if(lastMediaState != null) {
            if(lastMediaState.isBluetoothScoOn) {
                return false;
            }
            if(lastMediaState.isSpeakerphoneOn && ! useAutoDetectSpeaker) {
                // Imediate reason to not enable proximity sensor
                return false;
            }
        }

        if (callsInfo == null) {
            return false;
        }

        boolean isValidCallState = true;
        int count = 0;
        for (SipCallSession callInfo : callsInfo) {
            if(callInfo.mediaHasVideo()) {
                return false;
            }
            if(!callInfo.isAfterEnded()) {
                int state = callInfo.getCallState();

                isValidCallState &= (
                        (state == SipCallSession.InvState.CONFIRMED) ||
                                (state == SipCallSession.InvState.CONNECTING) ||
                                (state == SipCallSession.InvState.CALLING) ||
                                (state == SipCallSession.InvState.EARLY && !callInfo.isIncoming())
                );
                count ++;
            }
        }
        if(count == 0) {
            return false;
        }

        return isValidCallState;
    }

    @Override
    public void onProximityTrackingChanged(boolean acquired) {
        if(useAutoDetectSpeaker && service != null) {
            if(acquired) {
                if(lastMediaState == null || lastMediaState.isSpeakerphoneOn) {
                    try {
                        service.setSpeakerphoneOn(false);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Can't run speaker change");
                    }
                }
            }else {
                if(lastMediaState == null || !lastMediaState.isSpeakerphoneOn) {
                    try {
                        service.setSpeakerphoneOn(true);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Can't run speaker change");
                    }
                }
            }
        }
    }




    // Active call adapter
    private class CallsAdapter extends BaseAdapter {

        private boolean mActiveCalls;

        private SparseArray<Long> seenConnected = new SparseArray<Long>();

        public CallsAdapter(boolean notOnHold) {
            mActiveCalls = notOnHold;
        }

        private boolean isValidCallForAdapter(SipCallSession call) {
            boolean holdStateOk = false;
            if(mActiveCalls && !call.isLocalHeld()) {
                holdStateOk = true;
            }
            if(!mActiveCalls && call.isLocalHeld()) {
                holdStateOk = true;
            }
            if(holdStateOk) {
                long currentTime = System.currentTimeMillis();
                if(call.isAfterEnded()) {
                    // Only valid if we already seen this call in this adapter to be valid
                    if(hasNoMoreActiveCall() && seenConnected.get(call.getCallId(), currentTime + 2 * QUIT_DELAY) < currentTime + QUIT_DELAY) {
                        return true;
                    }else {
                        seenConnected.delete(call.getCallId());
                        return false;
                    }
                }else {
                    seenConnected.put(call.getCallId(), currentTime);
                    return true;
                }
            }
            return false;
        }

        private boolean hasNoMoreActiveCall() {
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return true;
                }

                for(SipCallSession call : callsInfo) {
                    // As soon as we have one not after ended, we have at least active call
                    if(!call.isAfterEnded()) {
                        return false;
                    }
                }

            }
            return true;
        }

        @Override
        public int getCount() {
            int count = 0;
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return 0;
                }

                for(SipCallSession call : callsInfo) {
                    if(isValidCallForAdapter(call)) {
                        count ++;
                    }
                }
            }
            return count;
        }

        @Override
        public Object getItem(int position) {
            synchronized (callMutex) {
                if(callsInfo == null) {
                    return null;
                }
                int count = 0;
                for(SipCallSession call : callsInfo) {
                    if(isValidCallForAdapter(call)) {
                        if(count == position) {
                            return call;
                        }
                        count ++;
                    }
                }
            }
            return null;
        }

        @Override
        public long getItemId(int position) {
            SipCallSession call = (SipCallSession) getItem(position);
            if(call != null) {
                return call.getCallId();
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                if(targetName != null && !targetName.isEmpty()){
                    convertView = new InCallCard(activity, null, targetName);
                } else {
                    convertView = new InCallCard(activity, null);
                }
            }

            if(convertView instanceof InCallCard) {
                InCallCard vc = (InCallCard) convertView;
                vc.setOnTriggerListener(InCallFragment.this);
                SipCallSession session = (SipCallSession) getItem(position);
                if(targetName != null && !targetName.isEmpty()){
                    vc.setCallState(session, targetName);
                } else {
                    vc.setCallState(session);
                }
            }

            return convertView;
        }

    }
}