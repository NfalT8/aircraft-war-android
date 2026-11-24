package edu.hitsz.application;

/**
 * 游戏逻辑分辨率。按屏幕实际宽高比自适应铺满全屏，保持素材等比缩放无黑边。
 */
public final class GameConfig {

    public static final int BASE_WIDTH = 512;
    public static final int BASE_HEIGHT = 768;

    public static volatile int WINDOW_WIDTH = BASE_WIDTH;
    public static volatile int WINDOW_HEIGHT = BASE_HEIGHT;

    private GameConfig() {
    }

    public static void updateSize(int width, int height) {
        WINDOW_WIDTH = Math.max(1, width);
        WINDOW_HEIGHT = Math.max(1, height);
    }
}
