package ru.ivedam.iploctool;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.concurrent.atomic.AtomicBoolean;

final class CheckController {
    private static final AtomicBoolean running = new AtomicBoolean();
    static SharedPreferences state(Context context) {
        return context.getSharedPreferences("last_check", Context.MODE_PRIVATE);
    }
    static boolean isRunning() { return running.get(); }

    // Only callers handling an explicit button press may invoke this method.
    static void check(Context context, Runnable finished) {
        Context app = context.getApplicationContext();
        if (!running.compareAndSet(false, true)) {
            if (finished != null) finished.run();
            return;
        }
        state(app).edit().putLong("revision", System.nanoTime()).commit();
        LocationWidget.renderAll(app);
        new Thread(() -> {
            try {
                Lookup.Result result = Lookup.fetch();
                state(app).edit().putString("ip", result.ip).putString("place", result.place)
                        .putString("provider", result.provider).putLong("checked_at", System.currentTimeMillis())
                        .remove("error").commit();
            } catch (Exception error) {
                String message = LookupError.message(error);
                android.util.Log.w("IpLocTool", "Manual IP check failed", error);
                state(app).edit().putString("error", message).commit();
            } finally {
                running.set(false);
                try {
                    LocationWidget.renderAll(app);
                    // Notify a visible Activity after running has been reset.
                    state(app).edit().putLong("revision", System.nanoTime()).commit();
                } finally {
                    if (finished != null) finished.run();
                }
            }
        }, "ip-check").start();
    }
    private CheckController() {}
}
