package edu.hitsz.ui;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;

import java.util.Random;

import edu.hitsz.application.GameConfig;

/**
 * 游戏打击感视觉特效系统（零 GC 压力对象池设计）：
 * - 击中火花与分级敌机/Boss 爆炸粒子
 * - 扩散冲击波圆环（击杀 / 道具拾取 / 大招）
 * - 屏幕震动（Screen Shake）
 * - 全屏环境与状态滤镜（炸弹闪白、受击红边、冰冻寒霜边缘与飘雪微粒）
 */
public class GameEffects {

    public static final int EXPLOSION_SMALL = 0;
    public static final int EXPLOSION_ELITE = 1;
    public static final int EXPLOSION_BOSS = 2;

    private static final int MAX_PARTICLES = 280;
    private static final int MAX_RINGS = 28;
    private static final int MAX_FLOATING_TEXTS = 14;
    private static final int FROST_MOTE_COUNT = 26;
    private static final int[] EXPLOSION_PALETTE =
            {0xFFFFFFFF, 0xFFFFF176, 0xFFFFB74D, 0xFFFF7043, 0xFFFF5252};

    private static final class Particle {
        boolean active;
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float age;
        float lifetime;
        float drag;
        int color;
        boolean spark;
    }

    private static final class Shockwave {
        boolean active;
        float x;
        float y;
        float startRadius;
        float maxRadius;
        float strokeWidth;
        float age;
        float lifetime;
        float delay;
        int color;
    }

    private static final class FrostMote {
        float normX;
        float normY;
        float speedY;
        float driftPhase;
        float radius;
    }

    private static final class FloatingText {
        boolean active;
        float x;
        float y;
        float vy;
        float age;
        float lifetime;
        float textSize;
        int color;
        String text;
    }

    private final Particle[] particles = new Particle[MAX_PARTICLES];
    private final Shockwave[] rings = new Shockwave[MAX_RINGS];
    private final FloatingText[] floatingTexts = new FloatingText[MAX_FLOATING_TEXTS];
    private final FrostMote[] frostMotes = new FrostMote[FROST_MOTE_COUNT];
    private final Random random = new Random();

    private int particleCursor = 0;
    private int ringCursor = 0;
    private int textCursor = 0;

    private float shakeIntensity = 0f;
    private float shakeOffsetX = 0f;
    private float shakeOffsetY = 0f;

    private float bombFlashAlpha = 0f;
    private float damageFlashAlpha = 0f;
    private long freezeOverlayEndMs = 0L;
    private float frostTime = 0f;

    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LinearGradient damageEdgeShader;
    private final LinearGradient frostEdgeShader;
    private final Matrix shaderMatrix = new Matrix();

    public GameEffects() {
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle();
        }
        for (int i = 0; i < MAX_RINGS; i++) {
            rings[i] = new Shockwave();
        }
        for (int i = 0; i < MAX_FLOATING_TEXTS; i++) {
            floatingTexts[i] = new FloatingText();
        }
        for (int i = 0; i < FROST_MOTE_COUNT; i++) {
            FrostMote mote = new FrostMote();
            mote.normX = random.nextFloat();
            mote.normY = random.nextFloat();
            mote.speedY = 0.05f + random.nextFloat() * 0.09f;
            mote.driftPhase = random.nextFloat() * 6.28f;
            mote.radius = 1.4f + random.nextFloat() * 2.4f;
            frostMotes[i] = mote;
        }

        particlePaint.setStrokeCap(Paint.Cap.ROUND);
        ringPaint.setStyle(Paint.Style.STROKE);
        overlayPaint.setStyle(Paint.Style.FILL);
        edgePaint.setStyle(Paint.Style.FILL);

        textFillPaint.setStyle(Paint.Style.FILL);
        textFillPaint.setTextAlign(Paint.Align.CENTER);
        textFillPaint.setTypeface(Typeface.DEFAULT_BOLD);

        textStrokePaint.setStyle(Paint.Style.STROKE);
        textStrokePaint.setTextAlign(Paint.Align.CENTER);
        textStrokePaint.setTypeface(Typeface.DEFAULT_BOLD);
        textStrokePaint.setStrokeJoin(Paint.Join.ROUND);
        textStrokePaint.setStrokeWidth(4.0f);
        textStrokePaint.setColor(0xDD070C16);

