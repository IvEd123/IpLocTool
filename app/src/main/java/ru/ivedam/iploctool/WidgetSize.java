package ru.ivedam.iploctool;

final class WidgetSize {
    static final int COMPACT = 0, MEDIUM = 1, LARGE = 2;
    static int select(int width, int height) {
        if (width < 170 || height < 130) return COMPACT;
        if (width >= 250 && height >= 180) return LARGE;
        return MEDIUM;
    }
    private WidgetSize() {}
}
