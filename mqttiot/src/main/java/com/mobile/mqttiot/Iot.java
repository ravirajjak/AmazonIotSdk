package com.mobile.mqttiot;

import android.app.Activity;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.mobile.mqttiot.awsinterface.IAwsIotConnectStatusListener;
import com.mobile.mqttiot.awsinterface.IAwsOnConnectedListener;

import java.io.UnsupportedEncodingException;
import java.security.KeyStore;

/**
 * Created by admin-pc on 11-06-2017.
 */

public class Iot {

    // --- Constants to modify per your configuration ---

    // IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com
    private static String CUSTOMER_SPECIFIC_ENDPOINT = null;
    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static String COGNITO_POOL_ID = null;
    // Name of the AWS IoT policy to attach to a newly created certificate
    private static String AWS_IOT_POLICY_NAME = null;
    //    private static final String AWS_IOT_POLICY_NAME = "100MB-Policy";
    // Region of AWS IoT
    private static Regions MY_REGION = null;


    // Filename of KeyStore file on the filesystem
    private static String KEYSTORE_NAME = "keystore_mqtt.bks";
    // Password for the private key in the KeyStore
    private static String KEYSTORE_PASSWORD = null;
    // Certificate and key aliases in the KeyStore
    private static String CERTIFICATE_ID = null;

    String clientId;
    AWSIotClient mIotAndroidClient;
    AWSIotMqttManager mqttManager;
    KeyStore clientKeyStore = null;
    Activity activity;
    CognitoCachingCredentialsProvider credentialsProvider;
    private String keystorePath, keystoreName, keystorePassword;
    private String certificateId;
    String LOG_TAG = "R-IOT";
    private String KEYSTORE_PATH;
    IAwsIotConnectStatusListener connectionListener;
    IAwsOnConnectedListener onSubscribeListener;
    static Iot iot = new Iot();
    public static void setAwsIotListener(IAwsIotConnectStatusListener connectionListener) {
        getInstance().setConnectionListener(connectionListener);

    }

/*    public static synchronized void setAwsOnSubscribeListener(IAwsOnConnectedListener onSubscribeListener) {
        setOnSubscribeListener(onSubscribeListener);
    }*/

    private static Iot getInstance() {
        return iot;
    }

    void setConnectionListener(IAwsIotConnectStatusListener connectionListener) {
        this.connectionListener = connectionListener;
    }

    void setOnSubscribeListener(IAwsOnConnectedListener onSubscribeListener) {
        this.onSubscribeListener = onSubscribeListener;
    }


    public void setAwsEndpointParameter(String endPoint, String cognitoPoolId, String awsIotPolicyName
            , Regions region, String keyStoreNameBks, String keystorePassword, String keystoreAliasName, String keystorePath, String uniqueClientId) {
        CUSTOMER_SPECIFIC_ENDPOINT = endPoint;
        COGNITO_POOL_ID = cognitoPoolId;
        AWS_IOT_POLICY_NAME = awsIotPolicyName;
        MY_REGION = region;
        KEYSTORE_NAME = keyStoreNameBks;
        KEYSTORE_PASSWORD = keystorePassword;
        CERTIFICATE_ID = keystoreAliasName;
        KEYSTORE_PATH = keystorePath;
        clientId = uniqueClientId;
    }


    private void initializeKeystore() {

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                activity, // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        final Region region = Region.getRegion(MY_REGION);

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);
        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);
        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);

        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);
        keystorePath = KEYSTORE_PATH;
        keystoreName = KEYSTORE_NAME;
        keystorePassword = KEYSTORE_PASSWORD;
        certificateId = CERTIFICATE_ID;
//        copyAssets();
//        System.out.println("KEY PATH "+keystorePath);
        // To load cert/key from keystore on filesystem
        try {
            if (AWSIotKeystoreHelper.isKeystorePresent(keystorePath, keystoreName)) {
                if (AWSIotKeystoreHelper.keystoreContainsAlias(certificateId, keystorePath,
                        keystoreName, keystorePassword)) {
                    Log.i(LOG_TAG, "Certificate " + certificateId
                            + " found in keystore - using for MQTT.");
                    // load keystore from file into memory to pass on connection
                    clientKeyStore = AWSIotKeystoreHelper.getIotKeystore(certificateId,
                            keystorePath, keystoreName, keystorePassword);
//                    btnConnect.setEnabled(true);
                } else {
                    Log.i(LOG_TAG, "Key/cert " + certificateId + " not found in keystore.");
                }
            } else {
                Log.i(LOG_TAG, "Keystore " + keystorePath + "/" + keystoreName + " not found.");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "An error occurred retrieving cert/key from keystore.", e);
        }
    }

    void connectMqtt() {
        try {
            initializeKeystore();
            mqttManager.connect(clientKeyStore, new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String mesg = null;
                            if (status == AWSIotMqttClientStatus.Connecting) {

                                mesg = "IOT Connecting";
                                connectionListener.onConnecting(mesg);

                            } else if (status == AWSIotMqttClientStatus.Connected) {

                                mesg = "IOT Connected";
                                connectionListener.onConnected(mesg);
                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {

                                if (throwable != null) {
                                    mesg = "IOT Connection Error";
                                    connectionListener.onConnectionLost(mesg);
                                }

                                mesg = "IOT Reconnecting";
                                connectionListener.onReconnecting(mesg);

                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {

                                if (throwable != null) {
                                    mesg = "IOT Connection Error";
                                    connectionListener.onConnectionLost(mesg);
                                }

                                mesg = "IOT Reconnecting";
                                connectionListener.onReconnecting(mesg);

                            } else {

                                mesg = "IOT Connection Error";
                                connectionListener.onConnectionLost(mesg);

                            }
                        }
                    });

                }
            });
        } catch (final Exception e) {
            Log.e("IOT-R-Exception", "Connection error.", e);
//            tvStatus.setText("Error! " + e.getMessage());
        }
    }
    public static void onConnectSubscribeToTopic(String topic){
        getInstance().onSubscribeToTopic(topic);
    }

    private void onSubscribeToTopic(String topic) {

        try {
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                    new AWSIotMqttNewMessageCallback() {
                        @Override
                        public void onMessageArrived(final String topic, final byte[] data) {
                            connectionListener.onMessageRecieved(data);
                        }
                    });
        } catch (Exception e) {
            Log.e(LOG_TAG, "Subscription error.", e);
        }
    }
    public static void publishTopic(String topic,String message){
        getInstance().publishMqtt(topic,message);
    }
    void publishMqtt(String topic, String msg) {

        String strTopic = topic;
        String strMesg = msg;

        try {
            mqttManager.publishString(strMesg, strTopic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public static void disconnectIot(){
        getInstance().disconnectIotConnection();
    }
    private void disconnectIotConnection() {

        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }
}
