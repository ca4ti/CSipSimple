package com.csipsimple.ui.incall;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Window;

import com.csipsimple.R;
import com.csipsimple.utils.Log;

/**
 * Created by kadyrovs on 18.01.2016.
 */
public class CallActivityWithFragment extends FragmentActivity {

    private static final String TAG = CallActivityWithFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        Log.i(TAG, "onCreate");
        // request a window without the title
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        this.setFinishOnTouchOutside(false);
        setContentView(R.layout.activity_call);
        InCallFragment firstFragment = new InCallFragment();
        firstFragment.setArguments(getIntent().getExtras());
        firstFragment.setIntent(getIntent());
        getSupportFragmentManager().beginTransaction()
        .add(R.id.frame_container, firstFragment).commit();
    }
}