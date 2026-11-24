package edu.hitsz.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.hitsz.R;
import edu.hitsz.application.BaseGame.BuffType;

/**
 * 右上角 Buff 倒计时图标视图：每个激活中的 Buff 以圆形图标 + 顺时针扇形遮罩呈现剩余时长。
 */
public class BuffCountdownView extends View {

    private static final int COLOR_SCATTER = 0xFFFFB300;
    private static final int COLOR_RING = 0xFF4DD0E1;
    private static final int COLOR_FREEZE = 0xFF81D4FA;

    private static final class ActiveBuff {
        final int slotKey;
        final int accentColor;
        final Drawable icon;
        final long durationMs;
        final long endTimeMs;

        ActiveBuff(int slotKey, int accentColor, Drawable icon,
                   long durationMs, long endTimeMs) {
            this.slotKey = slotKey;
            this.accentColor = accentColor;
            this.icon = icon;
            this.durationMs = durationMs;
            this.endTimeMs = endTimeMs;
        }
    }

    private final List<ActiveBuff> activeBuffs = new ArrayList<>();

    private final int iconSizePx;
    private final int itemGapPx;
    private final int drawableSizePx;
    private final float ringStrokePx;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tintBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private final RectF maskRect = new RectF();

    public BuffCountdownView(Context context) {
        this(context, null);
    }

    public BuffCountdownView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BuffCountdownView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        float density = getResources().getDisplayMetrics().density;
        iconSizePx = Math.round(48f * density);
        itemGapPx = Math.round(10f * density);
        drawableSizePx = Math.round(24f * density);
        ringStrokePx = 2.5f * density;

        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(0xCC0B1220);

        tintBgPaint.setStyle(Paint.Style.FILL);

        maskPaint.setStyle(Paint.Style.FILL);
        maskPaint.setColor(0xBA050A14);

        trackRingPaint.setStyle(Paint.Style.STROKE);
        trackRingPaint.setStrokeWidth(1.2f * density);
        trackRingPaint.setColor(0x40FFFFFF);

        progressRingPaint.setStyle(Paint.Style.STROKE);
        progressRingPaint.setStrokeWidth(ringStrokePx);
        progressRingPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    /**
     * 启动或刷新指定 Buff 的倒计时。武器类 Buff（散射/环射）共用同一槽位，冰冻独立成槽。
     */
    public void startBuff(@NonNull BuffType type, long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        int slotKey = (type == BuffType.FREEZE) ? 1 : 0;
        int accentColor = resolveColor(type);
        Drawable icon = resolveIcon(type, accentColor);
        long now = SystemClock.elapsedRealtime();
        ActiveBuff buff = new ActiveBuff(slotKey, accentColor, icon, durationMs, now + durationMs);

        int existingIndex = -1;
        for (int i = 0; i < activeBuffs.size(); i++) {
            if (activeBuffs.get(i).slotKey == slotKey) {
                existingIndex = i;
                break;
            }
        }
        if (existingIndex >= 0) {
            activeBuffs.set(existingIndex, buff);
        } else {
            activeBuffs.add(buff);
        }

        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        requestLayout();
        invalidate();
    }

    public void clearBuffs() {
        if (activeBuffs.isEmpty() && getVisibility() == GONE) {
            return;
        }
        activeBuffs.clear();
        setVisibility(GONE);
        requestLayout();
        invalidate();
    }

    private int resolveColor(BuffType type) {
        switch (type) {
            case SCATTER:
                return COLOR_SCATTER;
            case FREEZE:
                return COLOR_FREEZE;
            case RING:
            default:
                return COLOR_RING;
        }
    }

    @Nullable
    private Drawable resolveIcon(BuffType type, int tintColor) {
        int resId;
        switch (type) {
            case SCATTER:
                resId = R.drawable.ic_buff_scatter;
                break;
            case FREEZE:
                resId = R.drawable.ic_buff_freeze;
                break;
            case RING:
            default:
                resId = R.drawable.ic_skill_ring;
                break;
        }
        Drawable raw = ContextCompat.getDrawable(getContext(), resId);
        if (raw == null) {
            return null;
        }
        Drawable wrapped = DrawableCompat.wrap(raw.mutate());
        DrawableCompat.setTint(wrapped, tintColor);
        return wrapped;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = activeBuffs.size();
        if (count == 0) {
            setMeasuredDimension(0, 0);
            return;
        }
        int totalHeight = count * iconSizePx + (count - 1) * itemGapPx;
        setMeasuredDimension(iconSizePx, totalHeight);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (activeBuffs.isEmpty()) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        boolean removed = false;
        Iterator<ActiveBuff> it = activeBuffs.iterator();
        while (it.hasNext()) {
            if (it.next().endTimeMs <= now) {
                it.remove();
                removed = true;
            }
        }

        if (activeBuffs.isEmpty()) {
            post(() -> {
                if (activeBuffs.isEmpty()) {
                    setVisibility(GONE);
                    requestLayout();
                }
            });
            return;
        }
        if (removed) {
            post(this::requestLayout);
        }

        float radius = iconSizePx / 2f;
        float ringInset = ringStrokePx / 2f + 0.5f;
        float innerRadius = radius - ringStrokePx;
        int halfIcon = drawableSizePx / 2;

        for (int i = 0; i < activeBuffs.size(); i++) {
            ActiveBuff buff = activeBuffs.get(i);
            float top = i * (iconSizePx + itemGapPx);
            float cx = radius;
            float cy = top + radius;

            float remainingFraction = Math.max(0f, Math.min(1f,
                    (buff.endTimeMs - now) / (float) buff.durationMs));
            float elapsedSweep = 360f * (1f - remainingFraction);
            float remainingSweep = 360f * remainingFraction;

            // 1. 深色圆形底板 + 对应 Buff 主题微光
            canvas.drawCircle(cx, cy, radius - ringInset, bgPaint);
            tintBgPaint.setColor((buff.accentColor & 0x00FFFFFF) | 0x26000000);
            canvas.drawCircle(cx, cy, innerRadius, tintBgPaint);

            // 2. 居中绘制 Buff 图标
            if (buff.icon != null) {
                int iconLeft = Math.round(cx) - halfIcon;
                int iconTop = Math.round(cy) - halfIcon;
                buff.icon.setBounds(iconLeft, iconTop,
                        iconLeft + drawableSizePx, iconTop + drawableSizePx);
                buff.icon.draw(canvas);
            }

            // 3. 顺时针扇形遮罩（覆盖已流逝的时间）
            if (elapsedSweep > 0.5f) {
                maskRect.set(cx - innerRadius, cy - innerRadius,
                        cx + innerRadius, cy + innerRadius);
                canvas.drawArc(maskRect, -90f, elapsedSweep, true, maskPaint);
            }

            // 4. 外圈底轨 + 剩余时长进度弧
            arcRect.set(ringInset, top + ringInset,
                    iconSizePx - ringInset, top + iconSizePx - ringInset);
            canvas.drawOval(arcRect, trackRingPaint);
            if (remainingSweep > 0.5f) {
                progressRingPaint.setColor(buff.accentColor);
                canvas.drawArc(arcRect, -90f + elapsedSweep, remainingSweep, false, progressRingPaint);
            }
        }

        postInvalidateOnAnimation();
    }
}
