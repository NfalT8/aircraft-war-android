package edu.hitsz.music;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import edu.hitsz.R;

/**
 * 使用 SoundPool 播放短促的游戏音效。
 */
public class MySoundPool {

    private final SoundPool soundPool;
    private final int bulletHitSoundId;
    private final int bombExplosionSoundId;
    private final int getSupplySoundId;
    private final int gameOverSoundId;

    private boolean enabled = true;
    private boolean released = false;

    public MySoundPool(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(8)
                .setAudioAttributes(attributes)
                .build();

        bulletHitSoundId = soundPool.load(context, R.raw.bullet_hit, 1);
        bombExplosionSoundId = soundPool.load(context, R.raw.bomb_explosion, 1);
        getSupplySoundId = soundPool.load(context, R.raw.get_supply, 1);
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1);
    }

    public void playBulletHit() {
        play(bulletHitSoundId);
    }

    public void playBombExplosion() {
        play(bombExplosionSoundId);
    }

    public void playGetSupply() {
        play(getSupplySoundId);
    }

    public void playGameOver() {
        play(gameOverSoundId);
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

    private synchronized void play(int soundId) {
        if (released || !enabled || soundId == 0) {
            return;
        }
        soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
