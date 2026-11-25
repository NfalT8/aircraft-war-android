package edu.hitsz.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

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
import edu.hitsz.strategy.DirectShootStrategy;
import edu.hitsz.strategy.RingShootStrategy;
import edu.hitsz.strategy.ScatterShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 游戏主循环：固定 25Hz（40ms）逻辑步长 + Choreographer VSync 高帧率（60/90/120Hz）插值渲染，
 * 配合 GPU 硬件画布与零延迟触屏响应。
 */
public abstract class BaseGame extends SurfaceView implements SurfaceHolder.Callback {

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

    private static final long LOGIC_STEP_NANOS = 40_000_000L;
    private static final long MAX_FRAME_DELTA_NANOS = 200_000_000L;
    private static final int MAX_LOGIC_STEPS_PER_FRAME = 4;

    private final SurfaceHolder surfaceHolder;
    private HandlerThread gameThread;
    private Handler gameHandler;
    private Choreographer gameChoreographer;
    private volatile boolean running = false;
    private boolean useHardwareCanvas = true;

    private long lastFrameTimeNanos = 0L;
    private long accumulatorNanos = 0L;

    // 逻辑坐标到屏幕坐标的等比缩放系数与安全区偏移
    private volatile float scale = 0f;
    private volatile int topSafeInsetPx = 0;
    private boolean heroPositionedForSurface = false;
    private volatile boolean pendingHeroResetPosition = false;
    private volatile boolean pendingHeroClamp = false;

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
    protected double shootCycle = 20;
    protected int shootCounter = 0;

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

    //触屏位置由 UI 线程写入，游戏线程在下一帧应用
    private volatile int pendingHeroX = -1;
    private volatile int pendingHeroY = -1;
    private volatile boolean heroMovePending = false;

    private final MyMediaPlayer mediaPlayer;
    private final MySoundPool soundPool;
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
    private final RectF backgroundDest = new RectF();

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
        surfaceHolder = getHolder();
        surfaceHolder.addCallback(this);
        setFocusable(true);

