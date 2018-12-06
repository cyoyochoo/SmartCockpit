##  依赖Mqtt-release-V1.1.aar
将Mqtt-release-V1.0.aar放到libs目录
``` gradle
repositories {
    flatDir {dirs 'libs'}
}
```
``` gradle
dependencies {
    implementation(name: 'Mqtt-release-V1.0', ext: 'aar')
}
```

##  Mqtt使用
1. **创建mqtt**
``` java
Mqtt mqtt = new Mqtt.Builder()
                    .serverUri("") //服务器地址
                    .userName("") //用户名，可选
                    .password("") //用户密码，可选
                    .clientId("") //客户端唯一标识，不能重复
                    .withCaInputStream(null) //服务端证书校验，可选
                    .withClientInputStream(null) //客户端证书，可选
                    .withClientPassword("") //客户端证书密码，可选
                    .....
                    .build(context);
```
2. **连接mqtt**
``` java
mqtt.connect(new MqttListener() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) Log.i("", "推送重连成功");
            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.i("", "推送连接断开");
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) {
                String msg = new String(mqttMessage.getPayload());
                Log.i("", "收到推送消息：" + msg);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.i("", "推送发送完成");
            }

            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.i("", "推送连接成功");
                //订阅
                mqtt.subscribe(new String[]{"xx/xx"}, new int[]{Mqtt.QosType.best});
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.i("", "推送连接失败");
            }
        });
```
3. **订阅主题**
``` java
//订阅主题 这里订阅 ”aa/aa“，”bb/bb“两个主题
//主题对应的策略有三种：
//Mqtt.QosType.worst    表示只会发送一次推送消息 收到不收到都不关心
//Mqtt.QosType.normal   保证能收到消息，但不一定只收到一条
//Mqtt.QosType.best     保证收到且只能收到一条消息
mqtt.subscribe(new String[]{"aa/aa", "bb/bb"}, new int[]{Mqtt.QosType.best, Mqtt.QosType.normal});
```
4. **发布消息**
``` java
//分别是主题、消息内容、策略、server是否保留该消息
mqtt.publish(String topic, String msg, @Mqtt.QosType int qos, boolean retained);
```
5. **关闭连接**
``` java
mqtt.close();
```
6. **判断连接状态**
``` java
mqtt.isConnected();
```
7. **To be continued...**
