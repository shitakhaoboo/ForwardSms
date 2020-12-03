package com.googleplex.mqtt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    TextView connectionStatus;
    public String message_to_send;
//    for now, the phone number is hard-coded since we only have one meter
//    for a future use-case, it should match the account number to a phone number from the dataabase
    public String to_send_to="\\+254792766237";

    MqttAndroidClient client;
    private Calendar calendar;

    String serverURL = "tcp://broker.hivemq.com:1883";
    String topic = "Jkuat-grid/load_data";
    String msgBody = "";
    String to_send;

    boolean connectionFlag = false;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static MainActivity ins;

    public MainActivity() {
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ins = this;
        setContentView(R.layout.activity_main);
        ActionBar actionBar = getSupportActionBar();
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.SEND_SMS,Manifest.permission.READ_SMS}, PackageManager.PERMISSION_GRANTED);
        if (actionBar != null) {
            actionBar.setLogo(getDrawable(R.drawable.mqtt_icon));
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1000);
        }
        connectToBroker();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy 'at' HH:mm:ss");
        String currentDateandTime = sdf.format(new Date());
        String currentDateandTime1 = currentDateandTime.substring(0,10);
        Intent intent = getIntent();
//        to_send_to = intent.getStringExtra("from");
        message_to_send = intent.getStringExtra("message");
        if (getIntent().getExtras()!= null)
        {
            String[] m= message_to_send.split("\\s");
            Double tokens = Double.parseDouble(m[2].replace("Ksh",""));
            tokens=tokens*0.1;
            Random r = new Random();
            int low = 1000;
            int high = 9999;
            int result = r.nextInt(high-low) + low;
            String token = tokener(currentDateandTime1,""+result,m[10],""+String.format("%.1f",tokens).replace(".",""));
            to_send= "STIMAKONNEKT Token \n Meter Number: "+ m[10]+" \n Token: "+token+" \n Date: "+currentDateandTime+" \n Token Amount: "+tokens +" \n Amount: "+m[2] ;
            SmsManager mySmsManager = SmsManager.getDefault();
            mySmsManager.sendTextMessage(to_send_to,null, to_send, null, null);
        }
        /////////////////////////////////////////////////////////////////
        connectionStatus = findViewById(R.id.connection_status);
        /////////////////////////////////////////////////////////////////


        /////////////////////////////////////////////////////////////////
        /////////////////////////////////////////////////////////////////
//        new DoInBackground().execute();

    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    void connectToBroker() {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), serverURL, clientId);

        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @SuppressLint("SetTextI18n")
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    connectionStatus.setText("Connected To " + serverURL);
                    connectionFlag = true;
                    if (connectionFlag)
                    {
//                        Intent intent = getIntent();
//                        String message = intent.getStringExtra("message");
                        if (getIntent().getExtras()!= null)
                        {
                            msgBody = to_send;
                            sendMessage(topic,msgBody);
                        }
                        else
                        {
                            sendMessage(topic,"bado");
                        }
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Toast.makeText(getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    void sendMessage(String topic, String msg) {
        String payload = msg;
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
            Toast.makeText(getApplicationContext(), "Sent", Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

//    private class DoInBackground extends AsyncTask<Void, Integer, Void> {
//
//        @Override
//        protected Void doInBackground(Void... voids) {
//            new Receiver();
//            return null;
//        }
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionFlag) {
            try {
                IMqttToken disconnectToken = client.disconnect();
                disconnectToken.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        finish();
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
            connectionFlag = false;
        }
    }



    String tokener(String dt_token, String randString,String mt_number, String t_units)
    {
        while (t_units.length()<5)
        {
            t_units="0"+t_units;
        }
        String sToken = dt_token.replace("/","") + randString + mt_number + t_units;
        String str = sToken.replace(" ","");
        //Rearranging the characters in the token String
        String a = ""+str.charAt(0);
        String b = ""+str.charAt(1);
        String c = ""+str.charAt(2);
        String d = ""+str.charAt(3);
        String e = ""+str.charAt(4);
        String f = ""+str.charAt(5);
        String g = ""+str.charAt(6);
        String h = ""+str.charAt(7);
        String i = ""+str.charAt(8);
        String j = ""+str.charAt(9);
        String k = ""+str.charAt(10);
        String aa = ""+str.charAt(11);
        String ab = ""+str.charAt(12);
        String ac = ""+str.charAt(13);
        String ad = ""+str.charAt(14);
        String ae = ""+str.charAt(15);
        String af = ""+str.charAt(16);
        String ag = ""+str.charAt(17);
        String ah = ""+str.charAt(18);
        String ai = ""+str.charAt(19);
        //Rearranging the token String to encryption mode
        String token = i + ae + a + ab + af + g + b + ac + j + ag + h + c + k + ah + e + d + aa + ai + f + ad;
        return token;
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////


}
