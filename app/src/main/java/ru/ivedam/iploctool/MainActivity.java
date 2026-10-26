package ru.ivedam.iploctool;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private TextView ip, place, provider, status;
    private Button check;
    private SharedPreferences state;

    @Override public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main_root);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int top, bottom, left, right;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                top = bars.top; bottom = bars.bottom; left = bars.left; right = bars.right;
            } else {
                top = insets.getSystemWindowInsetTop(); bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft(); right = insets.getSystemWindowInsetRight();
            }
            view.setPadding(left, top, right, bottom);
            return insets;
        });
        ip = findViewById(R.id.ip);
        place = findViewById(R.id.place);
        provider = findViewById(R.id.provider);
        status = findViewById(R.id.status);
        check = findViewById(R.id.check);
        state = CheckController.state(this);
        check.setOnClickListener(v -> {
            CheckController.check(this, null);
            render();
        });
        Button widget = findViewById(R.id.add_widget);
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        boolean canPin = Build.VERSION.SDK_INT >= 26 && manager.isRequestPinAppWidgetSupported();
        widget.setVisibility(canPin ? View.VISIBLE : View.GONE);
        widget.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 26) manager.requestPinAppWidget(new ComponentName(this, LocationWidget.class), null, null);
        });
    }
    @Override protected void onStart() {
        super.onStart();
        state.registerOnSharedPreferenceChangeListener(this);
        render();
    }
    @Override protected void onStop() {
        state.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }
    @Override public void onSharedPreferenceChanged(SharedPreferences preferences, String key) { render(); }
    private void render() {
        boolean busy = CheckController.isRunning();
        ip.setText(state.getString("ip", "—"));
        place.setText(state.getString("place", "Местоположение пока неизвестно"));
        provider.setText(state.getString("provider", ""));
        long time = state.getLong("checked_at", 0);
        String message = busy ? "Проверяем IP и местоположение…" : state.getString("error", "");
        if (message.isEmpty()) message = time == 0 ? "Нажмите кнопку для первой проверки"
                : "Последняя проверка: " + DateFormat.format("dd.MM.yyyy HH:mm", time);
        status.setText(message);
        check.setEnabled(!busy);
        check.setText(busy ? "Проверяем…" : "Проверить IP");
    }
}
