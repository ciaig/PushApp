package com.yan.pushapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private ConnectionFactory factory = new ConnectionFactory();

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        startListen();
    }

    public void Push(View view){
        final String CHANNEL_ID = "channel_id_1";
        final String CHANNEL_NAME = "channel_name_1";
        final NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Push")
                .setContentText("hello world")
                .setAutoCancel(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(notificationChannel);
            mNotificationManager.notify(1, builder.build());
        }else {
            mNotificationManager.notify(1, builder.build());
            Intent intent = new Intent(Intent.ACTION_DEFAULT);
            PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, 0);
            builder.setFullScreenIntent(pendingIntent, true);
            mNotificationManager.notify("closed",1, builder.build());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                        mNotificationManager.cancel("closed", 1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    public void init(){
        factory.setHost("192.168.0.102");// MQ的IP
        factory.setPort(5672);// MQ端口
        factory.setUsername("root");// MQ用户名
        factory.setPassword("root");// MQ密码
    }
    public void startListen(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Connection connection = factory.newConnection();
                    Channel channel = connection.createChannel();
                    channel.queueDeclare("GMessage", true, false, false, null);
                    Consumer consumer = new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            System.out.println(new String(body));
                        }
                    };
                    channel.basicConsume("GMessage", true, consumer);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
