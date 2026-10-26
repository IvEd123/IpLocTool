package ru.ivedam.iploctool;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WidgetSizeTest {
    @Test public void narrowOrShortWidgetsUseCompactLayout() {
        assertEquals(WidgetSize.COMPACT, WidgetSize.select(110, 40));
        assertEquals(WidgetSize.COMPACT, WidgetSize.select(300, 70));
        assertEquals(WidgetSize.COMPACT, WidgetSize.select(150, 300));
    }
    @Test public void expandedWidgetsShowProvider() {
        assertEquals(WidgetSize.LARGE, WidgetSize.select(250, 180));
        assertEquals(WidgetSize.LARGE, WidgetSize.select(400, 300));
    }
    @Test public void mediumLayoutNeedsRoomForTextAndButton() {
        assertEquals(WidgetSize.MEDIUM, WidgetSize.select(170, 130));
        assertEquals(WidgetSize.MEDIUM, WidgetSize.select(249, 200));
        assertEquals(WidgetSize.MEDIUM, WidgetSize.select(350, 179));
    }
}
