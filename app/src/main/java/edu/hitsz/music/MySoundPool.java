package edu.hitsz.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

import java.util.Random;

import edu.hitsz.R;

/**
 * 使用 SoundPool 播放短促的游戏音效。
 * 对高频命中音效增加轻微音高变化与防爆音节流，提升连射听感。
 */
public class MySoundPool {

    private static final long BULLET_HIT_COOLDOWN_MS = 55L;

    private final SoundPool soundPool;
    private final int bulletHitSoundId;
    private final int bombExplosionSoundId;
    private final int getSupplySoundId;
    private final int gameOverSoundId;
    private final Random random = new Random();

    private boolean enabled = true;
    private boolean released = false;
    private long lastBulletHitTimeMs = 0L;

    public MySoundPool(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attributes)
                .build();

        bulletHitSoundId = soundPool.load(context, R.raw.bullet_hit, 1);
        bombExplosionSoundId = soundPool.load(context, R.raw.bomb_explosion, 1);
        getSupplySoundId = soundPool.load(context, R.raw.get_supply, 1);
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1);
    }

    public synchronized void playBulletHit() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBulletHitTimeMs < BULLET_HIT_COOLDOWN_MS) {
            return;
        }
        lastBulletHitTimeMs = now;
        float pitch = 0.92f + random.nextFloat() * 0.20f;
        playInternal(bulletHitSoundId, 0.65f, pitch);
    }

    public synchronized void playBombExplosion() {
        playInternal(bombExplosionSoundId, 1.0f, 1.0f);
    }

    public synchronized void playGetSupply() {
        playInternal(getSupplySoundId, 0.95f, 1.0f);
    }

    /**
     * 火力升级/超载音效：音高随火力阶数递升，增强成长正反馈。
     */
    public synchronized void playPowerUp(int tier) {
        float pitch = Math.min(1.38f, 0.94f + Math.max(0, tier - 1) * 0.085f);
        playInternal(getSupplySoundId, 1.0f, pitch);
    }

    public synchronized void playGameOver() {
        playInternal(gameOverSoundId, 1.0f, 1.0f);
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public synchronized void release() {
        if (released) {
            return;
        }
        released = true;
        soundPool.release();
    }

    private void playInternal(int soundId, float volume, float rate) {
        if (released || !enabled || soundId == 0) {
            return;
        }
        soundPool.play(soundId, volume, volume, 1, 0, rate);
    }
}
