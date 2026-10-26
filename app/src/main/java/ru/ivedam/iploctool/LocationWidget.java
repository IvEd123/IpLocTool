package ru.ivedam.iploctool;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.format.DateFormat;
import android.widget.RemoteViews;

public class LocationWidget extends AppWidgetProvider {
    private static final String CHECK = "ru.ivedam.iploctool.CHECK";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        renderAll(context);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (CHECK.equals(intent.getAction())) {
            PendingResult pending = goAsync();
            CheckController.check(context, pending::finish);
        }
    }
    static void renderAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, LocationWidget.class));
        SharedPreferences state = CheckController.state(context);
        boolean busy = CheckController.isRunning();
        long time = state.getLong("checked_at", 0);
        String status = busy ? "Проверяем…" : state.getString("error", "");
        if (status.isEmpty()) status = time == 0 ? "Проверка только по кнопке"
                : "Проверено: " + DateFormat.format("dd.MM HH:mm", time);
        for (int id : ids) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_loading);
            views.setTextViewText(R.id.widget_ip, state.getString("ip", "IP —"));
            views.setTextViewText(R.id.widget_place, state.getString("place", "Местоположение —"));
            views.setTextViewText(R.id.widget_status, status);
            views.setTextViewText(R.id.widget_check, busy ? "Проверяем…" : "Проверить IP");
            // Keep the PendingIntent usable if Android kills the process mid-check.
            // The controller coalesces extra clicks while the process is alive.
            views.setBoolean(R.id.widget_check, "setEnabled", true);
            PendingIntent check = PendingIntent.getBroadcast(context, 0,
                    new Intent(context, LocationWidget.class).setAction(CHECK),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_check, check);
            PendingIntent open = PendingIntent.getActivity(context, 1,
                    new Intent(context, MainActivity.class),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_info, open);
            manager.updateAppWidget(id, views);
        }
    }
}
