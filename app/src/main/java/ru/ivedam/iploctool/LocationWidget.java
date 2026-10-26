package ru.ivedam.iploctool;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.SizeF;
import android.widget.RemoteViews;
import java.util.LinkedHashMap;
import java.util.Map;

public class LocationWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) { renderAll(context); }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                   int id, Bundle options) { renderAll(context); }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        // Allow old already-installed widget PendingIntents to use the new execution path too.
        if ("ru.ivedam.iploctool.CHECK".equals(intent.getAction())) CheckService.start(context);
    }

    static void renderAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        Class<?>[] providers = {LocationWidget.class, CompactLocationWidget.class, LargeLocationWidget.class};
        for (Class<?> provider : providers) {
            for (int id : manager.getAppWidgetIds(new ComponentName(context, provider))) {
                RemoteViews views;
                if (Build.VERSION.SDK_INT >= 31) {
                    Map<SizeF, RemoteViews> sizes = new LinkedHashMap<>();
                    sizes.put(new SizeF(110, 60), createViews(context, WidgetSize.COMPACT));
                    sizes.put(new SizeF(170, 130), createViews(context, WidgetSize.MEDIUM));
                    sizes.put(new SizeF(250, 180), createViews(context, WidgetSize.LARGE));
                    views = new RemoteViews(sizes);
                } else {
                    Bundle options = manager.getAppWidgetOptions(id);
                    boolean portrait = context.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE;
                    int width = options.getInt(portrait ? AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH : AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
                    int height = options.getInt(portrait ? AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT : AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
                    int size = width > 0 && height > 0 ? WidgetSize.select(width, height)
                            : provider == CompactLocationWidget.class ? WidgetSize.COMPACT
                            : provider == LargeLocationWidget.class ? WidgetSize.LARGE : WidgetSize.MEDIUM;
                    views = createViews(context, size);
                }
                manager.updateAppWidget(id, views);
            }
        }
    }

    static RemoteViews createViews(Context context, int size) {
        int layout = size == WidgetSize.COMPACT ? R.layout.widget_compact
                : size == WidgetSize.LARGE ? R.layout.widget_large : R.layout.widget_loading;
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        SharedPreferences state = CheckController.state(context);
        boolean busy = CheckController.isRunning();
        long time = state.getLong("checked_at", 0);
        String status = busy ? "Проверяем…" : state.getString("error", "");
        if (status.isEmpty()) status = time == 0 ? "Нажмите ↻ для проверки"
                : "Проверено: " + DateFormat.format("dd.MM HH:mm", time);
        views.setTextViewText(R.id.widget_ip, state.getString("ip", "IP —"));
        views.setTextViewText(R.id.widget_place, state.getString("place", "Местоположение —"));
        views.setTextViewText(R.id.widget_status, status);
        if (size == WidgetSize.LARGE) views.setTextViewText(R.id.widget_provider, state.getString("provider", ""));
        views.setTextViewText(R.id.widget_check, size == WidgetSize.COMPACT ? (busy ? "…" : "↻")
                : busy ? "Проверяем…" : "Проверить IP");
        // Direct user interaction starts a bounded foreground operation, without a receiver's network limits.
        views.setOnClickPendingIntent(R.id.widget_check, CheckService.pendingIntent(context));
        views.setOnClickPendingIntent(R.id.widget_info, PendingIntent.getActivity(context, 1,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        return views;
    }
}
