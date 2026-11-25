package edu.hitsz.ui;

import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/**
 * 轻量触觉震动封装：为精英击杀、受击与炸弹/Boss 击破提供分级短震反馈。
 */
public class GameHaptics {

    private static final long MIN_INTERVAL_MS = 45L;

    private final Vibrator vibrator;
    private long lastVibrateTimeMs = 0L;

    public GameHaptics(Context context) {
        Context appContext = context.getApplicationContext();
        Vibrator resolved = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager =
                    (VibratorManager) appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (manager != null) {
                resolved = manager.getDefaultVibrator();
            }
        } else {
            resolved = (Vibrator) appContext.getSystemService(Context.VIBRATOR_SERVICE);
        }
        this.vibrator = (resolved != null && resolved.hasVibrator()) ? resolved : null;
    }

    public void vibrateLight() {
        vibrate(18L, 75);
    }

    public void vibrateMedium() {
        vibrate(34L, 155);
    }

    public void vibrateHeavy() {
        vibrate(68L, 235);
    }

    private void vibrate(long durationMs, int amplitude) {
        if (vibrator == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        if (now - lastVibrateTimeMs < MIN_INTERVAL_MS) {
            return;
        }
        lastVibrateTimeMs = now;
        try {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, amplitude));
        } catch (Exception ignored) {
            // 部分设备/模拟器无震动权限或硬件时安全降级
        }
    }
}
