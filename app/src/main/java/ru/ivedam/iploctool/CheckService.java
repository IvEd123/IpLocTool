package ru.ivedam.iploctool;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/** User-started, short-lived network operation. Never restarted or scheduled. */
public class CheckService extends Service {
    private static final String CHANNEL = "manual_ip_check";
    private boolean checking;

    static Intent intent(Context context) { return new Intent(context, CheckService.class); }

    static PendingIntent pendingIntent(Context context) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        return Build.VERSION.SDK_INT >= 26
                ? PendingIntent.getForegroundService(context, 2, intent(context), flags)
                : PendingIntent.getService(context, 2, intent(context), flags);
    }

    static void start(Context context) {
        try {
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent(context));
            else context.startService(intent(context));
        } catch (RuntimeException error) {
            CheckController.state(context).edit().putString("error",
                    "Android запретил запуск проверки. Откройте приложение и повторите.").apply();
            LocationWidget.renderAll(context);
            android.util.Log.w("IpLocTool", "Unable to start manual check", error);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Ручная проверка IP", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Только на время проверки по нажатию кнопки");
            manager.createNotificationChannel(channel);
        }
        Notification.Builder notification = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        notification.setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Проверяем IP")
                .setContentText("Уведомление исчезнет после завершения проверки")
                .setOngoing(true).setOnlyAlertOnce(true)
                .setContentIntent(PendingIntent.getActivity(this, 1,
                        new Intent(this, MainActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        startForeground(100, notification.build());
        if (!checking) {
            checking = true;
            CheckController.check(this, () -> new Handler(Looper.getMainLooper()).post(() -> {
                stopForeground(true);
                stopSelf();
            }));
        }
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