        damageEdgeShader = new LinearGradient(
                0f, 0f, 0f, 1f,
                0xCCFF2A3C, 0x00FF2A3C,
                Shader.TileMode.CLAMP);
        frostEdgeShader = new LinearGradient(
                0f, 0f, 0f, 1f,
                0x8864D8FF, 0x0064D8FF,
                Shader.TileMode.CLAMP);
    }

    public void reset() {
        for (Particle p : particles) {
            p.active = false;
        }
        for (Shockwave r : rings) {
            r.active = false;
        }
        for (FloatingText ft : floatingTexts) {
            ft.active = false;
        }
        shakeIntensity = 0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;
        bombFlashAlpha = 0f;
        damageFlashAlpha = 0f;
        freezeOverlayEndMs = 0L;
    }

    public void update(float dtSec) {
        float dt = Math.max(0f, Math.min(0.05f, dtSec));
        if (dt <= 0f) {
            return;
        }

        for (Particle p : particles) {
            if (!p.active) {
                continue;
            }
            p.age += dt;
            if (p.age >= p.lifetime) {
                p.active = false;
                continue;
            }
            float damping = Math.max(0f, 1f - p.drag * dt);
            p.vx *= damping;
            p.vy *= damping;
            p.x += p.vx * dt;
            p.y += p.vy * dt;
        }

        for (Shockwave r : rings) {
            if (!r.active) {
                continue;
            }
            if (r.delay > 0f) {
                r.delay -= dt;
                continue;
            }
            r.age += dt;
            if (r.age >= r.lifetime) {
                r.active = false;
            }
        }

        for (FloatingText ft : floatingTexts) {
            if (!ft.active) {
                continue;
            }
            ft.age += dt;
            if (ft.age >= ft.lifetime) {
                ft.active = false;
                continue;
            }
            ft.y += ft.vy * dt;
            ft.vy *= Math.max(0f, 1f - 1.6f * dt);
        }

        if (shakeIntensity > 0.15f) {
            float angle = random.nextFloat() * 6.283185f;
            float mag = shakeIntensity * (0.45f + random.nextFloat() * 0.55f);
            shakeOffsetX = (float) Math.cos(angle) * mag;
            shakeOffsetY = (float) Math.sin(angle) * mag;
            shakeIntensity *= (float) Math.exp(-11.5 * dt);
        } else {
            shakeIntensity = 0f;
            shakeOffsetX = 0f;
            shakeOffsetY = 0f;
        }

        if (bombFlashAlpha > 0f) {
            bombFlashAlpha = Math.max(0f, bombFlashAlpha - dt * 2.6f);
        }
        if (damageFlashAlpha > 0f) {
            damageFlashAlpha = Math.max(0f, damageFlashAlpha - dt * 2.2f);
        }

        if (getFreezeOverlayAlpha() > 0.01f) {
            frostTime += dt;
            for (FrostMote mote : frostMotes) {
                mote.normY += mote.speedY * dt;
                if (mote.normY > 1.02f) {
                    mote.normY = -0.02f;
                    mote.normX = random.nextFloat();
                }
            }
        }
    }

    public float getShakeOffsetX() {
        return shakeOffsetX;
    }

    public float getShakeOffsetY() {
        return shakeOffsetY;
    }

    public void addScreenShake(float intensity) {
        shakeIntensity = Math.min(18f, Math.max(shakeIntensity, intensity));
    }

    public void spawnHitSparks(float x, float y) {
        int count = 4 + random.nextInt(2);
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 6.283185f;
            float speed = 130f + random.nextFloat() * 190f;
            int color = (i % 2 == 0) ? 0xFFFFF4C2 : 0xFFFFB74D;
            spawnParticle(
                    x, y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    2.2f + random.nextFloat() * 1.4f,
                    0.12f + random.nextFloat() * 0.09f,
                    5.5f,
                    color,
                    true);
        }
    }

    public void spawnExplosion(float x, float y, int tier) {
        if (tier == EXPLOSION_BOSS) {
            addScreenShake(14.5f);
            bombFlashAlpha = Math.max(bombFlashAlpha, 0.55f);
            spawnRing(x, y, 14f, 165f, 6.5f, 0.46f, 0f, 0xFFFFD54F);
            spawnRing(x, y, 8f, 130f, 4.5f, 0.42f, 0.08f, 0xFFFF6E40);
            spawnRing(x, y, 10f, 195f, 3.2f, 0.54f, 0.16f, 0xFFFFFFFF);
            spawnExplosionParticles(x, y, 54, 110f, 430f, 4.2f, 8.5f, 0.42f, 0.78f);
        } else if (tier == EXPLOSION_ELITE) {
            addScreenShake(3.8f);
            spawnRing(x, y, 8f, 68f, 4.0f, 0.30f, 0f, 0xFFFFB74D);
            spawnRing(x, y, 6f, 46f, 2.2f, 0.24f, 0.05f, 0xFFFFFFFF);
            spawnExplosionParticles(x, y, 22, 85f, 290f, 3.0f, 6.2f, 0.26f, 0.52f);
        } else {
            spawnRing(x, y, 5f, 38f, 2.6f, 0.22f, 0f, 0xFFFFCA28);
            spawnExplosionParticles(x, y, 11, 70f, 210f, 2.2f, 4.6f, 0.18f, 0.36f);
        }
    }

    public void triggerBombBlast(float originX, float originY) {
        addScreenShake(11.5f);
        bombFlashAlpha = Math.max(bombFlashAlpha, 0.82f);
        float maxRadius = (float) Math.hypot(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT) * 0.75f;
        spawnRing(originX, originY, 20f, maxRadius, 9f, 0.45f, 0f, 0xFFFFE082);
        spawnRing(originX, originY, 10f, maxRadius * 0.85f, 4.5f, 0.40f, 0.06f, 0xFFFFFFFF);
        spawnExplosionParticles(originX, originY, 32, 140f, 420f, 3.2f, 6.5f, 0.32f, 0.62f);
    }

    public void triggerHeroDamage() {
        addScreenShake(7.8f);
        damageFlashAlpha = Math.max(damageFlashAlpha, 0.88f);
    }

    public void triggerFreezeOverlay(long durationMs) {
        long targetEnd = SystemClock.elapsedRealtime() + durationMs;
        if (targetEnd > freezeOverlayEndMs) {
            freezeOverlayEndMs = targetEnd;
        }
    }

    public void spawnPickupBurst(float x, float y, int color) {
        spawnRing(x, y, 10f, 62f, 3.5f, 0.32f, 0f, color);
        for (int i = 0; i < 12; i++) {
            float angle = (i / 12f) * 6.283185f + random.nextFloat() * 0.25f;
            float speed = 95f + random.nextFloat() * 95f;
            spawnParticle(
                    x, y,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    2.6f + random.nextFloat() * 1.6f,
                    0.24f + random.nextFloat() * 0.14f,
                    4.2f,
                    (i % 3 == 0) ? 0xFFFFFFFF : color,
                    false);
        }
    }

    public void spawnFloatingText(float x, float y, String text, int color, float textSize) {
        if (text == null || text.isEmpty()) {
            return;
        }
        FloatingText ft = floatingTexts[textCursor];
        textCursor = (textCursor + 1) % MAX_FLOATING_TEXTS;
        ft.active = true;
        ft.x = Math.max(76f, Math.min(GameConfig.WINDOW_WIDTH - 76f, x));
        ft.y = Math.max(56f, y);
        ft.vy = -58f;
        ft.age = 0f;
        ft.lifetime = 1.05f;
        ft.textSize = textSize;
        ft.color = color;
        ft.text = text;
    }

    /**
     * 在游戏世界坐标系（含屏幕震动）中绘制冲击波、粒子与上浮提示字。
     */
    public void drawWorldEffects(Canvas canvas) {
        // 1. 扩散冲击波
        for (Shockwave r : rings) {
            if (!r.active || r.delay > 0f) {
                continue;
            }
            float progress = Math.min(1f, r.age / r.lifetime);
            float eased = 1f - (1f - progress) * (1f - progress);
            float radius = r.startRadius + (r.maxRadius - r.startRadius) * eased;
            float alpha = 1f - progress;
            ringPaint.setColor(r.color);
            ringPaint.setAlpha(Math.round(230f * alpha));
            ringPaint.setStrokeWidth(Math.max(1f, r.strokeWidth * (1f - progress * 0.7f)));
            canvas.drawCircle(r.x, r.y, radius, ringPaint);
        }

        // 2. 粒子与火花
        for (Particle p : particles) {
            if (!p.active) {
                continue;
            }
            float progress = Math.min(1f, p.age / p.lifetime);
            float alpha = 1f - progress;
            particlePaint.setColor(p.color);
            particlePaint.setAlpha(Math.round(255f * alpha));
            if (p.spark) {
                particlePaint.setStyle(Paint.Style.STROKE);
                particlePaint.setStrokeWidth(p.size);
                float tailScale = 0.032f;
                canvas.drawLine(p.x, p.y, p.x - p.vx * tailScale, p.y - p.vy * tailScale, particlePaint);
            } else {
                particlePaint.setStyle(Paint.Style.FILL);
                float currentSize = Math.max(0.8f, p.size * (1f - progress * 0.55f));
                canvas.drawCircle(p.x, p.y, currentSize, particlePaint);
            }
        }

        // 3. 上浮状态提示字（如火力升级/超载/降级）
        for (FloatingText ft : floatingTexts) {
            if (!ft.active) {
                continue;
            }
            float progress = Math.min(1f, ft.age / ft.lifetime);
            float alpha = (progress < 0.65f) ? 1f : Math.max(0f, (1f - progress) / 0.35f);
            float popScale = (progress < 0.14f)
                    ? (0.78f + (progress / 0.14f) * 0.34f)
                    : (progress < 0.26f
                    ? (1.12f - ((progress - 0.14f) / 0.12f) * 0.12f)
                    : 1.0f);

            int textAlpha = Math.round(255f * alpha);
            textStrokePaint.setTextSize(ft.textSize);
            textStrokePaint.setAlpha(Math.round(220f * alpha));
            textFillPaint.setTextSize(ft.textSize);
            textFillPaint.setColor(ft.color);
            textFillPaint.setAlpha(textAlpha);

            int textSave = canvas.save();
            canvas.translate(ft.x, ft.y);
            canvas.scale(popScale, popScale);
            canvas.drawText(ft.text, 0f, 0f, textStrokePaint);
            canvas.drawText(ft.text, 0f, 0f, textFillPaint);
            canvas.restoreToCount(textSave);
        }
    }

    /**
     * 在屏幕顶层（不随震动偏移）绘制冰冻寒霜、受击红边与炸弹全屏闪白。
     */
    public void drawScreenOverlays(Canvas canvas, float width, float height) {
        // 1. 冰冻寒霜边缘与飘雪
        float freezeAlpha = getFreezeOverlayAlpha();
        if (freezeAlpha > 0.01f) {
            overlayPaint.setColor(0xFF4FC3F7);
            overlayPaint.setAlpha(Math.round(24f * freezeAlpha));
            canvas.drawRect(0f, 0f, width, height, overlayPaint);

            drawVignetteEdges(canvas, frostEdgeShader, width, height, 68f, freezeAlpha);

            particlePaint.setStyle(Paint.Style.FILL);
            particlePaint.setColor(0xFFE1F5FE);
            for (FrostMote mote : frostMotes) {
                float mx = (mote.normX * width) + (float) Math.sin(frostTime * 2.1f + mote.driftPhase) * 10f;
                float my = mote.normY * height;
                particlePaint.setAlpha(Math.round(140f * freezeAlpha));
                canvas.drawCircle(mx, my, mote.radius, particlePaint);
            }
        }

        // 2. 受击红边警示
        if (damageFlashAlpha > 0.01f) {
            drawVignetteEdges(canvas, damageEdgeShader, width, height, 84f, damageFlashAlpha);
        }

        // 3. 炸弹/Boss 击破全屏闪光
        if (bombFlashAlpha > 0.01f) {
            overlayPaint.setColor(0xFFFFF8E1);
            overlayPaint.setAlpha(Math.round(215f * bombFlashAlpha));
            canvas.drawRect(0f, 0f, width, height, overlayPaint);
        }
    }

    private float getFreezeOverlayAlpha() {
        long remaining = freezeOverlayEndMs - SystemClock.elapsedRealtime();
        if (remaining <= 0L) {
            return 0f;
        }
        if (remaining < 1000L) {
            return remaining / 1000f;
        }
        return 1f;
    }

    private void drawVignetteEdges(Canvas canvas, LinearGradient shader,
                                   float width, float height, float depth, float alphaFactor) {
        int alpha = Math.max(0, Math.min(255, Math.round(255f * alphaFactor)));
        edgePaint.setShader(shader);
        edgePaint.setAlpha(alpha);

        // 上边缘
        shaderMatrix.reset();
        shaderMatrix.setScale(1f, depth);
        shader.setLocalMatrix(shaderMatrix);
        canvas.drawRect(0f, 0f, width, depth, edgePaint);

        // 下边缘
        shaderMatrix.reset();
        shaderMatrix.setScale(1f, depth);
        shaderMatrix.postRotate(180f);
        shaderMatrix.postTranslate(0f, height);
        shader.setLocalMatrix(shaderMatrix);
        canvas.drawRect(0f, height - depth, width, height, edgePaint);

        // 左边缘
        shaderMatrix.reset();
        shaderMatrix.setScale(1f, depth);
        shaderMatrix.postRotate(-90f);
        shader.setLocalMatrix(shaderMatrix);
        canvas.drawRect(0f, 0f, depth, height, edgePaint);

        // 右边缘
        shaderMatrix.reset();
        shaderMatrix.setScale(1f, depth);
        shaderMatrix.postRotate(90f);
        shaderMatrix.postTranslate(width, 0f);
        shader.setLocalMatrix(shaderMatrix);
        canvas.drawRect(width - depth, 0f, width, height, edgePaint);
    }

    private void spawnExplosionParticles(float x, float y, int count,
                                         float minSpeed, float maxSpeed,
                                         float minSize, float maxSize,
                                         float minLife, float maxLife) {
        for (int i = 0; i < count; i++) {
            float angle = random.nextFloat() * 6.283185f;
            float speed = minSpeed + random.nextFloat() * (maxSpeed - minSpeed);
            float size = minSize + random.nextFloat() * (maxSize - minSize);
            float life = minLife + random.nextFloat() * (maxLife - minLife);
            int color = EXPLOSION_PALETTE[random.nextInt(EXPLOSION_PALETTE.length)];
            boolean isSpark = (i % 3 == 0);
            spawnParticle(
                    x + (random.nextFloat() - 0.5f) * 10f,
                    y + (random.nextFloat() - 0.5f) * 10f,
                    (float) Math.cos(angle) * speed,
                    (float) Math.sin(angle) * speed,
                    isSpark ? Math.max(2f, size * 0.55f) : size,
                    life,
                    isSpark ? 3.2f : 4.6f,
                    color,
                    isSpark);
        }
    }

    private void spawnParticle(float x, float y, float vx, float vy,
                               float size, float lifetime, float drag,
                               int color, boolean spark) {
        Particle p = particles[particleCursor];
        particleCursor = (particleCursor + 1) % MAX_PARTICLES;
        p.active = true;
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.size = size;
        p.age = 0f;
        p.lifetime = lifetime;
        p.drag = drag;
        p.color = color;
        p.spark = spark;
    }

    private void spawnRing(float x, float y, float startRadius, float maxRadius,
                           float strokeWidth, float lifetime, float delay, int color) {
        Shockwave r = rings[ringCursor];
        ringCursor = (ringCursor + 1) % MAX_RINGS;
        r.active = true;
        r.x = x;
        r.y = y;
        r.startRadius = startRadius;
        r.maxRadius = maxRadius;
        r.strokeWidth = strokeWidth;
        r.age = 0f;
        r.lifetime = lifetime;
        r.delay = delay;
        r.color = color;
    }
}
