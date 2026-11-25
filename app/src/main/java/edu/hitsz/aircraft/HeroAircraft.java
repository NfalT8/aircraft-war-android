package edu.hitsz.aircraft;

import android.os.SystemClock;

import edu.hitsz.application.GameConfig;
import edu.hitsz.application.ImageManager;
import edu.hitsz.strategy.RingShootStrategy;
import edu.hitsz.strategy.ShootStrategy;
import edu.hitsz.strategy.TieredHeroShootStrategy;

/**
 * 英雄飞机，游戏玩家操控。
 * 支持双轨制火力系统：Lv.1 ~ Lv.5 常驻阶梯火力 + 限时环射/超载覆盖态。
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    public static final int MIN_FIREPOWER_LEVEL = 1;
    public static final int MAX_FIREPOWER_LEVEL = 5;
    private static final long DOWNGRADE_PROTECT_MS = 1500L;

    private static volatile HeroAircraft instance = null;

    private final ShootStrategy baseStrategy = new TieredHeroShootStrategy();
    private final ShootStrategy ringStrategy = new RingShootStrategy();

    private int firepowerLevel = MIN_FIREPOWER_LEVEL;
    private long ringRemainingMs = 0L;
    private long overdriveRemainingMs = 0L;
    private long downgradeProtectUntilMs = 0L;

    private int energy = 0;
    private final int maxEnergy = 100;

    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = baseStrategy;
    }

    public void reset() {
        this.hp = 100;
        this.maxHp = 100;
        this.energy = 0;
        this.firepowerLevel = MIN_FIREPOWER_LEVEL;
        this.ringRemainingMs = 0L;
        this.overdriveRemainingMs = 0L;
        this.downgradeProtectUntilMs = 0L;
        this.isValid = true;
        this.strategy = baseStrategy;
        resetPosition();
    }

    public void resetPosition() {
        this.locationX = GameConfig.WINDOW_WIDTH / 2;
        this.locationY = GameConfig.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight();
    }

    public void clampToBounds() {
        this.locationX = Math.max(0, Math.min(GameConfig.WINDOW_WIDTH, this.locationX));
        this.locationY = Math.max(0, Math.min(GameConfig.WINDOW_HEIGHT, this.locationY));
    }

    public int getFirepowerLevel() {
        return firepowerLevel;
    }

    public boolean isMaxFirepower() {
        return firepowerLevel >= MAX_FIREPOWER_LEVEL;
    }

    /**
     * 提升 1 级常驻火力；若已达满级返回 false，由外部转为触发超载模式。
     */
    public boolean upgradeFirepower() {
        if (firepowerLevel < MAX_FIREPOWER_LEVEL) {
            firepowerLevel++;
            return true;
        }
        return false;
    }

    /**
     * 方案 A1：受击时降低 1 级常驻火力（最低保底 Lv.1），并附带 1.5s 降级保护冷却。
     * @return 是否实际发生了降级
     */
    public boolean downgradeFirepowerOnHit() {
        long now = SystemClock.elapsedRealtime();
        if (firepowerLevel <= MIN_FIREPOWER_LEVEL || now < downgradeProtectUntilMs) {
            return false;
        }
        firepowerLevel--;
        downgradeProtectUntilMs = now + DOWNGRADE_PROTECT_MS;
        return true;
    }

    /**
     * 开启限时环射覆盖态；倒计时结束后自动回落到当前常驻火力阶梯。
     */
    public void activateRingOverride(long durationMs) {
        if (durationMs <= 0L) {
            return;
        }
        ringRemainingMs = Math.max(ringRemainingMs, durationMs);
        this.strategy = ringStrategy;
    }

    /**
     * 开启满级溢出超载模式（大幅提升射速）。
     */
    public void activateOverdrive(long durationMs) {
        if (durationMs <= 0L) {
            return;
        }
        overdriveRemainingMs = Math.max(overdriveRemainingMs, durationMs);
    }

    public boolean isRingActive() {
        return ringRemainingMs > 0L;
    }

    public boolean isOverdriveActive() {
        return overdriveRemainingMs > 0L;
    }

    /**
     * 在固定步长游戏逻辑中推进限时状态，天然与暂停/重开保持一致，无需额外后台线程。
     */
    public void tickTimedEffects(long stepMs) {
        if (ringRemainingMs > 0L) {
            ringRemainingMs = Math.max(0L, ringRemainingMs - stepMs);
            if (ringRemainingMs == 0L) {
                this.strategy = baseStrategy;
            }
        }
        if (overdriveRemainingMs > 0L) {
            overdriveRemainingMs = Math.max(0L, overdriveRemainingMs - stepMs);
        }
    }

    public void addEnergy(int value) {
        energy = Math.min(energy + value, maxEnergy);
    }

    public void resetEnergy() {
        energy = 0;
    }

    public boolean isEnergyFull() {
        return energy >= maxEnergy;
    }

    public int getEnergy() {
        return energy;
    }

    public static HeroAircraft getInstance() {
        if (instance == null) {
            synchronized (HeroAircraft.class) {
                if (instance == null) {
                    int initX = GameConfig.WINDOW_WIDTH / 2;
                    int initY = GameConfig.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight();
                    int initSpeedX = 0;
                    int initSpeedY = 0;
                    int initHp = 100;
                    int initPower = 20;
                    instance = new HeroAircraft(initX, initY, initSpeedX, initSpeedY, initHp, initPower);
                }
            }
        }
        return instance;
    }

    @Override
    public void forward() {
        // 英雄机由触屏控制，不通过 forward 函数移动
    }
}