        Context appContext = context.getApplicationContext();
        mediaPlayer = new MyMediaPlayer(appContext);
        soundPool = new MySoundPool(appContext);

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
    //   SurfaceView 生命周期
    //***********************

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        running = true;
        if (!gameOverFlag) {
            mediaPlayer.resume();
        }
        if (gameThread == null || !gameThread.isAlive()) {
            gameThread = new HandlerThread("aircraft-game-loop", Process.THREAD_PRIORITY_DISPLAY);
            gameThread.start();
            gameHandler = new Handler(gameThread.getLooper());
            gameHandler.post(() -> {
                if (!running) {
                    return;
                }
                lastFrameTimeNanos = 0L;
                accumulatorNanos = 0L;
                gameChoreographer = Choreographer.getInstance();
                gameChoreographer.postFrameCallback(frameCallback);
            });
        }
    }

    public void setTopSafeInsetPx(int topSafeInsetPx) {
        this.topSafeInsetPx = Math.max(0, topSafeInsetPx);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        mediaPlayer.pause();
        HandlerThread thread = gameThread;
        Handler handler = gameHandler;
        gameThread = null;
        gameHandler = null;
        if (handler != null) {
            handler.post(() -> {
                if (gameChoreographer != null) {
                    gameChoreographer.removeFrameCallback(frameCallback);
                    gameChoreographer = null;
                }
            });
        }
        if (thread != null) {
            thread.quitSafely();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
            if (gameChoreographer != null) {
                gameChoreographer.postFrameCallback(this);
            }

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

            // 英雄机位置每渲染帧立即应用，保证 60/120Hz 零延迟跟手
            applyPendingHeroMove();

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

            float alpha = gameOverFlag
                    ? 1f
                    : Math.max(0f, Math.min(1f, accumulatorNanos / (float) LOGIC_STEP_NANOS));

            Canvas canvas = null;
            try {
                canvas = lockCanvasCompat();
                if (canvas != null) {
                    drawFrame(canvas, alpha);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    };

    private Canvas lockCanvasCompat() {
        if (!surfaceHolder.getSurface().isValid()) {
            return null;
        }
        if (useHardwareCanvas) {
            try {
                return surfaceHolder.lockHardwareCanvas();
            } catch (IllegalStateException | UnsupportedOperationException e) {
                useHardwareCanvas = false;
            }
        }
        return surfaceHolder.lockCanvas();
    }

    private void action() {
        bgTickCount++;
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
        shootCounter++;
        if (shootCounter >= shootCycle) {
            shootCounter = 0;
            //英雄机射击
            heroBullets.addAll(heroAircraft.shoot());
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
                bullet.vanish();
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
                    bullet.vanish();
                    if (enemyAircraft.notValid()) {
                        if (enemyAircraft instanceof BossEnemy) {
                            score += 100;
                            bossExisted = false;
                            mediaPlayer.stopBossBgm();
                            heroAircraft.addEnergy(15);
                        } else if (enemyAircraft instanceof EliteProEnemy) {
                            score += 30;
                            heroAircraft.addEnergy(10);
                        } else if (enemyAircraft instanceof ElitePlusEnemy) {
                            score += 20;
                            heroAircraft.addEnergy(5);
                        } else if (enemyAircraft instanceof EliteEnemy) {
                            score += 10;
                            heroAircraft.addEnergy(3);
                        } else {
                            score += 5;
                            heroAircraft.addEnergy(2);
                        }
                        dropProp(enemyAircraft);
                    }
                }

                // 英雄机 与 敌机 相撞，均损毁
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
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
                if (prop instanceof BombProp) {
                    soundPool.playBombExplosion();
                } else {
                    soundPool.playGetSupply();
                }
                if (prop instanceof BloodProp) {
                    heroAircraft.increaseHp(10);
                } else if (prop instanceof BulletProp) {
                    heroAircraft.setStrategy(new ScatterShootStrategy());
                    applyFirepowerTimeout(BuffType.SCATTER, 5000);
                } else if (prop instanceof BulletPlusProp) {
                    heroAircraft.setStrategy(new RingShootStrategy());
                    applyFirepowerTimeout(BuffType.RING, 5000);
                } else if (prop instanceof FreezeProp) {
                    FreezeProp freeze = (FreezeProp) prop;
                    for (AbstractAircraft enemy : enemyAircrafts) {
                        freeze.addObserver((GameObserver) enemy);
                    }
                    for (BaseBullet bullet : enemyBullets) {
                        freeze.addObserver((GameObserver) bullet);
                    }
                    freeze.notifyObservers();
                    notifyBuffStarted(BuffType.FREEZE, 5000);
                } else if (prop instanceof BombProp) {
                    BombProp bomb = (BombProp) prop;

                    for (AbstractAircraft enemy : enemyAircrafts) {
                        bomb.addObserver((GameObserver) enemy);
                    }
                    for (BaseBullet bullet : enemyBullets) {
                        bomb.addObserver((GameObserver) bullet);
                    }

                    bomb.notifyObservers();
                }

                prop.vanish();
            }
        }
    }

    private void releaseUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();

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
        }
    }

    private void healUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();
            heroAircraft.increaseHp(50);
        }
    }

    private void ringShootUltimate() {
        if (heroAircraft.isEnergyFull()) {
            heroAircraft.resetEnergy();
            heroAircraft.setStrategy(new RingShootStrategy());
            applyFirepowerTimeout(BuffType.RING, 7000);
        }
    }

    /**
     * 处理火力道具及环射大招的限时恢复逻辑
     */
    private void applyFirepowerTimeout(BuffType buffType, long duration) {
        long expectedEndTime = System.currentTimeMillis() + duration;

        heroAircraft.setEffectEndTime(expectedEndTime);
        notifyBuffStarted(buffType, duration);

        new Thread(() -> {
            try {
                Thread.sleep(duration);

                if (System.currentTimeMillis() >= heroAircraft.getEffectEndTime()) {
                    heroAircraft.setStrategy(new DirectShootStrategy());
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
                } else if (enemyAircraft instanceof ElitePlusEnemy) {
                    score += 20;
                    heroAircraft.addEnergy(5);
                } else if (enemyAircraft instanceof EliteEnemy) {
                    score += 10;
                    heroAircraft.addEnergy(3);
                } else {
                    score += 5;
                    heroAircraft.addEnergy(3);
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
        heroMovePending = false;
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

        // 绘制背景（按固定步长插值平滑滚动）
        drawBackground(canvas, alpha);

        // 先绘制子弹，后绘制飞机与道具（高帧率位置插值）
        paintImageWithPositionRevised(canvas, enemyBullets, alpha);
        paintImageWithPositionRevised(canvas, heroBullets, alpha);
        paintImageWithPositionRevised(canvas, enemyAircrafts, alpha);
        paintImageWithPositionRevised(canvas, props, alpha);

        Bitmap heroImage = ImageManager.HERO_IMAGE;
        if (heroImage != null) {
            canvas.drawBitmap(heroImage,
                    heroAircraft.getLocationX() - heroImage.getWidth() / 2f,
                    heroAircraft.getLocationY() - heroImage.getHeight() / 2f,
                    bitmapPaint);
        }

        //绘制得分和生命值
        paintScoreAndLife(canvas);

        canvas.restoreToCount(saveCount);
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
            canvas.drawBitmap(image,
                    object.getInterpolatedX(alpha) - image.getWidth() / 2f,
                    object.getInterpolatedY(alpha) - image.getHeight() / 2f,
                    bitmapPaint);
        }
    }

    private void paintScoreAndLife(Canvas canvas) {
        final float left = 12f;
        final float topMarginPx = 30f * getResources().getDisplayMetrics().density;
        final float top = (topSafeInsetPx + topMarginPx) / scale;
        final float right = 268f;
        final float bottom = top + 112f;
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

        float barLeft = x;
        float barTop = top + 68f;
        float barRight = right - 16f;
        float barBottom = barTop + 9f;
        paint.setColor(0x33FFFFFF);
        canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, 4.5f, 4.5f, paint);

        int energy = Math.min(heroAircraft.getEnergy(), 100);
        float fillRight = barLeft + (barRight - barLeft) * energy / 100f;
        if (fillRight > barLeft) {
            paint.setColor(heroAircraft.isEnergyFull() ? 0xFFFFA726 : 0xFF4DD0E1);
            canvas.drawRoundRect(barLeft, barTop, fillRight, barBottom, 4.5f, 4.5f, paint);
        }

        paint.setTextSize(12f);
        if (heroAircraft.isEnergyFull()) {
            paint.setColor(0xFFFFA726);
            canvas.drawText("能量已满 · 技能就绪", x, top + 102f, paint);
        } else {
            paint.setColor(0xFF8EE8FF);
            canvas.drawText("能量 " + energy + " / 100", x, top + 102f, paint);
        }
    }

    //***********************
    //      触屏控制
    //***********************

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            if (gameOverFlag) {
                restartRequested = true;
            } else {
                moveHero(event.getX(), event.getY());
            }
            return true;
        }
        if (action == MotionEvent.ACTION_MOVE) {
            if (!gameOverFlag) {
                moveHero(event.getX(), event.getY());
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    private void moveHero(float screenX, float screenY) {
        if (scale <= 0f) {
            return;
        }
        float x = Math.max(0f, Math.min(GameConfig.WINDOW_WIDTH, screenX / scale));
        float y = Math.max(0f, Math.min(GameConfig.WINDOW_HEIGHT, screenY / scale));
        pendingHeroX = Math.round(x);
        pendingHeroY = Math.round(y);
        heroMovePending = true;
    }

    private void applyPendingHeroMove() {
        if (pendingHeroResetPosition) {
            pendingHeroResetPosition = false;
            heroAircraft.resetPosition();
        }
        if (pendingHeroClamp) {
            pendingHeroClamp = false;
            heroAircraft.clampToBounds();
        }
        if (heroMovePending) {
            heroMovePending = false;
            heroAircraft.setLocation(pendingHeroX, pendingHeroY);
        }
    }
}
