package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.GameObserver;

/**
 * 普通敌机
 * 不可射击、不掉落道具
 * @author hitsz
 */
public class MobEnemy extends AbstractAircraft implements GameObserver {

    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
    }

    @Override
    public void updateOnFreeze() {
        this.speedX = 0;
        this.speedY = 0;
    }

    @Override
    public void updateOnBomb(){
        this.setDestroyedByBomb(true);
        this.decreaseHp(this.hp);
    }

    @Override
    public void forward() {
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= GameConfig.WINDOW_HEIGHT ) {
            vanish();
        }
    }


}
