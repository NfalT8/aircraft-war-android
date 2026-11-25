package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.EliteProEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.factory.BossEnemyFactory;
import edu.hitsz.factory.EliteEnemyFactory;
import edu.hitsz.factory.ElitePlusEnemyFactory;
import edu.hitsz.factory.EliteProEnemyFactory;
import edu.hitsz.factory.EnemyFactory;
import edu.hitsz.factory.MobEnemyFactory;
import edu.hitsz.factory.PropFactory;
import edu.hitsz.music.MyMediaPlayer;
import edu.hitsz.music.MySoundPool;
import edu.hitsz.observer.GameObserver;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.BulletPlusProp;
import edu.hitsz.prop.BulletProp;
import edu.hitsz.prop.FreezeProp;
import edu.hitsz.ui.GameEffects;
import edu.hitsz.ui.GameHaptics;

import java.util.LinkedList;
import java.util.List;

/**
 * 游戏主视图：基于硬件加速 View + Choreographer VSync 高帧率（60/90/120Hz）插值渲染，
 * 固定 25Hz（40ms）逻辑步长与零延迟触屏响应，避免独立 SurfaceView 销毁时的 EGL 竞争闪退。
 */
public abstract class BaseGame extends View {

    public enum BuffType {
        SCATTER,
        RING,
        FREEZE
    }

    public interface UiListener {
        void onEnergyFullChanged(boolean full);

        void onBuffStarted(BuffType type, long durationMs);

        void onBuffsCleared();

        void onBossHpChanged(int hp, int maxHp);

        void onGameOver(int score);
    }

    private static final long LOGIC_STEP_MS = 40L;
    private static final long LOGIC_STEP_NANOS = 40_000_000L;
    private static final long MAX_FRAME_DELTA_NANOS = 200_000_000L;
    private static final int MAX_LOGIC_STEPS_PER_FRAME = 4;
    private static final long OVERDRIVE_DURATION_MS = 5000L;
    private static final int OVERDRIVE_BONUS_SCORE = 100;
    private static final int OVERDRIVE_BONUS_ENERGY = 15;

    private boolean attached = false;
    private boolean resumed = false;
    private boolean running = false;

    private long lastFrameTimeNanos = 0L;
    private long accumulatorNanos = 0L;
    private float renderAlpha = 1f;

    // 逻辑坐标到屏幕坐标的等比缩放系数与安全区偏移
    private float scale = 0f;
    private int topSafeInsetPx = 0;
    private boolean heroPositionedForSurface = false;
    private boolean pendingHeroResetPosition = false;
    private boolean pendingHeroClamp = false;

    private long bgTickCount = 0L;
    protected Bitmap backgroundImage;
    protected GameDifficulty difficulty;

    protected EnemyFactory mobEnemyFactory = new MobEnemyFactory();
    protected EnemyFactory eliteEnemyFactory = new EliteEnemyFactory();
    protected EnemyFactory elitePlusEnemyFactory = new ElitePlusEnemyFactory();
    protected EnemyFactory eliteProEnemyFactory = new EliteProEnemyFactory();

    protected double probElitePro = 0.05;
    protected double probElitePlus = 0.10;
    protected double probElite = 0.15;

    protected final HeroAircraft heroAircraft;
    protected final List<AbstractAircraft> enemyAircrafts;
    protected final List<BaseBullet> heroBullets;
    protected final List<BaseBullet> enemyBullets;
    protected final List<AbstractProp> props = new LinkedList<>();

    //屏幕中出现的敌机最大数量
    protected int enemyMaxNumber = 5;

    //敌机生成周期
    protected int enemySpawnCycle = 30;
    protected int enemySpawnCounter = 0;

    //英雄机和敌机射击周期
    protected double shootCycle = 6;
    protected double shootCounter = 0.0;

    protected double enemyShootCycle = 40;
    protected int enemyShootCounter = 0;

    //当前玩家分数
    protected int score = 0;

    protected int bossScore = 500;
    protected boolean bossExisted = false;
    protected EnemyFactory bossEnemyFactory = new BossEnemyFactory();

    //游戏结束标志
    protected volatile boolean gameOverFlag = false;
    private volatile boolean restartRequested = false;

    // 相对拖拽触屏位移（UI 线程累积，渲染线程每 VSync 帧消费）
    private final Object heroInputLock = new Object();
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private float lastTouchScreenX = 0f;
    private float lastTouchScreenY = 0f;
    private float pendingDeltaX = 0f;
    private float pendingDeltaY = 0f;

    // 英雄机亚像素位置与动态视觉状态（仅在渲染线程读写）
    private float heroExactX = 0f;
    private float heroExactY = 0f;
    private boolean heroExactInitialized = false;
    private float heroTiltDeg = 0f;
    private float heroVerticalBoost = 0f;
    private float muzzleFlashAlpha = 0f;
    private float thrusterPhase = 0f;

    private final MyMediaPlayer mediaPlayer;
    private final MySoundPool soundPool;
    private final GameEffects effects = new GameEffects();
    private final GameHaptics haptics;
    private boolean musicEnabled = true;

    //技能请求由 UI 线程写入，游戏线程消费
    private volatile boolean bombUltimateRequested = false;
    private volatile boolean healUltimateRequested = false;
    private volatile boolean ringUltimateRequested = false;

    private UiListener uiListener;
    private boolean lastEnergyFull = false;
    private int lastReportedBossHp = -1;
    private int lastReportedBossMaxHp = -1;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint hitFlashPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint heroDamagePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint heroBulletTierPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint heroBulletMaxPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint fxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path flamePath = new Path();
    private final RectF backgroundDest = new RectF();
    private final RectF healthBarRect = new RectF();
    private final Path healthBarClipPath = new Path();

    protected abstract void initDifficultyParams();

    protected abstract void increaseDifficultyWithTime();

    protected abstract boolean shouldGenerateBoss();

    protected abstract void generateBoss();

    /**
     * 重新开始一局时，由子类恢复各自难度相关的状态。
     */
    protected abstract void resetDifficultyState();

