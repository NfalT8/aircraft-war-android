package edu.hitsz.music;

import android.content.Context;
import android.media.MediaPlayer;

import edu.hitsz.R;

/**
 * 使用 MediaPlayer 播放游戏背景音乐与 Boss 背景音乐。
 */
public class MyMediaPlayer {

    private final MediaPlayer bgmPlayer;
    private final MediaPlayer bossBgmPlayer;

    private boolean enabled = true;
    private boolean bossActive = false;
    private boolean released = false;

    public MyMediaPlayer(Context context) {
        bgmPlayer = MediaPlayer.create(context, R.raw.bgm);
        if (bgmPlayer != null) {
            bgmPlayer.setLooping(true);
        }
        bossBgmPlayer = MediaPlayer.create(context, R.raw.bgm_boss);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.setLooping(true);
        }
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (released) {
            return;
        }
        if (enabled) {
            resumeCurrent();
        } else {
            pause(bgmPlayer);
            pause(bossBgmPlayer);
        }
    }

    public synchronized boolean isEnabled() {
        return enabled;
    }

    /**
     * 开始（或重新开始）普通背景音乐，同时关闭 Boss 音乐。
     */
    public synchronized void startGameBgm() {
        if (released) {
            return;
        }
        bossActive = false;
        pause(bossBgmPlayer);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.seekTo(0);
        }
        if (bgmPlayer != null) {
            bgmPlayer.seekTo(0);
            if (enabled) {
                bgmPlayer.start();
            }
        }
    }

    /**
     * Boss 出现：暂停普通 BGM，播放 Boss BGM。
     */
    public synchronized void playBossBgm() {
        if (released) {
            return;
        }
        bossActive = true;
        pause(bgmPlayer);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.seekTo(0);
            if (enabled) {
                bossBgmPlayer.start();
            }
        }
    }

    /**
     * Boss 被击败：停止 Boss BGM，恢复普通 BGM。
     */
    public synchronized void stopBossBgm() {
        if (released) {
            return;
        }
        bossActive = false;
        pause(bossBgmPlayer);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.seekTo(0);
        }
        if (enabled && bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    /**
     * 游戏结束：暂停全部背景音乐并回到开头。
     */
    public synchronized void stopAllAndReset() {
        if (released) {
            return;
        }
        bossActive = false;
        pause(bgmPlayer);
        pause(bossBgmPlayer);
        if (bgmPlayer != null) {
            bgmPlayer.seekTo(0);
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.seekTo(0);
        }
    }

    /**
     * Activity 回到前台时恢复当前应有的背景音乐。
     */
    public synchronized void resume() {
        if (released || !enabled) {
            return;
        }
        resumeCurrent();
    }

    /**
     * Activity 进入后台时暂停音乐，但保留播放位置。
     */
    public synchronized void pause() {
        if (released) {
            return;
        }
        pause(bgmPlayer);
        pause(bossBgmPlayer);
    }

    public synchronized void release() {
        if (released) {
            return;
        }
        released = true;
        if (bgmPlayer != null) {
            bgmPlayer.release();
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.release();
        }
    }

    private void resumeCurrent() {
        if (bossActive) {
            if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
                bossBgmPlayer.start();
            }
        } else if (bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    private void pause(MediaPlayer player) {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }
}
