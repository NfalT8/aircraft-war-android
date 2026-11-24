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
    public void updateOnFreeze(){
        int originalSpeedY = this.speedY;
        int originalSpeedX = this.speedX;
        this.speedY = 0;
        this.speedX = 0;

        // 开启多线程计时恢复
        new Thread(() -> {
            try {
                Thread.sleep(4000);
                this.speedY = originalSpeedY;
                this.speedX = originalSpeedX;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
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
