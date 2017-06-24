package com.mobile.mqttiot.awsinterface;

/**
 * Created by admin-pc on 11-06-2017.
 */

public interface IAwsIotConnectStatusListener {
    void onConnecting(String status);
    void onConnected(String status);
    void onReconnecting(String status);
    void onConnectionLost(String status);
//    void onSubscribe(String topic);
    void onMessageRecieved(byte[] bytesData);
//    void onDisconnect();
}
