package ru.ivedam.iploctool;

import org.junit.Test;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import static org.junit.Assert.*;

public class LookupErrorTest {
    @Test public void dnsAndTlsFailuresAreActionable() {
        assertTrue(LookupError.message(new UnknownHostException()).contains("DNS"));
        assertTrue(LookupError.message(new SSLHandshakeException("bad cert")).contains("дату"));
    }
    @Test public void timeoutsAndRateLimitsAreNotHiddenAsGenericErrors() {
        assertTrue(LookupError.message(new SocketTimeoutException()).contains("время ожидания"));
        assertEquals("Лимит сервиса. Повторите позже.", LookupError.message(new IOException("Лимит сервиса. Повторите позже.")));
    }
    @Test public void rawTransportDetailsAreNotShownToUser() {
        assertFalse(LookupError.message(new IOException("raw internal details")).contains("raw internal"));
    }
}