    public BaseGame(Context context) {
        super(context);
        setFocusable(true);

        Context appContext = context.getApplicationContext();
        mediaPlayer = new MyMediaPlayer(appContext);
        soundPool = new MySoundPool(appContext);
        haptics = new GameHaptics(appContext);

        hitFlashPaint.setColorFilter(
                new PorterDuffColorFilter(0xD8FFFFFF, PorterDuff.Mode.SRC_ATOP));
        heroDamagePaint.setColorFilter(
                new PorterDuffColorFilter(0xB8FF4757, PorterDuff.Mode.SRC_ATOP));
        heroBulletTierPaint.setColorFilter(
                new PorterDuffColorFilter(0x384DD0E1, PorterDuff.Mode.SRC_ATOP));
        heroBulletMaxPaint.setColorFilter(
                new PorterDuffColorFilter(0x66FFB300, PorterDuff.Mode.SRC_ATOP));

        heroAircraft = HeroAircraft.getInstance();
        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();

        // 重置难度倍率
        AbstractAircraft.setHpMultiplier(1.0);
        AbstractAircraft.setSpeedMultiplier(1.0);
        initDifficultyParams();
        loadBackground();
        heroAircraft.reset();
    }

    private void loadBackground() {
        if (difficulty == null) {
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        backgroundImage = BitmapFactory.decodeResource(
                getResources(), difficulty.getBackgroundId(), options);
    }

    public void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        mediaPlayer.setEnabled(enabled && !gameOverFlag);
        soundPool.setEnabled(enabled);
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void releaseAudio() {
        mediaPlayer.release();
        soundPool.release();
    }

    protected void onBossSpawned() {
        mediaPlayer.playBossBgm();
    }

    public void setUiListener(UiListener listener) {
        uiListener = listener;
        if (listener != null) {
            post(() -> listener.onEnergyFullChanged(heroAircraft.isEnergyFull()));
        }
    }

    public void requestBombUltimate() {
        bombUltimateRequested = true;
    }

    public void requestHealUltimate() {
        healUltimateRequested = true;
    }

    public void requestRingUltimate() {
        ringUltimateRequested = true;
    }

    public void restartGame() {
        if (gameOverFlag) {
            restartRequested = true;
        }
    }

    //***********************
    //      View 生命周期
    //***********************

    public void onResume() {
        resumed = true;
        if (!gameOverFlag) {
            mediaPlayer.resume();
        }
        updateLoopState();
    }

    public void onPause() {
        resumed = false;
        updateLoopState();
        mediaPlayer.pause();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        attached = true;
        updateLoopState();
    }

    @Override
    protected void onDetachedFromWindow() {
        attached = false;
        updateLoopState();
        mediaPlayer.pause();
        super.onDetachedFromWindow();
    }

    private void updateLoopState() {
        boolean shouldRun = attached && resumed;
        if (shouldRun && !running) {
            running = true;
            lastFrameTimeNanos = 0L;
            accumulatorNanos = 0L;
            Choreographer.getInstance().postFrameCallback(frameCallback);
        } else if (!shouldRun && running) {
            running = false;
            Choreographer.getInstance().removeFrameCallback(frameCallback);
        }
    }

    public void setTopSafeInsetPx(int topSafeInsetPx) {
        int clamped = Math.max(0, topSafeInsetPx);
        if (this.topSafeInsetPx != clamped) {
            this.topSafeInsetPx = clamped;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width <= 0 || height <= 0) {
            return;
        }
        float baseAspect = GameConfig.BASE_HEIGHT / (float) GameConfig.BASE_WIDTH;
        int logicalWidth;
        int logicalHeight;
        if (height / (float) width >= baseAspect) {
            logicalWidth = GameConfig.BASE_WIDTH;
            scale = width / (float) logicalWidth;
            logicalHeight = (int) Math.ceil(height / scale);
        } else {
            logicalHeight = GameConfig.BASE_HEIGHT;
            scale = height / (float) logicalHeight;
            logicalWidth = (int) Math.ceil(width / scale);
        }
        GameConfig.updateSize(logicalWidth, logicalHeight);
        if (!heroPositionedForSurface) {
            heroPositionedForSurface = true;
            pendingHeroResetPosition = true;
        } else {
            pendingHeroClamp = true;
        }
        applyPendingHeroMove(0f);
        invalidate();
    }

    //***********************
    //      游戏主循环
    //***********************

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            if (!running) {
                return;
            }
            Choreographer.getInstance().postFrameCallback(this);

            if (lastFrameTimeNanos == 0L) {
                lastFrameTimeNanos = frameTimeNanos;
            }
            long deltaNanos = frameTimeNanos - lastFrameTimeNanos;
            lastFrameTimeNanos = frameTimeNanos;
            if (deltaNanos < 0L) {
                deltaNanos = 0L;
            } else if (deltaNanos > MAX_FRAME_DELTA_NANOS) {
                deltaNanos = MAX_FRAME_DELTA_NANOS;
            }

            if (scale <= 0f) {
                return;
            }

            if (gameOverFlag && restartRequested) {
                restartRequested = false;
                accumulatorNanos = 0L;
                resetGame();
            }

            float dtSec = deltaNanos / 1_000_000_000f;

            // 英雄机位置与倾斜/尾焰每渲染帧立即更新，保证 60/120Hz 零延迟跟手
            applyPendingHeroMove(dtSec);

            if (!gameOverFlag) {
                accumulatorNanos += deltaNanos;
                int steps = 0;
                while (accumulatorNanos >= LOGIC_STEP_NANOS && steps < MAX_LOGIC_STEPS_PER_FRAME) {
                    action();
                    accumulatorNanos -= LOGIC_STEP_NANOS;
                    steps++;
                    if (gameOverFlag) {
                        accumulatorNanos = 0L;
                        break;
                    }
                }
                if (accumulatorNanos >= LOGIC_STEP_NANOS) {
                    accumulatorNanos %= LOGIC_STEP_NANOS;
                }
            }

            if (muzzleFlashAlpha > 0f) {
                muzzleFlashAlpha = Math.max(0f, muzzleFlashAlpha - dtSec * 14f);
            }
            thrusterPhase += dtSec * 36f;
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (isEliteEnemy(enemy)) {
                    enemy.updateHpTrail(dtSec);
                }
            }
            effects.update(dtSec);

