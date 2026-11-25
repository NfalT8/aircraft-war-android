package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.GameObserver;
import edu.hitsz.strategy.DoubleShootStrategy;

public class ElitePlusEnemy extends AbstractAircraft implements GameObserver {
    public ElitePlusEnemy(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = new DoubleShootStrategy();
    }

    @Override
    public void updateOnFreeze() {
        applyFreeze(3000L, 0.0);
    }

    @Override
    public void updateOnBomb(){
        this.setDestroyedByBomb(true);
        this.decreaseHp(this.hp);
    }

    @Override
    public void forward() {
        super.forward();
        if (locationX <= 0) {
            locationX = 0;
            speedX = -speedX;
        } else if (locationX >= GameConfig.WINDOW_WIDTH) {
            locationX = GameConfig.WINDOW_WIDTH;
            speedX = -speedX;
        }
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }

}
