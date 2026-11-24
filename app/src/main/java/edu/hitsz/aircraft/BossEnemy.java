package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.GameObserver;
import edu.hitsz.strategy.RingShootStrategy;

public class BossEnemy extends AbstractAircraft implements GameObserver {
    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = new RingShootStrategy();
    }

    @Override
    public void updateOnFreeze(){}

    @Override
    public void updateOnBomb(){}

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