            renderAlpha = gameOverFlag
                    ? 1f
                    : Math.max(0f, Math.min(1f, accumulatorNanos / (float) LOGIC_STEP_NANOS));
            invalidate();
        }
    };

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        drawFrame(canvas, renderAlpha);
    }

    private void action() {
        bgTickCount++;
        heroAircraft.tickTimedEffects(LOGIC_STEP_MS);
        processUltimateRequests();
        increaseDifficultyWithTime();

        generateRegularEnemies();

        if (shouldGenerateBoss()) {
            generateBoss();
        }

        shootAction();
        bulletsMoveAction();
        aircraftsMoveAction();
        propsMoveAction();
        crashCheckAction();
        postProcessAction();
        checkResultAction();
        updateEnergyUi();
        updateBossHpUi();
    }

    private void processUltimateRequests() {
        if (bombUltimateRequested) {
            bombUltimateRequested = false;
            releaseUltimate();
        }
        if (healUltimateRequested) {
            healUltimateRequested = false;
            healUltimate();
        }
        if (ringUltimateRequested) {
            ringUltimateRequested = false;
            ringShootUltimate();
        }
    }

    private void updateEnergyUi() {
        boolean full = heroAircraft.isEnergyFull();
        if (full == lastEnergyFull) {
            return;
        }
        lastEnergyFull = full;
        UiListener listener = uiListener;
        if (listener != null) {
            post(() -> listener.onEnergyFullChanged(full));
        }
    }

    private void updateBossHpUi() {
        int currentHp = -1;
        int currentMaxHp = -1;
        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy instanceof BossEnemy && !enemy.notValid()) {
                currentHp = enemy.getHp();
                currentMaxHp = enemy.getMaxHp();
                break;
            }
        }
        if (currentHp == lastReportedBossHp && currentMaxHp == lastReportedBossMaxHp) {
            return;
        }
        lastReportedBossHp = currentHp;
        lastReportedBossMaxHp = currentMaxHp;

        final int reportHp = Math.max(0, currentHp);
        final int reportMaxHp = Math.max(0, currentMaxHp);
        UiListener listener = uiListener;
        if (listener != null) {
            post(() -> listener.onBossHpChanged(reportHp, reportMaxHp));
        }
    }

    //***********************
    //      Action 各部分
    //***********************

    private void generateRegularEnemies() {
        enemySpawnCounter++;
        if (enemySpawnCounter >= enemySpawnCycle) {
            enemySpawnCounter = 0;
            if (enemyAircrafts.size() < enemyMaxNumber) {

                double randomType = Math.random();
                EnemyFactory currentFactory;

                if (randomType <= probElitePro) {
                    currentFactory = eliteProEnemyFactory;
                } else if (randomType <= probElitePro + probElitePlus) {
                    currentFactory = elitePlusEnemyFactory;
                } else if (randomType <= probElitePro + probElitePlus + probElite) {
                    currentFactory = eliteEnemyFactory;
                } else {
                    currentFactory = mobEnemyFactory;
                }

                enemyAircrafts.add(currentFactory.createEnemy());
            }
        }
    }

    private void shootAction() {
        double effectiveCycle = shootCycle;
        if (heroAircraft.isOverdriveActive()) {
            effectiveCycle = Math.max(2.5, shootCycle * 0.65);
        } else if (heroAircraft.isMaxFirepower()) {
            effectiveCycle = Math.max(3.5, shootCycle * 0.88);
        }

        shootCounter += 1.0;
        if (shootCounter >= effectiveCycle) {
            shootCounter -= effectiveCycle;
            //英雄机射击
            List<BaseBullet> fired = heroAircraft.shoot();
            if (!fired.isEmpty()) {
                heroBullets.addAll(fired);
                muzzleFlashAlpha = 1f;
            }
        }
        enemyShootCounter++;
        if (enemyShootCounter >= enemyShootCycle) {
            enemyShootCounter = 0;
            for (AbstractAircraft enemy : enemyAircrafts) {
                enemyBullets.addAll(enemy.shoot());
            }
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    private void dropProp(AbstractAircraft enemy) {
        String[] elitePlusPool = {"Blood", "Bullet", "BulletPlus", "Bomb"};
        String[] eliteProPool = {"Blood", "Bullet", "BulletPlus", "Freeze", "Bomb"};
        String[] elitePool = {"Blood", "Bullet", "BulletPlus"};
        String[] bossPool = {"Blood", "Bullet", "BulletPlus", "Freeze", "Bomb"};
        double dropChance = 0;
        String[] currentPool = null;
        if (enemy instanceof BossEnemy) {
            for (int i = 0; i < 3; i++) {
                int randomIndex = (int) (Math.random() * bossPool.length);
                String selectedType = bossPool[randomIndex];

                int offsetX = (i - 1) * 30;
                AbstractProp prop = PropFactory.createProp(
                        selectedType, enemy.getLocationX() + offsetX, enemy.getLocationY());

                if (prop != null) {
                    props.add(prop);
                }
            }
        }
        if (enemy instanceof EliteProEnemy) {
            dropChance = 0.8;
            currentPool = eliteProPool;
        } else if (enemy instanceof ElitePlusEnemy) {
            dropChance = 0.6;
            currentPool = elitePlusPool;
        } else if (enemy instanceof EliteEnemy) {
            dropChance = 0.5;
            currentPool = elitePool;
        }

        if (currentPool != null && currentPool != bossPool && Math.random() < dropChance) {
            int randomIndex = (int) (Math.random() * currentPool.length);
            String selectedType = currentPool[randomIndex];

            AbstractProp prop = PropFactory.createProp(
                    selectedType, enemy.getLocationX(), enemy.getLocationY());
            if (prop != null) {
                props.add(prop);
            }
        }
    }

    /**
     * 碰撞检测：
     * 1. 敌机攻击英雄
     * 2. 英雄攻击/撞击敌机
     * 3. 英雄获得补给
     */
    private void crashCheckAction() {
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet)) {
                soundPool.playBulletHit();
                heroAircraft.decreaseHp(bullet.getPower());
                heroAircraft.triggerHitFlash(140L);
                effects.spawnHitSparks(bullet.getLocationX(), bullet.getLocationY());
                effects.triggerHeroDamage();
                haptics.vibrateMedium();
                bullet.vanish();
                if (heroAircraft.getHp() > 0 && heroAircraft.downgradeFirepowerOnHit()) {
                    effects.spawnFloatingText(
                            heroAircraft.getLocationX(),
                            heroAircraft.getLocationY() - 44f,
                            "POWER DOWN · LV." + heroAircraft.getFirepowerLevel(),
                            0xFFFF6B6B,
                            16f);
                }
            }
        }
        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    // 已被其他子弹击毁的敌机，不再检测
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    soundPool.playBulletHit();
                    enemyAircraft.decreaseHp(bullet.getPower());
                    effects.spawnHitSparks(bullet.getLocationX(), bullet.getLocationY());
                    bullet.vanish();
                    if (enemyAircraft.notValid()) {
                        if (enemyAircraft instanceof BossEnemy) {
                            score += 100;
                            bossExisted = false;
                            mediaPlayer.stopBossBgm();
                            heroAircraft.addEnergy(15);
                            effects.spawnExplosion(enemyAircraft.getLocationX(),
                                    enemyAircraft.getLocationY(), GameEffects.EXPLOSION_BOSS);
                            haptics.vibrateHeavy();
                        } else if (enemyAircraft instanceof EliteProEnemy) {
                            score += 30;
                            heroAircraft.addEnergy(10);
                            effects.spawnExplosion(enemyAircraft.getLocationX(),
                                    enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                            haptics.vibrateLight();
                        } else if (enemyAircraft instanceof ElitePlusEnemy) {
                            score += 20;
                            heroAircraft.addEnergy(5);
                            effects.spawnExplosion(enemyAircraft.getLocationX(),
                                    enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                            haptics.vibrateLight();
                        } else if (enemyAircraft instanceof EliteEnemy) {
                            score += 10;
                            heroAircraft.addEnergy(3);
                            effects.spawnExplosion(enemyAircraft.getLocationX(),
                                    enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                            haptics.vibrateLight();
                        } else {
                            score += 5;
                            heroAircraft.addEnergy(2);
                            effects.spawnExplosion(enemyAircraft.getLocationX(),
                                    enemyAircraft.getLocationY(), GameEffects.EXPLOSION_SMALL);
                        }
                        dropProp(enemyAircraft);
                    } else {
                        enemyAircraft.triggerHitFlash(65L);
                    }
                }

                // 英雄机 与 敌机 相撞，均损毁
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    effects.spawnExplosion(enemyAircraft.getLocationX(),
                            enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                    effects.triggerHeroDamage();
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }

            if (heroAircraft.crash(prop) || prop.crash(heroAircraft)) {
                if (prop instanceof BloodProp) {
                    soundPool.playGetSupply();
                    heroAircraft.increaseHp(10);
                    effects.spawnPickupBurst(heroAircraft.getLocationX(),
                            heroAircraft.getLocationY(), 0xFF4CD964);
                    effects.spawnFloatingText(heroAircraft.getLocationX(),
                            heroAircraft.getLocationY() - 44f, "HP +10", 0xFF4CD964, 16f);
                    haptics.vibrateLight();
                } else if (prop instanceof BulletProp) {
                    if (heroAircraft.upgradeFirepower()) {
                        int lv = heroAircraft.getFirepowerLevel();
                        boolean reachedMax = heroAircraft.isMaxFirepower();
                        int burstColor = reachedMax
                                ? 0xFFFFB300
                                : (lv >= 3 ? 0xFF2EE59D : 0xFF4DD0E1);
                        String label = reachedMax ? "MAX POWER!" : ("POWER UP · LV." + lv);
                        soundPool.playPowerUp(lv);
                        effects.spawnPickupBurst(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY(), burstColor);
                        effects.spawnFloatingText(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY() - 46f,
                                label, burstColor, reachedMax ? 20f : 17f);
                        if (reachedMax) {
                            haptics.vibrateMedium();
                        } else {
                            haptics.vibrateLight();
                        }
                    } else {
                        // Lv.5 满级溢出：触发 5 秒超载模式 + 额外分数与能量
                        heroAircraft.activateOverdrive(OVERDRIVE_DURATION_MS);
                        score += OVERDRIVE_BONUS_SCORE;
                        heroAircraft.addEnergy(OVERDRIVE_BONUS_ENERGY);
                        notifyBuffStarted(BuffType.SCATTER, OVERDRIVE_DURATION_MS);
                        soundPool.playPowerUp(HeroAircraft.MAX_FIREPOWER_LEVEL + 1);
                        effects.spawnPickupBurst(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY(), 0xFFFFB300);
                        effects.spawnFloatingText(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY() - 48f,
                                "OVERDRIVE! +" + OVERDRIVE_BONUS_SCORE, 0xFFFFD54F, 20f);
                        haptics.vibrateMedium();
                    }
                } else if (prop instanceof BulletPlusProp) {
                    heroAircraft.activateRingOverride(5000L);
                    notifyBuffStarted(BuffType.RING, 5000L);
                    if (heroAircraft.upgradeFirepower()) {
                        int lv = heroAircraft.getFirepowerLevel();
                        soundPool.playPowerUp(lv);
                        String label = heroAircraft.isMaxFirepower()
                                ? "RING + MAX!"
                                : ("RING + LV." + lv);
                        effects.spawnFloatingText(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY() - 48f, label, 0xFF4DD0E1, 18f);
                    } else {
                        score += OVERDRIVE_BONUS_SCORE;
                        heroAircraft.addEnergy(OVERDRIVE_BONUS_ENERGY);
                        soundPool.playPowerUp(HeroAircraft.MAX_FIREPOWER_LEVEL + 1);
                        effects.spawnFloatingText(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY() - 48f,
                                "RING MAX! +" + OVERDRIVE_BONUS_SCORE, 0xFFFFD54F, 19f);
                    }
                    effects.spawnPickupBurst(heroAircraft.getLocationX(),
                            heroAircraft.getLocationY(), 0xFF4DD0E1);
                    haptics.vibrateMedium();
                } else if (prop instanceof FreezeProp) {
                    soundPool.playGetSupply();
                    FreezeProp freeze = (FreezeProp) prop;
                    for (AbstractAircraft enemy : enemyAircrafts) {
                        freeze.addObserver((GameObserver) enemy);
                    }
                    for (BaseBullet bullet : enemyBullets) {
                        freeze.addObserver((GameObserver) bullet);
                    }
                    freeze.notifyObservers();
                    notifyBuffStarted(BuffType.FREEZE, 5000);
                    effects.triggerFreezeOverlay(5000L);
                    effects.spawnPickupBurst(heroAircraft.getLocationX(),
                            heroAircraft.getLocationY(), 0xFF81D4FA);
                    haptics.vibrateLight();
                } else if (prop instanceof BombProp) {
                    soundPool.playBombExplosion();
                    BombProp bomb = (BombProp) prop;

                    for (AbstractAircraft enemy : enemyAircrafts) {
                        bomb.addObserver((GameObserver) enemy);
                    }
                    for (BaseBullet bullet : enemyBullets) {
                        bomb.addObserver((GameObserver) bullet);
                    }

                    bomb.notifyObservers();
                    effects.triggerBombBlast(heroAircraft.getLocationX(),
                            heroAircraft.getLocationY());
                    haptics.vibrateHeavy();
                }

                prop.vanish();
            }
        }
    }

    private void releaseUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();
            soundPool.playBombExplosion();

            BombProp virtualBomb = new BombProp(0, 0, 0, 0);
            for (AbstractAircraft enemy : enemyAircrafts) {
                virtualBomb.addObserver((GameObserver) enemy);
            }
            for (BaseBullet bullet : enemyBullets) {
                if (bullet instanceof GameObserver) {
                    virtualBomb.addObserver((GameObserver) bullet);
                }
            }

            virtualBomb.notifyObservers();
            effects.triggerBombBlast(heroAircraft.getLocationX(), heroAircraft.getLocationY());
            haptics.vibrateHeavy();
        }
    }

    private void healUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();
            heroAircraft.increaseHp(50);
            soundPool.playGetSupply();
            effects.spawnPickupBurst(heroAircraft.getLocationX(),
                    heroAircraft.getLocationY(), 0xFF4CD964);
            effects.spawnFloatingText(heroAircraft.getLocationX(),
                    heroAircraft.getLocationY() - 46f, "HEAL +50", 0xFF4CD964, 18f);
            haptics.vibrateLight();
        }
    }

    private void ringShootUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();
            heroAircraft.activateRingOverride(7000L);
            notifyBuffStarted(BuffType.RING, 7000L);
            soundPool.playPowerUp(HeroAircraft.MAX_FIREPOWER_LEVEL);
            effects.spawnPickupBurst(heroAircraft.getLocationX(),
                    heroAircraft.getLocationY(), 0xFF4DD0E1);
            effects.spawnFloatingText(heroAircraft.getLocationX(),
                    heroAircraft.getLocationY() - 48f, "RING BARRAGE!", 0xFF4DD0E1, 19f);
            haptics.vibrateMedium();
        }
    }

    private void notifyBuffStarted(BuffType type, long durationMs) {
        UiListener listener = uiListener;
        if (listener != null) {
            post(() -> listener.onBuffStarted(type, durationMs));
        }
    }

    /**
     * 后处理：
     * 1. 删除无效的子弹
     * 2. 删除无效的敌机
     * 3. 删除无效的道具
     */
    private void postProcessAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            if (enemyAircraft.notValid() && enemyAircraft.isDestroyedByBomb()) {
                if (enemyAircraft instanceof EliteProEnemy) {
                    score += 30;
                    heroAircraft.addEnergy(10);
                    effects.spawnExplosion(enemyAircraft.getLocationX(),
                            enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                } else if (enemyAircraft instanceof ElitePlusEnemy) {
                    score += 20;
                    heroAircraft.addEnergy(5);
                    effects.spawnExplosion(enemyAircraft.getLocationX(),
                            enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                } else if (enemyAircraft instanceof EliteEnemy) {
                    score += 10;
                    heroAircraft.addEnergy(3);
                    effects.spawnExplosion(enemyAircraft.getLocationX(),
                            enemyAircraft.getLocationY(), GameEffects.EXPLOSION_ELITE);
                } else {
                    score += 5;
                    heroAircraft.addEnergy(3);
                    effects.spawnExplosion(enemyAircraft.getLocationX(),
                            enemyAircraft.getLocationY(), GameEffects.EXPLOSION_SMALL);
                }
            }
        }
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractProp::notValid);
    }

    private void checkResultAction() {
        if (!gameOverFlag && heroAircraft.getHp() <= 0) {
            gameOverFlag = true;
            effects.spawnExplosion(heroAircraft.getLocationX(),
                    heroAircraft.getLocationY(), GameEffects.EXPLOSION_BOSS);
            effects.triggerHeroDamage();
            haptics.vibrateHeavy();
            mediaPlayer.stopAllAndReset();
            soundPool.playGameOver();

            UiListener listener = uiListener;
            if (listener != null) {
                final int finalScore = score;
                post(() -> listener.onGameOver(finalScore));
            }
        }
    }

    private void resetGame() {
        score = 0;
        bgTickCount = 0L;
        enemySpawnCounter = 0;
        shootCounter = 0;
        enemyShootCounter = 0;
        bossExisted = false;
        gameOverFlag = false;
        synchronized (heroInputLock) {
            pendingDeltaX = 0f;
            pendingDeltaY = 0f;
        }
        heroTiltDeg = 0f;
        heroVerticalBoost = 0f;
        muzzleFlashAlpha = 0f;
        effects.reset();
        bombUltimateRequested = false;
        healUltimateRequested = false;
        ringUltimateRequested = false;

        enemyAircrafts.clear();
        heroBullets.clear();
        enemyBullets.clear();
        props.clear();

        AbstractAircraft.setHpMultiplier(1.0);
        AbstractAircraft.setSpeedMultiplier(1.0);

        heroAircraft.reset();
        syncHeroExactFromAircraft();
        resetDifficultyState();
        loadBackground();

        mediaPlayer.setEnabled(musicEnabled);
        mediaPlayer.startGameBgm();
        soundPool.setEnabled(musicEnabled);

        lastEnergyFull = false;
        lastReportedBossHp = -1;
        lastReportedBossMaxHp = -1;
        UiListener listener = uiListener;
        if (listener != null) {
            post(() -> {
                listener.onEnergyFullChanged(false);
                listener.onBuffsCleared();
                listener.onBossHpChanged(0, 0);
            });
        }
    }

    //***********************
    //      Canvas 绘制
    //***********************

    private void drawFrame(Canvas canvas, float alpha) {
        if (scale <= 0f) {
            return;
        }
        canvas.drawColor(Color.BLACK);

        int saveCount = canvas.save();
        canvas.scale(scale, scale);

        // 世界层随屏幕震动偏移，HUD 与全屏滤镜保持稳定清晰
        int worldSave = canvas.save();
        canvas.translate(effects.getShakeOffsetX(), effects.getShakeOffsetY());

        // 绘制背景（按固定步长插值平滑滚动）
        drawBackground(canvas, alpha);

        // 先绘制子弹，后绘制飞机与道具（高帧率位置插值）
        paintImageWithPositionRevised(canvas, enemyBullets, alpha);
        paintHeroBullets(canvas, alpha);
        paintImageWithPositionRevised(canvas, enemyAircrafts, alpha);
        paintImageWithPositionRevised(canvas, props, alpha);
        drawEliteHealthBars(canvas, alpha);

        drawHeroWithEffects(canvas);

        // 世界层粒子与冲击波特效
        effects.drawWorldEffects(canvas);
        canvas.restoreToCount(worldSave);

        // 顶层状态滤镜（冰冻寒霜、受击红边、炸弹闪白）
        effects.drawScreenOverlays(canvas, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        //绘制得分和生命值
        paintScoreAndLife(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawHeroWithEffects(Canvas canvas) {
        Bitmap heroImage = ImageManager.HERO_IMAGE;
        if (heroImage == null) {
            return;
        }
        float drawX = heroExactInitialized ? heroExactX : heroAircraft.getLocationX();
        float drawY = heroExactInitialized ? heroExactY : heroAircraft.getLocationY();
        float halfW = heroImage.getWidth() / 2f;
        float halfH = heroImage.getHeight() / 2f;
        int firepowerLevel = heroAircraft.getFirepowerLevel();
        boolean overdrive = heroAircraft.isOverdriveActive();
        boolean maxTier = heroAircraft.isMaxFirepower() || overdrive;

        int heroSave = canvas.save();
        canvas.translate(drawX, drawY);
        canvas.rotate(heroTiltDeg);

        // 1. 双引擎动态推进尾焰（超载时呈炽金尾焰）
        if (!gameOverFlag) {
            float pulse = (float) Math.sin(thrusterPhase) * 3.2f
                    + (float) Math.sin(thrusterPhase * 2.3f) * 1.4f;
            float flameLen = (overdrive ? 22f : 17f) + pulse + heroVerticalBoost * 10f;
            float nozzleY = halfH - 5f;
            float nozzleOffsetX = Math.max(7f, halfW * 0.27f);
            int outerFlameColor = overdrive ? 0xD8FF9100 : 0xCC29B6F6;
            drawSingleFlame(canvas, -nozzleOffsetX, nozzleY, flameLen, outerFlameColor);
            drawSingleFlame(canvas, nozzleOffsetX, nozzleY, flameLen, outerFlameColor);
        }

        // 2. 英雄机本体（受击时短暂泛红闪烁）
        Paint heroPaint = (!gameOverFlag && heroAircraft.isHitFlashing())
                ? heroDamagePaint
                : bitmapPaint;
        canvas.drawBitmap(heroImage, -halfW, -halfH, heroPaint);

        // 3. 枪口开火微闪（随火力阶数联动单/双/三炮口与色调）
        if (!gameOverFlag && muzzleFlashAlpha > 0.02f) {
            float muzzleY = -halfH + 4f;
            int glowColor = maxTier
                    ? 0xFFFFB300
                    : (firepowerLevel >= 3 ? 0xFF2EE59D : 0xFF4DD0E1);
            if (firepowerLevel == 2 || firepowerLevel == 4) {
                drawMuzzleFlash(canvas, -11f, muzzleY + 4f, glowColor, 0.85f);
                drawMuzzleFlash(canvas, 11f, muzzleY + 4f, glowColor, 0.85f);
            } else if (firepowerLevel == 3 || firepowerLevel >= 5) {
                drawMuzzleFlash(canvas, 0f, muzzleY, glowColor, maxTier ? 1.15f : 1.0f);
                drawMuzzleFlash(canvas, -14f, muzzleY + 6f, glowColor, 0.72f);
                drawMuzzleFlash(canvas, 14f, muzzleY + 6f, glowColor, 0.72f);
            } else {
                drawMuzzleFlash(canvas, 0f, muzzleY, glowColor, 1.0f);
            }
        }

        canvas.restoreToCount(heroSave);
    }

    private void drawMuzzleFlash(Canvas canvas, float cx, float cy, int glowColor, float sizeScale) {
        fxPaint.setStyle(Paint.Style.FILL);
        fxPaint.setColor(glowColor);
        fxPaint.setAlpha(Math.round(135f * muzzleFlashAlpha));
        canvas.drawCircle(cx, cy, (10f + 5f * muzzleFlashAlpha) * sizeScale, fxPaint);

        fxPaint.setColor(0xFFFFF8D6);
        fxPaint.setAlpha(Math.round(230f * muzzleFlashAlpha));
        canvas.drawCircle(cx, cy, (4.5f + 2.5f * muzzleFlashAlpha) * sizeScale, fxPaint);
    }

    private void drawSingleFlame(Canvas canvas, float cx, float topY, float length, int outerColor) {
        fxPaint.setStyle(Paint.Style.FILL);

        // 外焰
        flamePath.reset();
        flamePath.moveTo(cx - 5.2f, topY);
        flamePath.lineTo(cx + 5.2f, topY);
        flamePath.lineTo(cx, topY + length);
        flamePath.close();
        fxPaint.setColor(outerColor);
        canvas.drawPath(flamePath, fxPaint);

        // 内焰（亮白金）
        flamePath.reset();
        flamePath.moveTo(cx - 2.6f, topY);
        flamePath.lineTo(cx + 2.6f, topY);
        flamePath.lineTo(cx, topY + length * 0.62f);
        flamePath.close();
        fxPaint.setColor(0xF5FFF8E1);
        canvas.drawPath(flamePath, fxPaint);
    }

    private void drawBackground(Canvas canvas, float alpha) {
        if (backgroundImage == null) {
            return;
        }
        int imgWidth = backgroundImage.getWidth();
        int imgHeight = backgroundImage.getHeight();
        if (imgWidth <= 0 || imgHeight <= 0) {
            return;
        }
        float tileWidth = GameConfig.WINDOW_WIDTH;
        float tileHeight = Math.max(1f, tileWidth * (imgHeight / (float) imgWidth));

        float logicalScroll = gameOverFlag ? (float) bgTickCount : (bgTickCount + alpha);
        float scrollOffset = logicalScroll % tileHeight;

        for (float y = scrollOffset - tileHeight; y < GameConfig.WINDOW_HEIGHT; y += tileHeight) {
            if (y + tileHeight <= 0f) {
                continue;
            }
            backgroundDest.set(0f, y, tileWidth, y + tileHeight + 0.5f);
            canvas.drawBitmap(backgroundImage, null, backgroundDest, bitmapPaint);
        }
    }

    private void paintImageWithPositionRevised(Canvas canvas,
                                               List<? extends AbstractFlyingObject> objects,
                                               float alpha) {
        for (AbstractFlyingObject object : objects) {
            Bitmap image = object.getImage();
            if (image == null) {
                continue;
            }
            Paint drawPaint = (object instanceof AbstractAircraft
                    && ((AbstractAircraft) object).isHitFlashing())
                    ? hitFlashPaint
                    : bitmapPaint;
            canvas.drawBitmap(image,
                    object.getInterpolatedX(alpha) - image.getWidth() / 2f,
                    object.getInterpolatedY(alpha) - image.getHeight() / 2f,
                    drawPaint);
        }
    }

    private void paintHeroBullets(Canvas canvas, float alpha) {
        Paint bulletPaint;
        if (heroAircraft.isOverdriveActive() || heroAircraft.isMaxFirepower()) {
            bulletPaint = heroBulletMaxPaint;
        } else if (heroAircraft.getFirepowerLevel() >= 3) {
            bulletPaint = heroBulletTierPaint;
        } else {
            bulletPaint = bitmapPaint;
        }

        for (BaseBullet bullet : heroBullets) {
            Bitmap image = bullet.getImage();
            if (image == null) {
                continue;
            }
            canvas.drawBitmap(image,
                    bullet.getInterpolatedX(alpha) - image.getWidth() / 2f,
                    bullet.getInterpolatedY(alpha) - image.getHeight() / 2f,
                    bulletPaint);
        }
    }

    private boolean isEliteEnemy(AbstractAircraft aircraft) {
        return aircraft instanceof EliteEnemy
                || aircraft instanceof ElitePlusEnemy
                || aircraft instanceof EliteProEnemy;
    }

    private void drawEliteHealthBars(Canvas canvas, float alpha) {
        final float barHeight = 6.5f;
        final float radius = barHeight * 0.5f;
        final float marginX = 4f;
        final float minTop = 4f;

        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy.notValid() || enemy.getHp() <= 0 || enemy.getMaxHp() <= 0
                    || !isEliteEnemy(enemy)) {
                continue;
            }
            Bitmap image = enemy.getImage();
            if (image == null) {
                continue;
            }

            float centerX = enemy.getInterpolatedX(alpha);
            float centerY = enemy.getInterpolatedY(alpha);
            float barWidth = Math.max(46f, Math.min(78f, image.getWidth() * 0.72f));
            float barLeft = centerX - barWidth * 0.5f;
            barLeft = Math.max(marginX, Math.min(GameConfig.WINDOW_WIDTH - barWidth - marginX, barLeft));
            float barTop = Math.max(minTop, centerY - image.getHeight() * 0.5f - barHeight - 6f);
            float barRight = barLeft + barWidth;
            float barBottom = barTop + barHeight;

            float hpRatio = enemy.getHpRatio();
            float trailRatio = Math.max(hpRatio, enemy.getHpTrailRatio());
            float fillRight = barLeft + barWidth * hpRatio;
            float trailRight = barLeft + barWidth * trailRatio;

            healthBarRect.set(barLeft, barTop, barRight, barBottom);

            // 1. 深色半透明底槽
            fxPaint.setStyle(Paint.Style.FILL);
            fxPaint.setColor(0xCC070C16);
            canvas.drawRoundRect(healthBarRect, radius, radius, fxPaint);

            // 2. 亮白延迟掉血残影 + 三段式血量填充
            healthBarClipPath.reset();
            healthBarClipPath.addRoundRect(healthBarRect, radius, radius, Path.Direction.CW);
            int clipSave = canvas.save();
            canvas.clipPath(healthBarClipPath);

            if (trailRight > fillRight) {
                fxPaint.setColor(0xF2FFFFFF);
                canvas.drawRect(fillRight, barTop, trailRight, barBottom, fxPaint);
            }

            if (fillRight > barLeft) {
                if (hpRatio > 0.60f) {
                    fxPaint.setColor(0xFF2EE59D);
                } else if (hpRatio > 0.25f) {
                    fxPaint.setColor(0xFFFFB830);
                } else {
                    fxPaint.setColor(0xFFFF4757);
                }
                canvas.drawRect(barLeft, barTop, fillRight, barBottom, fxPaint);
            }

            canvas.restoreToCount(clipSave);

            // 3. 外框细描边（受击瞬间微微高亮）
            fxPaint.setStyle(Paint.Style.STROKE);
            fxPaint.setStrokeWidth(1.0f);
            fxPaint.setColor(enemy.isHitFlashing() ? 0xEBFFFFFF : 0x66FFFFFF);
            canvas.drawRoundRect(healthBarRect, radius, radius, fxPaint);
        }
        fxPaint.setStyle(Paint.Style.FILL);
    }

    private void paintScoreAndLife(Canvas canvas) {
        final float left = 12f;
        final float topMarginPx = 30f * getResources().getDisplayMetrics().density;
        final float top = (topSafeInsetPx + topMarginPx) / scale;
        final float right = 268f;
        final float bottom = top + 140f;
        final float radius = 16f;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xB30B1220);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f);
        paint.setColor(0x40FFFFFF);
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTypeface(Typeface.DEFAULT_BOLD);

        float x = left + 16f;
        paint.setColor(0xFFFFD54F);
        paint.setTextSize(24f);
        canvas.drawText("SCORE  " + score, x, top + 32f, paint);

        paint.setColor(0xFFAFC6E8);
        paint.setTextSize(13f);
        canvas.drawText(difficulty.getDisplayName() + "  ·  生命 " + heroAircraft.getHp(),
                x, top + 56f, paint);

        float barTop = top + 68f;
        float barRight = right - 16f;
        float barBottom = barTop + 9f;
        paint.setColor(0x33FFFFFF);
        canvas.drawRoundRect(x, barTop, barRight, barBottom, 4.5f, 4.5f, paint);

        int energy = Math.min(heroAircraft.getEnergy(), 100);
        float fillRight = x + (barRight - x) * energy / 100f;
        if (fillRight > x) {
            paint.setColor(heroAircraft.isEnergyFull() ? 0xFFFFA726 : 0xFF4DD0E1);
            canvas.drawRoundRect(x, barTop, fillRight, barBottom, 4.5f, 4.5f, paint);
        }

        paint.setTextSize(12f);
        if (heroAircraft.isEnergyFull()) {
            paint.setColor(0xFFFFA726);
            canvas.drawText("能量已满 · 技能就绪", x, top + 98f, paint);
        } else {
            paint.setColor(0xFF8EE8FF);
            canvas.drawText("能量 " + energy + " / 100", x, top + 98f, paint);
        }

        // 5 阶火力灯条（LV.1 ~ MAX / 超载）
        int firepowerLv = heroAircraft.getFirepowerLevel();
        boolean overdrive = heroAircraft.isOverdriveActive();
        int tierColor = (overdrive || heroAircraft.isMaxFirepower())
                ? 0xFFFFB300
                : (firepowerLv >= 3 ? 0xFF2EE59D : 0xFF4DD0E1);
        String tierLabel = overdrive
                ? "超载 MAX"
                : (heroAircraft.isMaxFirepower() ? "火力 MAX" : ("火力 LV." + firepowerLv));

        paint.setColor(tierColor);
        paint.setTextSize(12f);
        canvas.drawText(tierLabel, x, top + 123f, paint);

        float segAreaLeft = x + 68f;
        float segGap = 4f;
        int maxLv = HeroAircraft.MAX_FIREPOWER_LEVEL;
        float segW = (barRight - segAreaLeft - segGap * (maxLv - 1)) / maxLv;
        float segTop = top + 114f;
        float segBottom = segTop + 9f;
        for (int i = 0; i < maxLv; i++) {
            float segLeft = segAreaLeft + i * (segW + segGap);
            paint.setColor(i < firepowerLv ? tierColor : 0x2EFFFFFF);
            canvas.drawRoundRect(segLeft, segTop, segLeft + segW, segBottom, 3.5f, 3.5f, paint);
        }
    }

    //***********************
    //      触屏控制（相对位移拖拽）
    //***********************

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOverFlag) {
            activePointerId = MotionEvent.INVALID_POINTER_ID;
            return false;
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                activePointerId = event.getPointerId(0);
                lastTouchScreenX = event.getX(0);
                lastTouchScreenY = event.getY(0);
                return true;

            case MotionEvent.ACTION_MOVE: {
                if (activePointerId == MotionEvent.INVALID_POINTER_ID || scale <= 0f) {
                    return true;
                }
                int pointerIndex = event.findPointerIndex(activePointerId);
                if (pointerIndex < 0) {
                    return true;
                }
                float currX = event.getX(pointerIndex);
                float currY = event.getY(pointerIndex);
                float dx = (currX - lastTouchScreenX) / scale;
                float dy = (currY - lastTouchScreenY) / scale;
                lastTouchScreenX = currX;
                lastTouchScreenY = currY;
                synchronized (heroInputLock) {
                    pendingDeltaX += dx;
                    pendingDeltaY += dy;
                }
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int pointerIndex = event.getActionIndex();
                int pointerId = event.getPointerId(pointerIndex);
                if (pointerId == activePointerId) {
                    int newIndex = (pointerIndex == 0) ? 1 : 0;
                    if (newIndex < event.getPointerCount()) {
                        activePointerId = event.getPointerId(newIndex);
                        lastTouchScreenX = event.getX(newIndex);
                        lastTouchScreenY = event.getY(newIndex);
                    } else {
                        activePointerId = MotionEvent.INVALID_POINTER_ID;
                    }
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private void syncHeroExactFromAircraft() {
        heroExactX = heroAircraft.getLocationX();
        heroExactY = heroAircraft.getLocationY();
        heroExactInitialized = true;
    }

    private void applyPendingHeroMove(float dtSec) {
        if (pendingHeroResetPosition) {
            pendingHeroResetPosition = false;
            heroAircraft.resetPosition();
            syncHeroExactFromAircraft();
            synchronized (heroInputLock) {
                pendingDeltaX = 0f;
                pendingDeltaY = 0f;
            }
            heroTiltDeg = 0f;
            heroVerticalBoost = 0f;
            return;
        }
        if (pendingHeroClamp) {
            pendingHeroClamp = false;
            heroAircraft.clampToBounds();
            syncHeroExactFromAircraft();
        }
        if (!heroExactInitialized) {
            syncHeroExactFromAircraft();
        }

        float dx;
        float dy;
        synchronized (heroInputLock) {
            dx = pendingDeltaX;
            dy = pendingDeltaY;
            pendingDeltaX = 0f;
            pendingDeltaY = 0f;
        }

        if (!gameOverFlag && (dx != 0f || dy != 0f)) {
            Bitmap heroImage = ImageManager.HERO_IMAGE;
            float marginX = heroImage != null ? heroImage.getWidth() * 0.25f : 0f;
            float marginY = heroImage != null ? heroImage.getHeight() * 0.35f : 0f;
            heroExactX = Math.max(marginX, Math.min(GameConfig.WINDOW_WIDTH - marginX, heroExactX + dx));
            heroExactY = Math.max(marginY, Math.min(GameConfig.WINDOW_HEIGHT - marginY, heroExactY + dy));
            heroAircraft.setLocation(Math.round(heroExactX), Math.round(heroExactY));
        }

        float safeDt = Math.max(0.004f, Math.min(0.05f, dtSec));
        float velocityX = dx / safeDt;
        float velocityY = dy / safeDt;
        float targetTilt = gameOverFlag ? 0f : Math.max(-11f, Math.min(11f, velocityX * 0.025f));
        float targetBoost = gameOverFlag ? 0f : Math.max(-0.35f, Math.min(1.0f, -velocityY * 0.003f));
        float ease = Math.min(1f, safeDt * 15f);
        heroTiltDeg += (targetTilt - heroTiltDeg) * ease;
        heroVerticalBoost += (targetBoost - heroVerticalBoost) * ease;
    }
}
