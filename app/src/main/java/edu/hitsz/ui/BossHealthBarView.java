package edu.hitsz.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 顶部极简分段 Boss 血条：
 * - 100% ~ 60%：翠绿
 * - 60%  ~ 25%：琥珀黄
 * - 25%  ~  0%：猩红
 * 主血条即时响应受击，并以亮白色延迟掉血残影呈现伤害反馈。
 */
public class BossHealthBarView extends View {

    private static final float THRESHOLD_LOW = 0.25f;
    private static final float THRESHOLD_HIGH = 0.60f;

    private static final int COLOR_GREEN = 0xFF2EE59D;
    private static final int COLOR_AMBER = 0xFFFFB830;
    private static final int COLOR_RED = 0xFFFF4757;
    private static final int COLOR_TRAIL_WHITE = 0xF2FFFFFF;

    // 亮白残影停顿延迟（毫秒）与回落速度（每秒比例）
    private static final long TRAIL_HOLD_MS = 240L;
    private static final float TRAIL_SHRINK_SPEED_PER_SEC = 1.15f;

    private final float borderStrokePx;
    private final float dividerWidthPx;
    private final float dividerGlowWidthPx;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final LinearGradient dividerGlowShader;
    private final Matrix shaderMatrix = new Matrix();

    private final RectF outerRect = new RectF();
    private final RectF innerRect = new RectF();
    private final Path innerClipPath = new Path();

    private boolean bossActive = false;
    private float currentRatio = 0f;
    private float trailRatio = 0f;
    private long trailHoldUntilMs = 0L;
    private long lastFrameTimeMs = 0L;

    public BossHealthBarView(Context context) {
        this(context, null);
    }

