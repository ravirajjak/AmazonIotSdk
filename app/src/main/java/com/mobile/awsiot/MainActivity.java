package com.mobile.awsiot;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.mobile.mqttiot.Iot;
import com.mobile.mqttiot.awsinterface.IAwsIotConnectStatusListener;

public class MainActivity extends AppCompatActivity implements IAwsIotConnectStatusListener{
    static  String TAG = "R-MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Iot.setAwsIotListener(this);
    }

    @Override
    public void onConnecting(String status) {
        Log.d(TAG,status);
    }

    @Override
    public void onConnected(String status) {
        Log.d(TAG,status);
        Iot.onConnectSubscribeToTopic("sub");
    }

    @Override
    public void onReconnecting(String status) {
        Log.d(TAG,status);
    }

    @Override
    public void onConnectionLost(String status) {
        Log.d(TAG,status);
    }

    @Override
    public void onMessageRecieved(byte[] bytesData) {
    }


}
