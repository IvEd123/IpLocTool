package ru.ivedam.iploctool;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class LookupTest {
    @Test public void parsesActualFlagObjectAndDeduplicatesCityRegion() throws Exception {
        Lookup.Result value = Lookup.parse("{\"success\":true,\"ip\":\"8.8.8.8\",\"city\":\"Mountain View\",\"region\":\"Mountain View\",\"country\":\"United States\",\"country_code\":\"US\",\"flag\":{\"emoji\":\"🇺🇸\"},\"connection\":{\"isp\":\"Google\"}}");
        assertEquals("8.8.8.8", value.ip);
        assertEquals("🇺🇸 Mountain View, United States", value.place);
        assertEquals("Google", value.provider);
    }
    @Test public void acceptsIpv6AndUnknownLocation() throws Exception {
        Lookup.Result value = Lookup.parse("{\"success\":true,\"ip\":\"2001:4860:4860::8888\",\"country\":null}");
        assertEquals("2001:4860:4860::8888", value.ip);
        assertEquals("Местоположение не определено", value.place);
        assertEquals("", value.provider);
    }
    @Test(expected = IOException.class) public void rejectsServiceFailure() throws Exception {
        Lookup.parse("{\"success\":false,\"message\":\"reserved range\"}");
    }
    @Test(expected = IOException.class) public void rejectsMissingIp() throws Exception {
        Lookup.parse("{\"success\":true}");
    }
    @Test(expected = IOException.class) public void rejectsMalformedIp() throws Exception {
        Lookup.parse("{\"success\":true,\"ip\":\"not an IP\"}");
    }
    @Test public void missingFlagDoesNotBreakLocation() throws Exception {
        assertEquals("Paris, France", Lookup.parse("{\"success\":true,\"ip\":\"1.1.1.1\",\"city\":\"Paris\",\"country\":\"France\"}").place);
    }
}
