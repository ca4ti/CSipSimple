package com.example.project.csipsimple_example;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import eu.miraculouslife.android.csipsimple.apilib.ApiConstants;

public class MainActivity extends AppCompatActivity {

    private EditText number;
    private EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initializeFields();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Starting a call", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                String num = number.getText().toString();
                String nam = name.getText().toString();
                if (TextUtils.isEmpty(num)) {
                    Toast.makeText(getApplicationContext(), "Enter number", Toast.LENGTH_SHORT).show();
                    return;
                } else if (TextUtils.isEmpty(nam)) {
                    Toast.makeText(getApplicationContext(), "Enter name", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent();
                intent.putExtra(ApiConstants.REQUEST_TYPE_INTENT_KEY, ApiConstants.REQUEST_TYPE_MAKE_CALL);
                intent.putExtra(ApiConstants.TO_CALL_NUMBER_INTENT_KEY, num);
                intent.putExtra(ApiConstants.TO_CALL_NAME_INTENT_KEY, nam);
                intent.setAction(ApiConstants.API_REQUEST_ACTION);
                sendBroadcast(intent);
                Toast.makeText(getApplicationContext(), "Sent broadcast", Toast.LENGTH_SHORT).show();
            }
        });

        FloatingActionButton settings = (FloatingActionButton) findViewById(R.id.settings);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Opening accounts page", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

                Intent intent = new Intent();
                intent.putExtra(ApiConstants.REQUEST_TYPE_INTENT_KEY, ApiConstants.REQUEST_TYPE_OPEN_SETTINGS_PAGE);
                intent.setAction(ApiConstants.API_REQUEST_ACTION);
                sendBroadcast(intent);
                Toast.makeText(getApplicationContext(), "Sent account broadcast", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initializeFields(){
        number = (EditText) findViewById(R.id.number);
        name = (EditText) findViewById(R.id.name);
    }
}
