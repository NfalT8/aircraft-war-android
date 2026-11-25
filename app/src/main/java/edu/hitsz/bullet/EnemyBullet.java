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
    public void updateOnFreeze() {
        applyFreeze(5000L, 0.0);
    }

    @Override
    public void updateOnBomb(){
        this.vanish();
    }

}
