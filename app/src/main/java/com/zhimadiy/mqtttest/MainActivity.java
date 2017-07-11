package com.zhimadiy.mqtttest;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


public class MainActivity extends AppCompatActivity {

    MqttAndroidClient mqttAndroidClient;

    String serverUri;//= "tcp://183.230.40.39:6002";

    String clientId = "3784858";
    String subscriptionTopic = "/6046214/notice";
    String publishTopic = "/6046214/notice";
    String publishMessage = "芝麻DIY";
    String serverusername,serverpassword;
    private TextView display;
    private EditText server_broker,server_port,server_clientid,server_username,server_password,sub_topic,pub_topic,pub_message;
    private Button server_connect,server_disconnect;
    private AlertDialog.Builder builder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        display= (TextView)findViewById(R.id.noticetext);
        display.setMovementMethod(new ScrollingMovementMethod());
        server_broker = (EditText)findViewById(R.id.server_broker);
        server_port = (EditText)findViewById(R.id.server_port);
        server_clientid = (EditText)findViewById(R.id.server_clientid);
        server_username = (EditText)findViewById(R.id.server_username);
        server_password =  (EditText)findViewById(R.id.server_password);
        sub_topic = (EditText)findViewById(R.id.sub_topic);
        pub_topic = (EditText)findViewById(R.id.pub_topic);
        pub_message = (EditText)findViewById(R.id.pub_message);
        server_connect = (Button)findViewById(R.id.server_connect);
        server_disconnect= (Button)findViewById(R.id.server_disconnect);

        display.setMovementMethod(ScrollingMovementMethod.getInstance());

        builder = new AlertDialog.Builder(this);
        builder.setTitle("关于");
        builder.setIcon(R.drawable.icon);
        builder.setMessage("安卓端MQTT消息测试程序\n"+
                "Powered by ZhimaDIY");
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //((ViewGroup) view.getParent()).removeView(view);
            }
        });
        server_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if(mqttAndroidClient.isConnected()){
                    mqttAndroidClient.disconnect();}
                } catch (MqttException e) {
                    e.printStackTrace();
                }
                addToHistory("断开连接");
            }
        });

        server_connect.setOnClickListener(new View.OnClickListener() {                              //监听连接按钮

            @Override
            public void onClick(View view) {
                serverUri="tcp://"+server_broker.getText().toString()+":"+server_port.getText().toString();
                clientId=server_clientid.getText().toString();
                serverusername=server_username.getText().toString();
                serverpassword=server_password.getText().toString();
                MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
                mqttConnectOptions.setAutomaticReconnect(false);
                mqttConnectOptions.setCleanSession(false);
                mqttConnectOptions.setUserName(serverusername);
                mqttConnectOptions.setPassword(serverpassword.toCharArray());


                mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);

                try {
                    addToHistory("连接中...");
                    mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {    //点击连接按钮后，开始连接
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                            disconnectedBufferOptions.setBufferEnabled(true);
                            disconnectedBufferOptions.setBufferSize(100);
                            disconnectedBufferOptions.setPersistBuffer(false);
                            disconnectedBufferOptions.setDeleteOldestMessages(false);
                            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            addToHistory("Failed to connect to: " + serverUri);
                        }
                    });
                    mqttAndroidClient.setCallback(new MqttCallbackExtended() {                      //设置callback
                        @Override
                        public void connectComplete(boolean reconnect, String serverURI) {
                            if (reconnect) {
                                addToHistory("Reconnected to : " + serverURI);
                                // Because Clean Session is true, we need to re-subscribe
                            } else {
                                addToHistory("已连接至" + serverURI);
                            }
                        }

                        @Override
                        public void connectionLost(Throwable cause) {
                            addToHistory("The Connection was lost.");
                        }

                        @Override
                        public void messageArrived(String topic, MqttMessage message) throws Exception {
                            addToHistory("Incoming message: " + topic + new String(message.getPayload()));
                        }

                        @Override
                        public void deliveryComplete(IMqttDeliveryToken token) {

                        }
                    });

                } catch (MqttException ex){
                    ex.printStackTrace();
                }
            }
        });

        FloatingActionButton sub = (FloatingActionButton)findViewById(R.id.sub_button);             //订阅消息
            sub.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(mqttAndroidClient.isConnected()){
                    subscriptionTopic=sub_topic.getText().toString();
                    subscribeToTopic();}
                }
            });

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);                    //推送消息
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                //publishMessage=pack_msg(publishMessage);
                if(mqttAndroidClient.isConnected()){
                publishTopic=pub_topic.getText().toString();
                publishMessage=pub_message.getText().toString();
                publishMessage();}

            }
        });
    }

    public void addToHistory(String mainText){

        System.out.println("LOG: " + mainText);
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
        display.setText(display.getText()+mainText+"\n");
        refreshLogView();

    }
    void refreshLogView(){                                                                         //TextView始终滚动至最后一行
           int offset=display.getLineCount()*display.getLineHeight();
           if(offset>display.getHeight()){
               display.scrollTo(0,offset-display.getHeight());
                }
         }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_about: {
                builder.show();
                break;
            }
            case R.id.action_exit:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void subscribeToTopic(){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    addToHistory("Subscribed!"+subscriptionTopic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    addToHistory("Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(subscriptionTopic, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // message Arrived!
                    addToHistory("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex){
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }


    public void publishMessage(){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(publishMessage.getBytes());
            mqttAndroidClient.publish(publishTopic, message);
            addToHistory(publishTopic+":"+publishMessage);
            if(!mqttAndroidClient.isConnected()){
                addToHistory(mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
