package com.example.project.csipsimple_example;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;

public class UpdateRegistrationActivity extends AppCompatActivity {

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_registration);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText etDisplayName = (EditText) findViewById(R.id.et_displayname);
        final EditText etUserName = (EditText) findViewById(R.id.et_username);
        final EditText etDomain = (EditText) findViewById(R.id.et_domain);
        final EditText etPass = (EditText) findViewById(R.id.et_pass);

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        etDisplayName.setText(pref.getString(PreferenceKeys.KEY_SIP_DISPLAYNAME, ""));
        etUserName.setText(pref.getString(PreferenceKeys.KEY_SIP_USERNAME, ""));
        etDomain.setText(pref.getString(PreferenceKeys.KEY_SIP_DOMAIN, ""));
        etPass.setText(pref.getString(PreferenceKeys.KEY_SIP_PASS, ""));

        Button btnUpdateRegistration = (Button) findViewById(R.id.btn_update_reg);
        btnUpdateRegistration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = etUserName.getText().toString().trim();
                String displayname = etDisplayName.getText().toString().trim();
                String domain = etDomain.getText().toString().trim();
                String pass = etPass.getText().toString().trim();

                if (TextUtils.isEmpty(username)) {
                    Toast.makeText(getApplicationContext(), "Enter number", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(displayname)) {
                    Toast.makeText(getApplicationContext(), "Enter name", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(domain)) {
                    Toast.makeText(getApplicationContext(), "Enter domain", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(pass)) {
                    Toast.makeText(getApplicationContext(), "Enter pass", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent intent = new Intent();
                intent.putExtra(ApiConstants.REQUEST_TYPE_INTENT_KEY, ApiConstants.REQUEST_TYPE_UPDATE_REGISTRATION);
                intent.putExtra(ApiConstants.CONTACT_NUMBER_INTENT_KEY, username);
                intent.putExtra(ApiConstants.CONTACT_NAME_INTENT_KEY, displayname);
                intent.putExtra(ApiConstants.CONTACT_DOMAIN_INTENT_KEY, domain);
                intent.putExtra(ApiConstants.CONTACT_PASS_INTENT_KEY, pass);
                intent.setAction(ApiConstants.API_REQUEST_ACTION);
                sendBroadcast(intent);
                Toast.makeText(getApplicationContext(), "Sent broadcast", Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = pref.edit();
                editor.putString(PreferenceKeys.KEY_SIP_DISPLAYNAME, displayname);
                editor.putString(PreferenceKeys.KEY_SIP_USERNAME, username);
                editor.putString(PreferenceKeys.KEY_SIP_DOMAIN, domain);
                editor.putString(PreferenceKeys.KEY_SIP_PASS, pass);
                editor.apply();
            }
        });
    }
}
