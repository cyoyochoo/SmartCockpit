package com.hsae.smartcockpit.mqtt;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.text.TextUtils;
import android.util.Pair;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

public class Mqtt {
    private Context context;
    private String serverUri;
    private String userName;
    private String password;
    private String clientId;
    private int timeOut;
    private int keepAliveInterval;
    private boolean cleanSession;
    private boolean autoReconnect;
    private InputStream caInputStream;
    private InputStream clientInputStream;
    private String clientPassword;

    private MqttAndroidClient client;
    private MqttConnectOptions options;
    private MqttListener mqttListener;
    private Handler handler;
    /*private boolean innerConnectExecuted;
    private BroadcastReceiver innerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (innerConnectExecuted && !isConnected() && isNetworkAvailable()) {
                innerConnect();
            }
        }
    };*/

    private Mqtt(Builder builder) {
        this.context = builder.context;
        this.serverUri = builder.serverUri;
        this.userName = builder.userName;
        this.password = builder.password;
        this.clientId = builder.clientId;
        this.timeOut = builder.timeOut;
        this.keepAliveInterval = builder.keepAliveInterval;
        this.cleanSession = builder.cleanSession;
        this.autoReconnect = builder.autoReconnect;
        this.caInputStream = builder.caInputStream;
        this.clientInputStream = builder.clientInputStream;
        this.clientPassword = builder.clientPassword;
        init();
    }

    private void init() {
        client = new MqttAndroidClient(context, serverUri, clientId);
        client.setCallback(mqttCallback);

        options = new MqttConnectOptions();
        options.setAutomaticReconnect(autoReconnect);
        options.setCleanSession(cleanSession);
        options.setConnectionTimeout(timeOut);
        options.setKeepAliveInterval(keepAliveInterval);
        if (password != null) options.setPassword(password.toCharArray());
        if (!TextUtils.isEmpty(userName)) options.setUserName(userName);

        if (caInputStream != null || (clientInputStream != null && clientPassword != null)) {
            HandlerThread thread = new HandlerThread("mqtt", Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            handler = new Handler(thread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Pair<SSLSocketFactory, X509TrustManager> pair = new SSLOptions.Builder()
                            .withCaInputStream(caInputStream)
                            .withClientInputStream(clientInputStream)
                            .withClientPassword(clientPassword).build().get();
                    options.setSocketFactory(pair.first);
                }
            });
        }
//        context.registerReceiver(innerReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable();
        }
        return false;
    }

    public void connect(@NonNull MqttListener mqttListener) {
        this.mqttListener = mqttListener;
        if (handler != null) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    innerConnect();
                    handler.getLooper().quit();
                    handler = null;
                }
            });
        } else {
            innerConnect();
        }
    }

    private void innerConnect() {
        if (!client.isConnected()) {
            try {
                client.connect(options, null, mqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
//        innerConnectExecuted = true;
    }

    public void close() {
        try {
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
//        context.unregisterReceiver(innerReceiver);
    }

    public void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    public void publish(String topic, String msg , @QosType int qos, boolean retained) {
        try {
            client.publish(topic, msg.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String[] topic, @QosType int[] qos) {
        try {
            client.subscribe(topic, qos);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void unsubscribe(String[] topic) {
        try {
            client.unsubscribe(topic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private MqttCallback mqttCallback = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            mqttListener.connectComplete(reconnect, serverURI);
        }

        @Override
        public void connectionLost(Throwable cause) {
            mqttListener.connectionLost(cause);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            mqttListener.messageArrived(topic, message);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            mqttListener.deliveryComplete(token);
        }
    };

    private IMqttActionListener mqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            mqttListener.onSuccess(asyncActionToken);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            mqttListener.onFailure(asyncActionToken, exception);
        }
    };

    @IntDef({QosType.worst, QosType.normal, QosType.best})
    @Retention(RetentionPolicy.SOURCE)
    public @interface QosType {
        int worst = 0;
        int normal= 1;
        int best  = 2;
    }

    public static final class Builder {
        private Context context;
        private String serverUri = "ssl://120.78.255.108:8886";
        private String userName;
        private String password = "";
        private String clientId;
        private int timeOut = 30;
        private int keepAliveInterval = 60;
        private boolean cleanSession;
        private boolean autoReconnect = true;
        private InputStream caInputStream;
        private InputStream clientInputStream;
        private String clientPassword;

        public Mqtt build(@NonNull Context context) {
            this.context = context.getApplicationContext();
            return new Mqtt(this);
        }

        public Builder serverUri(String serverUri) {
            this.serverUri = serverUri;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public Builder timeOut(int timeOut) {
            this.timeOut = timeOut;
            return this;
        }

        public Builder keepAliveInterval(int keepAliveInterval) {
            this.keepAliveInterval = keepAliveInterval;
            return this;
        }

        public Builder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder withCaInputStream(InputStream caInputStream) {
            this.caInputStream = caInputStream;
            return this;
        }

        public Builder withClientInputStream(InputStream clientInputStream) {
            this.clientInputStream = clientInputStream;
            return this;
        }

        public Builder withClientPassword(String clientPassword) {
            this.clientPassword = clientPassword;
            return this;
        }
    }
}