    public BossHealthBarView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BossHealthBarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        borderStrokePx = 1.2f * density;
        dividerWidthPx = 2f * density;
        dividerGlowWidthPx = 6f * density;

        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(0xCC070C16);

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderStrokePx);
        borderPaint.setColor(0x52FFFFFF);

        fillPaint.setStyle(Paint.Style.FILL);

        trailPaint.setStyle(Paint.Style.FILL);
        trailPaint.setColor(COLOR_TRAIL_WHITE);

        dividerGlowPaint.setStyle(Paint.Style.FILL);
        dividerPaint.setStyle(Paint.Style.FILL);

        dividerGlowShader = new LinearGradient(
                0f, 0f, 1f, 0f,
                new int[]{0x00FFFFFF, 0x70FFFFFF, 0x00FFFFFF},
                new float[]{0f, 0.5f, 1f},
                Shader.TileMode.CLAMP);
        dividerGlowPaint.setShader(dividerGlowShader);
    }

    public void updateBossHp(int hp, int maxHp) {
        long now = SystemClock.elapsedRealtime();
        if (maxHp <= 0 || hp <= 0) {
            if (bossActive) {
                bossActive = false;
                currentRatio = 0f;
                trailHoldUntilMs = now + TRAIL_HOLD_MS;
                lastFrameTimeMs = now;
                invalidate();
                animate().cancel();
                animate()
                        .alpha(0f)
                        .setDuration(260)
                        .withEndAction(() -> {
                            if (!bossActive) {
                                setVisibility(GONE);
                            }
                        })
                        .start();
            }
            return;
        }

        float ratio = Math.max(0f, Math.min(1f, hp / (float) maxHp));
        if (!bossActive) {
            bossActive = true;
            currentRatio = ratio;
            trailRatio = ratio;
            trailHoldUntilMs = 0L;
            lastFrameTimeMs = now;
            animate().cancel();
            setAlpha(0f);
            setVisibility(VISIBLE);
            animate().alpha(1f).setDuration(200).start();
        } else {
            if (ratio < currentRatio) {
                if (trailRatio < currentRatio) {
                    trailRatio = currentRatio;
                }
                currentRatio = ratio;
                trailHoldUntilMs = now + TRAIL_HOLD_MS;
                lastFrameTimeMs = now;
            } else if (ratio > currentRatio) {
                currentRatio = ratio;
                trailRatio = Math.max(trailRatio, ratio);
                lastFrameTimeMs = now;
            }
        }
        invalidate();
    }

    public void hideImmediately() {
        bossActive = false;
        currentRatio = 0f;
        trailRatio = 0f;
        trailHoldUntilMs = 0L;
        lastFrameTimeMs = 0L;
        animate().cancel();
        setAlpha(1f);
        setVisibility(GONE);
    }

    private int resolveFillColor(float ratio) {
        if (ratio > THRESHOLD_HIGH) {
            return COLOR_GREEN;
        }
        if (ratio > THRESHOLD_LOW) {
            return COLOR_AMBER;
        }
        return COLOR_RED;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float halfStroke = borderStrokePx / 2f;
        outerRect.set(halfStroke, halfStroke, w - halfStroke, h - halfStroke);
        innerRect.set(borderStrokePx, borderStrokePx,
                Math.max(borderStrokePx, w - borderStrokePx),
                Math.max(borderStrokePx, h - borderStrokePx));

        innerClipPath.reset();
        float innerRadius = innerRect.height() / 2f;
        innerClipPath.addRoundRect(innerRect, innerRadius, innerRadius, Path.Direction.CW);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (innerRect.width() <= 0f || innerRect.height() <= 0f) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        boolean needsMoreFrames = false;
        if (trailRatio > currentRatio) {
            if (now >= trailHoldUntilMs) {
                float dtSec = (lastFrameTimeMs > 0L)
                        ? Math.min(0.05f, Math.max(0f, (now - lastFrameTimeMs) / 1000f))
                        : 0.016f;
                trailRatio = Math.max(currentRatio, trailRatio - TRAIL_SHRINK_SPEED_PER_SEC * dtSec);
            }
            if (trailRatio > currentRatio) {
                needsMoreFrames = true;
            }
        }
        lastFrameTimeMs = now;

        float outerRadius = outerRect.height() / 2f;
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, trackPaint);

        int barColor = resolveFillColor(currentRatio);
        float innerLeft = innerRect.left;
        float innerTop = innerRect.top;
        float innerBottom = innerRect.bottom;
        float innerWidth = innerRect.width();
        float fillRight = innerLeft + innerWidth * currentRatio;
        float trailRight = innerLeft + innerWidth * trailRatio;

        int saveCount = canvas.save();
        canvas.clipPath(innerClipPath);

        // 1. 亮白色延迟掉血残影
        if (trailRight > fillRight) {
            canvas.drawRect(fillRight, innerTop, trailRight, innerBottom, trailPaint);
        }

        // 2. 主血条即时填充
        if (fillRight > innerLeft) {
            fillPaint.setColor(barColor);
            canvas.drawRect(innerLeft, innerTop, fillRight, innerBottom, fillPaint);
        }

        // 3. 25% 与 60% 白色分段竖线
        drawDivider(canvas, innerLeft + innerWidth * THRESHOLD_LOW, innerTop, innerBottom, fillRight);
        drawDivider(canvas, innerLeft + innerWidth * THRESHOLD_HIGH, innerTop, innerBottom, fillRight);

        canvas.restoreToCount(saveCount);

        // 4. 外框细描边
        canvas.drawRoundRect(outerRect, outerRadius, outerRadius, borderPaint);

        if (needsMoreFrames) {
            postInvalidateOnAnimation();
        }
    }

    private void drawDivider(Canvas canvas, float x, float top, float bottom, float fillRight) {
        if (x <= fillRight) {
            float halfGlow = dividerGlowWidthPx / 2f;
            shaderMatrix.reset();
            shaderMatrix.setScale(dividerGlowWidthPx, 1f);
            shaderMatrix.postTranslate(x - halfGlow, 0f);
            dividerGlowShader.setLocalMatrix(shaderMatrix);
            canvas.drawRect(x - halfGlow, top, x + halfGlow, bottom, dividerGlowPaint);

            dividerPaint.setColor(0xFFFFFFFF);
            float halfCore = dividerWidthPx / 2f;
            canvas.drawRect(x - halfCore, top, x + halfCore, bottom, dividerPaint);
        } else {
            dividerPaint.setColor(0x55FFFFFF);
            float halfCore = dividerWidthPx * 0.4f;
            canvas.drawRect(x - halfCore, top, x + halfCore, bottom, dividerPaint);
        }
    }
}
