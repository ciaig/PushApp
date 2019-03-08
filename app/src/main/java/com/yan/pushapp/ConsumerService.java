package com.yan.pushapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.TimeoutException;

public class ConsumerService extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    private static ConnectionFactory factory = new ConnectionFactory();

    private Connection connection;

    private Channel channel;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        startListen();
    }
    public void Push(String title,String content){
        final String CHANNEL_ID = "channel_id_1";
        final String CHANNEL_NAME = "channel_name_1";
        final NotificationManager mNotificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder= new NotificationCompat.Builder(this,CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.timg)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(notificationChannel);
            mNotificationManager.notify(LocalDateTime.now().getNano(), builder.build());
        }else {
            mNotificationManager.notify((int) (Math.random()*1000), builder.build());
            Intent intent = new Intent(this,MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
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
        factory.setHost("47.106.120.61");// MQ的IP
        factory.setPort(5672);// MQ端口
        factory.setUsername("admin");// MQ用户名
        factory.setPassword("admin");// MQ密码
//        factory.setHost("192.168.0.102");// MQ的IP
//        factory.setPort(5672);// MQ端口
//        factory.setUsername("root");// MQ用户名
//        factory.setPassword("root");// MQ密码
    }
    public void startListen(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connection = factory.newConnection();
                    channel = connection.createChannel();
                    channel.queueDeclare("GMessage", true, false, false, null);
                    Consumer consumer = new DefaultConsumer(channel){
                        @Override
                        public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                            Push("test",new String(body));
                        }
                    };
                    channel.basicConsume("GMessage", true, consumer);
                } catch (IOException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    channel.close();
                    connection.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }).start();
        super.onDestroy();
    }
}
