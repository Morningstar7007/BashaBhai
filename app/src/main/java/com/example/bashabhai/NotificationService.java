package com.example.bashabhai;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "BashaBhaiAlerts";
    private DatabaseReference myNotificationsRef;
    private ChildEventListener childEventListener;
    private long startupTime;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String currentUid = FirebaseAuth.getInstance().getUid();
        if (currentUid != null) {
            myNotificationsRef = FirebaseDb.getRef("Notifications").child(currentUid);
            startupTime = System.currentTimeMillis();
            processQueuedNotifications();
        } else {
            stopSelf();
        }
        return START_STICKY;
    }

    private void processQueuedNotifications() {
        if (myNotificationsRef == null) return;
        
        // 1. Single sweep for unread queued notifications (missed while offline/logged out)
        myNotificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot notifSnapshot : snapshot.getChildren()) {
                    AppNotification notification = notifSnapshot.getValue(AppNotification.class);
                    if (notification != null && !notification.isRead()) {
                        sendAndroidNotification(notification);
                        notifSnapshot.getRef().child("read").setValue(true);
                    }
                }
                // 2. After queue is processed, start listening for real-time live events
                listenForLiveNotifications();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // If it fails, fallback to just listening for new ones
                listenForLiveNotifications();
            }
        });
    }

    private void listenForLiveNotifications() {
        if (childEventListener == null && myNotificationsRef != null) {
            childEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    AppNotification notification = snapshot.getValue(AppNotification.class);
                    // Only trigger for brand NEW notifications arriving after the service started
                    if (notification != null && !notification.isRead() && notification.getTimestamp() >= startupTime) {
                        sendAndroidNotification(notification);
                        snapshot.getRef().child("read").setValue(true);
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {}

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            myNotificationsRef.addChildEventListener(childEventListener);
        }
    }

    private void sendAndroidNotification(AppNotification notification) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.bashabhai_logo) // Using app icon
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getMessage())
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notification.getId() != null ? notification.getId().hashCode() : (int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "BashaBhai Alerts";
            String description = "Important notifications about your properties and account";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myNotificationsRef != null && childEventListener != null) {
            myNotificationsRef.removeEventListener(childEventListener);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
