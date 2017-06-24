package com.mobile.mqttiot.awsinterface;

/**
 * Created by admin-pc on 11-06-2017.
 */

public interface IAwsOnConnectedListener {
    void onSubscribe();
    void onMessageRecieved();
    void onDisconnect();

}
