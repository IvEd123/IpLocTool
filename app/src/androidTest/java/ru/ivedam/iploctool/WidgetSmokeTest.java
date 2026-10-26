package ru.ivedam.iploctool;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WidgetSmokeTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    @Before public void clearState() { CheckController.state(context).edit().clear().commit(); }

    @Test public void allLayoutsInflateAndKeepRefreshButtonInsideBounds() {
        CheckController.state(context).edit().putString("ip", "2001:4860:4860::8888")
                .putString("place", "🇷🇺 Санкт-Петербург, Россия").putString("provider", "Test provider").commit();
        instrumentation.runOnMainSync(() -> {
            int[][] dimensions = {{WidgetSize.COMPACT, 160, 60}, {WidgetSize.MEDIUM, 200, 150}, {WidgetSize.LARGE, 300, 240}};
            float density = context.getResources().getDisplayMetrics().density;
            for (int[] item : dimensions) {
                View view = LocationWidget.createViews(context, item[0]).apply(context, new FrameLayout(context));
                int width = (int) (item[1] * density), height = (int) (item[2] * density);
                view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
                view.layout(0, 0, width, height);
                View button = view.findViewById(R.id.widget_check);
                assertTrue(button.hasOnClickListeners());
                assertTrue(button.getWidth() > 0 && button.getHeight() > 0);
                assertTrue(button.getRight() <= width && button.getBottom() <= height);
                assertEquals("2001:4860:4860::8888", ((TextView) view.findViewById(R.id.widget_ip)).getText().toString());
            }
        });
        // Rendering/resizing alone must not initiate any lookup.
        assertFalse(CheckController.isRunning());
        assertEquals(0, CheckController.state(context).getLong("revision", 0));
    }

    @Test public void widgetUsesDirectForegroundServiceIntent() {
        if (Build.VERSION.SDK_INT >= 31) assertTrue(CheckService.pendingIntent(context).isForegroundService());
    }

    @Test public void buttonStartedServiceCompletesAndStopsItself() throws Exception {
        Activity activity = instrumentation.startActivitySync(new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.runOnMainSync(() -> {
                // Same PendingIntent delivered by the home screen refresh button.
                try { CheckService.pendingIntent(context).send(); }
                catch (android.app.PendingIntent.CanceledException error) { throw new AssertionError(error); }
            });
            long deadline = SystemClock.elapsedRealtime() + 30000;
            boolean completed = false;
            while (SystemClock.elapsedRealtime() < deadline) {
                boolean hasResult = CheckController.state(context).contains("ip") || CheckController.state(context).contains("error");
                if (hasResult && !CheckController.isRunning() && !isServiceRunning()) { completed = true; break; }
                SystemClock.sleep(100);
            }
            assertTrue("Manual widget check must finish and stop its service", completed);
            assertTrue("PendingIntent must actually execute CheckController", CheckController.state(context).getLong("revision", 0) != 0);
            // A temporary external API outage is valid; service/permission failure is not.
            assertFalse(CheckController.state(context).getString("error", "").contains("Android запретил"));
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isServiceRunning() {
        for (ActivityManager.RunningServiceInfo service : context.getSystemService(ActivityManager.class).getRunningServices(100)) {
            if (service.service.getClassName().equals(CheckService.class.getName())) return true;
        }
        return false;
    }
}
