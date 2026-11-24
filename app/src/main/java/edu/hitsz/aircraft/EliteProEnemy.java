package edu.hitsz.aircraft;

import edu.hitsz.application.GameConfig;
import edu.hitsz.observer.GameObserver;
import edu.hitsz.strategy.ScatterShootStrategy;

public class EliteProEnemy extends AbstractAircraft implements GameObserver {
    public EliteProEnemy(int locationX, int locationY, int speedX, int speedY, int hp, int power) {
        super(locationX, locationY, speedX, speedY, hp, power);
        this.strategy = new ScatterShootStrategy();
    }

    @Override
    public void updateOnFreeze(){
        int originalSpeedY = this.speedY;
        int originalSpeedX = this.speedX;
        this.speedY *= 0.3;
        this.speedX *= 0.3;

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
        this.decreaseHp(60);
        if (this.hp <= 0) {
            this.setDestroyedByBomb(true);
        }
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
