package ru.ivedam.iploctool;

import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

final class Lookup {
    static final class Result {
        final String ip, place, provider;
        Result(String ip, String place, String provider) {
            this.ip = ip; this.place = place; this.provider = provider;
        }
    }

    static Result fetch() throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL("https://ipwho.is/?lang=ru").openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(false);
        // A user-started foreground service owns the request, not a broadcast receiver.
        AtomicBoolean expired = new AtomicBoolean();
        Timer deadline = new Timer("lookup-deadline", true);
        deadline.schedule(new TimerTask() {
            @Override public void run() { expired.set(true); connection.disconnect(); }
        }, 20000);
        try {
            int status = connection.getResponseCode();
            if (status == 429) throw new IOException("Лимит сервиса. Повторите позже.");
            if (status != 200) throw new IOException("Сервис недоступен (HTTP " + status + ").");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (output.size() + count > 32768) throw new IOException("Некорректный ответ сервиса.");
                    output.write(buffer, 0, count);
                }
                return parse(new String(output.toByteArray(), StandardCharsets.UTF_8));
            }
        } catch (Exception error) {
            if (expired.get()) throw new SocketTimeoutException("Lookup deadline exceeded");
            throw error;
        } finally {
            deadline.cancel();
            connection.disconnect();
        }
    }

    static Result parse(String body) throws Exception {
        JSONObject json = new JSONObject(body);
        if (!json.optBoolean("success", false)) throw new IOException("Сервис не смог определить IP.");
        String ip = json.optString("ip", "").trim();
        if (ip.isEmpty() || ip.length() > 45 || !ip.matches("[0-9a-fA-F:.]+")) {
            throw new IOException("Сервис вернул некорректный IP.");
        }
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        for (String key : new String[]{"city", "region", "country"}) {
            if (!json.isNull(key)) {
                String part = json.optString(key, "").trim();
                if (!part.isEmpty()) parts.add(part);
            }
        }
        StringBuilder joined = new StringBuilder();
        for (String part : parts) {
            if (joined.length() > 0) joined.append(", ");
            joined.append(part);
        }
        String place = joined.toString();
        if (place.isEmpty()) place = "Местоположение не определено";
        String code = json.optString("country_code", "").toUpperCase(Locale.ROOT);
        if (code.matches("[A-Z]{2}")) {
            String flag = new String(Character.toChars(0x1F1E6 + code.charAt(0) - 'A'))
                    + new String(Character.toChars(0x1F1E6 + code.charAt(1) - 'A'));
            place = flag + " " + place;
        }
        JSONObject network = json.optJSONObject("connection");
        return new Result(ip, place, network == null ? "" : network.optString("isp", ""));
    }
    private Lookup() {}
}
