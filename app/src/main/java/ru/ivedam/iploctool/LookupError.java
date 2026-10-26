package ru.ivedam.iploctool;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

final class LookupError {
    static String message(Exception error) {
        if (error instanceof SocketTimeoutException) return "Превышено время ожидания сервиса. Повторите проверку.";
        if (error instanceof UnknownHostException) return "Не удалось найти ipwho.is. Проверьте интернет, DNS или VPN.";
        if (error instanceof SSLException) return "Ошибка защищённого соединения. Проверьте дату телефона, VPN или фильтрацию HTTPS.";
        if (error instanceof java.io.IOException && error.getMessage() != null
                && (error.getMessage().startsWith("Сервис") || error.getMessage().startsWith("Лимит"))) {
            return error.getMessage();
        }
        if (error instanceof org.json.JSONException) return "Сервис вернул некорректный ответ. Повторите позже.";
        return "Нет соединения с ipwho.is. Проверьте интернет, VPN и доступ приложения к сети.";
    }
    private LookupError() {}
}
