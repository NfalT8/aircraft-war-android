package edu.hitsz.aircraft;

import android.os.SystemClock;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.strategy.ShootStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有种类飞机的抽象父类
 * @author hitsz
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {

    //难度系数倍率（静态变量，所有敌机共享）
    protected static double hpMultiplier = 1.0;   // 血量倍率
    protected static double speedMultiplier = 1.0; // 速度倍率

    //最大生命值
    protected int maxHp;
    protected int hp;
    protected int power;
    protected boolean destroyedByBomb = false; // 默认不是被炸弹炸死的
    private long hitFlashUntilMs = 0L;
    private float hpTrailRatio;
    private long hpTrailHoldUntilMs = 0L;

    private static final long HP_TRAIL_HOLD_MS = 180L;
    private static final float HP_TRAIL_SHRINK_PER_SEC = 1.6f;

    public void triggerHitFlash(long durationMs) {
        hitFlashUntilMs = SystemClock.elapsedRealtime() + durationMs;
    }

    public boolean isHitFlashing() {
        return SystemClock.elapsedRealtime() < hitFlashUntilMs;
    }

    public float getHpRatio() {
        if (maxHp <= 0) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, hp / (float) maxHp));
    }

    public float getHpTrailRatio() {
        return hpTrailRatio;
    }

    public void updateHpTrail(float dtSec) {
        float currentRatio = getHpRatio();
        if (hpTrailRatio <= currentRatio) {
            hpTrailRatio = currentRatio;
            return;
        }
        if (SystemClock.elapsedRealtime() >= hpTrailHoldUntilMs) {
            hpTrailRatio = Math.max(currentRatio, hpTrailRatio - HP_TRAIL_SHRINK_PER_SEC * dtSec);
        }
    }

    public boolean isDestroyedByBomb() {
        return destroyedByBomb;
    }

    public void setDestroyedByBomb(boolean destroyedByBomb) {
        this.destroyedByBomb = destroyedByBomb;
    }

    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.power = power;
        this.maxHp = hp;
        this.hpTrailRatio = maxHp > 0 ? Math.max(0f, Math.min(1f, hp / (float) maxHp)) : 0f;
    }

    public int getPower(){
        return this.power;
    }

    public void decreaseHp(int decrease){
        float prevRatio = getHpRatio();
        this.hp -= decrease;
        if(this.hp <= 0){
            this.hp=0;
            vanish();
        }
        if (decrease > 0) {
            if (hpTrailRatio < prevRatio) {
                hpTrailRatio = prevRatio;
            }
            hpTrailHoldUntilMs = SystemClock.elapsedRealtime() + HP_TRAIL_HOLD_MS;
        }
    }

    public void increaseHp(int increase){
        this.hp += increase;
        float ratio = getHpRatio();
        if (hpTrailRatio < ratio) {
            hpTrailRatio = ratio;
        }
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public static void setHpMultiplier(double multiplier) {
        hpMultiplier = multiplier;
    }

    public static double getHpMultiplier() {
        return hpMultiplier;
    }

    public static void setSpeedMultiplier(double multiplier) {
        speedMultiplier = multiplier;
    }

    public static double getSpeedMultiplier() {
        return speedMultiplier;
    }

    protected ShootStrategy strategy;

    public void setStrategy(ShootStrategy strategy){
        this.strategy = strategy;
    }
    /**
     * 飞机射击方法：将本帧子弹追加到已有列表，避免每次射击创建中间列表。
     */
    public void shootInto(List<BaseBullet> out) {
        if (this.strategy != null) {
            this.strategy.shootInto(this, out);
        }
    }

    public List<BaseBullet> shoot() {
        List<BaseBullet> out = new ArrayList<>();
        shootInto(out);
        return out;
    }

}