package com.ta.nearby;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

public class NearbyScannerService extends Service {
    private static final String TAG = "NotificationUtils";
    private static final String NEARBY_CHANNEL_ID = "Nearby Channel Id";
    private static final int NEARBY_NOTIFICATION_ID = 101;

    private static NotificationManager mNotificationManager;
    Handler mNearbyHandler = null;
    Runnable mNearbyHandlerRunnable = new Runnable() {
        @Override
        public void run() {
            mNearbyHandler.postDelayed(this, 5000);
        }
    };
    private NotificationCompat.Builder mNotificationCompatBuilder;

    public NearbyScannerService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNearbyHandler = new Handler();
        createNotification();
        startForeground(NEARBY_NOTIFICATION_ID, mNotificationCompatBuilder.build());
        NearbyUtils.startNearby(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mNearbyHandler.post(mNearbyHandlerRunnable);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotification() {
        PendingIntent nearbyPendingIntent = PendingIntent.getActivity(this, NEARBY_NOTIFICATION_ID, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel(this);
        mNotificationCompatBuilder = new NotificationCompat.Builder(this, NEARBY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Nearby Notification")
                .setContentText("Collecting Nearby devices data")
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setContentIntent(nearbyPendingIntent)
                .setLocalOnly(true)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.nearby_channel_name);
            String description = context.getString(R.string.nearby_channel_description);
            int importance = NotificationManager.IMPORTANCE_MIN;
            NotificationChannel notificationChannel = new NotificationChannel(NEARBY_CHANNEL_ID, name, importance);
            notificationChannel.enableLights(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                notificationChannel.setAllowBubbles(false);
            }
            notificationChannel.setDescription(description);
            // Register the notificationChannel with the system; you can't change the importance
            // or other notification behaviors after this
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNearbyHandler.removeCallbacks(mNearbyHandlerRunnable);
        mNearbyHandler = null;
        stopForeground(true);
    }
}
