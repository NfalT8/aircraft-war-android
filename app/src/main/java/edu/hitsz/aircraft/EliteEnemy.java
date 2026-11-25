package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.GameObserver;
import edu.hitsz.strategy.DirectShootStrategy;

public class EliteEnemy extends AbstractAircraft implements GameObserver {
    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = new DirectShootStrategy();
    }
    @Override
    public void updateOnFreeze() {
        applyFreeze(4000L, 0.0);
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
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }

}
