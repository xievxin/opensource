package com.xx.fasksdk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        Notification notification = new Notification.Builder(MainActivity.this)
//                .setSmallIcon(R.drawable.ic_launcher_background)
//                .setContentText("texxxxxxt")
//                .setContentTitle("title")
//                .setTicker("ticker")
//                .build();
//        notification.flags |= Notification.FLAG_AUTO_CANCEL;
//        notification.defaults |= Notification.DEFAULT_ALL;
//        manager.notify(777, notification);

    }
}
