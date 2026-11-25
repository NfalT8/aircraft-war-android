package edu.hitsz.basic;

import android.graphics.Bitmap;
import android.os.SystemClock;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.application.ImageManager;

/**
 * 可飞行对象的基类
 */
public abstract class AbstractFlyingObject {

    protected int locationX;
    protected int locationY;
    protected int prevLocationX;
    protected int prevLocationY;

    protected int speedX;
    protected int speedY;

    protected Bitmap image = null;

    protected int width = -1;
    protected int height = -1;

    protected boolean isValid = true;

    private boolean frozen = false;
    private long freezeEndTimeMs = 0L;
    private int frozenOriginalSpeedX = 0;
    private int frozenOriginalSpeedY = 0;

    public AbstractFlyingObject() {
    }

    public AbstractFlyingObject(int locationX, int locationY, int speedX, int speedY) {
        this.locationX = locationX;
        this.locationY = locationY;
        this.prevLocationX = locationX;
        this.prevLocationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    /**
     * 在游戏主循环线程安全地施加冰冻/减速效果：
     * 重复冰冻时仅延长结束时间，不会覆盖原始速度，避免永久冻结。
     */
    protected void applyFreeze(long durationMs, double speedFactor) {
        long targetEnd = SystemClock.elapsedRealtime() + durationMs;
        if (!frozen) {
            frozen = true;
            frozenOriginalSpeedX = this.speedX;
            frozenOriginalSpeedY = this.speedY;
            this.speedX = (int) Math.round(this.speedX * speedFactor);
            this.speedY = (int) Math.round(this.speedY * speedFactor);
        }
        if (targetEnd > freezeEndTimeMs) {
            freezeEndTimeMs = targetEnd;
        }
    }

    /**
     * 根据速度移动对象，并记录上一逻辑帧位置以供高帧率渲染插值。
     */
    public void forward() {
        if (frozen && SystemClock.elapsedRealtime() >= freezeEndTimeMs) {
            frozen = false;
            int restoredX = Math.abs(frozenOriginalSpeedX);
            if (this.speedX < 0 || (this.speedX == 0 && frozenOriginalSpeedX < 0)) {
                restoredX = -restoredX;
            }
            this.speedX = restoredX;
            this.speedY = frozenOriginalSpeedY;
        }
        prevLocationX = locationX;
        prevLocationY = locationY;
        locationX += speedX;
        locationY += speedY;
    }

    public float getInterpolatedX(float alpha) {
        return prevLocationX + (locationX - prevLocationX) * alpha;
    }

    public float getInterpolatedY(float alpha) {
        return prevLocationY + (locationY - prevLocationY) * alpha;
    }

    public boolean crash(AbstractFlyingObject flyingObject) {
        int factor = this instanceof AbstractAircraft ? 2 : 1;
        int fFactor = flyingObject instanceof AbstractAircraft ? 2 : 1;

        int x = flyingObject.getLocationX();
        int y = flyingObject.getLocationY();
        int fWidth = flyingObject.getWidth();
        int fHeight = flyingObject.getHeight();

        return x + (fWidth + this.getWidth()) / 2 > locationX
                && x - (fWidth + this.getWidth()) / 2 < locationX
                && y + (fHeight / fFactor + this.getHeight() / factor) / 2 > locationY
                && y - (fHeight / fFactor + this.getHeight() / factor) / 2 < locationY;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocation(double locationX, double locationY) {
        this.locationX = (int) locationX;
        this.locationY = (int) locationY;
        this.prevLocationX = this.locationX;
        this.prevLocationY = this.locationY;
    }

    public int getSpeedY() {
        return speedY;
    }

    public Bitmap getImage() {
        if (image == null) {
            image = ImageManager.get(this);
        }
        return image;
    }

    public int getWidth() {
        if (width == -1) {
            width = ImageManager.get(this).getWidth();
        }
        return width;
    }

    public int getHeight() {
        if (height == -1) {
            height = ImageManager.get(this).getHeight();
        }
        return height;
    }

    public void vanish() {
        isValid = false;
    }

    public boolean notValid() {
        return !this.isValid;
    }
}
