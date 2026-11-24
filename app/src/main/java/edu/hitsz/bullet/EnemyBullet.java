package edu.hitsz.bullet;

import edu.hitsz.observer.GameObserver;

/**
 * 敌机子弹
 * @Author hitsz
 */
public class EnemyBullet extends BaseBullet implements GameObserver {

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public void updateOnFreeze(){
        int originalSpeedY = this.speedY;
        int originalSpeedX = this.speedX;
        this.speedY = 0;
        this.speedX = 0;

        // 开启多线程计时恢复
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                this.speedY = originalSpeedY;
                this.speedX = originalSpeedX;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void updateOnBomb(){
        this.vanish();
    }

}
